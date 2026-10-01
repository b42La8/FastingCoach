import json
import os
import re
from typing import Any

import requests
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from openai import OpenAI
from pydantic import BaseModel, Field

app = FastAPI(title="Fasting Coach API", version="0.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # tighten this for production
    allow_credentials=False,
    allow_methods=["POST", "GET"],
    allow_headers=["*"],
)

client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY")) if os.environ.get("OPENAI_API_KEY") else None
OPENAI_MODEL = os.environ.get("OPENAI_MODEL", "gpt-5.6-luna")
USDA_API_KEY = os.environ.get("USDA_API_KEY", "DEMO_KEY")


class FoodScanRequest(BaseModel):
    image_base64: str = Field(min_length=100)


class FoodResult(BaseModel):
    name: str
    grams: float
    calories: int
    protein_g: float
    carbs_g: float
    fat_g: float
    confidence: float | None = None
    nutrition_source: str = "USDA FoodData Central"


class FoodScanResponse(BaseModel):
    items: list[FoodResult]
    warnings: list[str] = []


def _clean_json(text: str) -> Any:
    text = text.strip()
    text = re.sub(r"^```(?:json)?\s*", "", text)
    text = re.sub(r"\s*```$", "", text)
    return json.loads(text)


def identify_foods(image_base64: str) -> list[dict[str, Any]]:
    if client is None:
        raise HTTPException(500, "OPENAI_API_KEY is not configured on the server")

    prompt = """
You are the visual recognition component of a nutrition logging app.
Identify the distinct foods visible in this meal photo and estimate the edible portion in grams.
Do not invent calories or nutrients. Nutrition is looked up separately.
Return ONLY valid JSON in this exact shape:
{
  "foods": [
    {"name": "plain searchable food name", "estimated_grams": 150, "confidence": 0.85}
  ]
}
Use common USDA-searchable names. If uncertain, lower confidence. Do not claim exact portion accuracy from an image.
""".strip()

    response = client.responses.create(
        model=OPENAI_MODEL,
        input=[
            {
                "role": "user",
                "content": [
                    {"type": "input_text", "text": prompt},
                    {
                        "type": "input_image",
                        "image_url": f"data:image/jpeg;base64,{image_base64}",
                    },
                ],
            }
        ],
    )
    parsed = _clean_json(response.output_text)
    foods = parsed.get("foods", [])
    if not isinstance(foods, list):
        raise ValueError("Invalid food-recognition response")
    return foods[:12]


def _nutrient_value(food: dict[str, Any], names: set[str]) -> float:
    for nutrient in food.get("foodNutrients", []) or []:
        nutrient_name = str(nutrient.get("nutrientName", "")).strip().lower()
        if nutrient_name in names:
            try:
                return float(nutrient.get("value") or 0)
            except (TypeError, ValueError):
                return 0.0
    return 0.0


def lookup_usda(food_name: str, grams: float) -> dict[str, float] | None:
    try:
        r = requests.post(
            "https://api.nal.usda.gov/fdc/v1/foods/search",
            params={"api_key": USDA_API_KEY},
            json={"query": food_name, "pageSize": 5},
            timeout=15,
        )
        r.raise_for_status()
        foods = r.json().get("foods", [])
        if not foods:
            return None

        # Prefer Foundation/SR Legacy/Survey foods over branded matches when possible.
        preferred_types = {"Foundation", "SR Legacy", "Survey (FNDDS)"}
        food = next((f for f in foods if f.get("dataType") in preferred_types), foods[0])
        factor = max(grams, 1.0) / 100.0

        calories = _nutrient_value(food, {"energy"})
        # Some search results can expose multiple Energy rows. Prefer kcal when present.
        for nutrient in food.get("foodNutrients", []) or []:
            if str(nutrient.get("nutrientName", "")).strip().lower() == "energy" and str(nutrient.get("unitName", "")).upper() == "KCAL":
                calories = float(nutrient.get("value") or 0)
                break

        protein = _nutrient_value(food, {"protein"})
        carbs = _nutrient_value(food, {"carbohydrate, by difference", "carbohydrate"})
        fat = _nutrient_value(food, {"total lipid (fat)", "total fat"})

        return {
            "calories": calories * factor,
            "protein_g": protein * factor,
            "carbs_g": carbs * factor,
            "fat_g": fat * factor,
        }
    except requests.RequestException:
        return None


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/analyze-food", response_model=FoodScanResponse)
def analyze_food(request: FoodScanRequest) -> FoodScanResponse:
    try:
        recognized = identify_foods(request.image_base64)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(502, f"Food recognition failed: {exc}") from exc

    items: list[FoodResult] = []
    warnings: list[str] = [
        "Image-based serving sizes are estimates. Confirm portions before saving."
    ]

    for food in recognized:
        name = str(food.get("name") or "Unknown food").strip()
        grams = float(food.get("estimated_grams") or 0)
        confidence = food.get("confidence")
        if grams <= 0:
            grams = 100.0

        nutrition = lookup_usda(name, grams)
        if nutrition is None:
            warnings.append(f"No USDA nutrition match found for {name}.")
            continue

        items.append(
            FoodResult(
                name=name,
                grams=round(grams, 1),
                calories=round(nutrition["calories"]),
                protein_g=round(nutrition["protein_g"], 1),
                carbs_g=round(nutrition["carbs_g"], 1),
                fat_g=round(nutrition["fat_g"], 1),
                confidence=float(confidence) if confidence is not None else None,
            )
        )

    if not items:
        raise HTTPException(422, "Foods were recognized, but no nutrition matches were returned. Try a clearer photo or manual entry.")
    return FoodScanResponse(items=items, warnings=warnings)
