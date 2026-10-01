# Fasting Coach MVP

Android-first MVP for fasting, AI-assisted food logging, calorie/macronutrient tracking, Health Connect steps, estimated walking equivalents for calorie overages, and weekly eating/activity insights.

## What is implemented

- Fasting timer that survives app restarts, configurable fasting target, completed-fast history, and cautious educational fasting-stage cards.
- Daily calorie/macronutrient targets using a standard BMR/TDEE estimate, plus an optional user-defined calorie target.
- Camera meal capture with an editable serving-size review before a meal is saved.
- Server-side food recognition using OpenAI image input; OpenAI identifies foods/estimated grams while USDA FoodData Central supplies nutrition values.
- Automatic calorie/macro addition after a scanned meal is confirmed.
- Health Connect step reading for today and the previous 7 days. This is the intended bridge for Samsung Health/Galaxy Watch data when the user has enabled sharing into Health Connect.
- Estimated extra walking steps when daily food calories exceed the calorie target. The UI deliberately labels this as an estimate rather than an exact calorie offset.
- Weekly report showing calories, protein, steps, fasting consistency, over-target food contributors, evening-eating patterns, weekend patterns, repeated foods, and concrete improvement areas.
- Local persistence using SharedPreferences/JSON for an MVP. A production release should migrate to Room or a secured backend/sync layer.

## Project structure

- `app/` — native Android/Kotlin/Jetpack Compose app.
- `backend/` — FastAPI service that holds the OpenAI key and queries USDA FoodData Central.

## Android setup

This project is configured for Android API 37, AGP 9.3.0, Kotlin 2.4.10 and the September 2026 Compose BOM. Open the project folder in a current Android Studio and let it sync dependencies. The Gradle properties temporarily opt out of AGP 9 built-in Kotlin so the Kotlin/Compose plugin setup remains conventional for this MVP; migrate to built-in Kotlin before AGP 10.

The emulator points to the local API at `http://10.0.2.2:8000`. For a physical phone, change `API_BASE_URL` in `app/build.gradle.kts` to an HTTPS backend URL before building.

The manifest enables cleartext HTTP only for local MVP development. Remove `android:usesCleartextTraffic="true"` in production and use HTTPS.

## Build an APK without Android Studio

A GitHub Actions workflow is included at `.github/workflows/build-apk.yml`. It builds a debug-signed `FastingCoach-debug.apk` using JDK 17, Android API 37, Build Tools 36.0.0 and Gradle 9.5.0. See `BUILD_APK_NO_ANDROID_STUDIO.md` for click-by-click instructions.

## Backend setup

From `backend/`:

```bash
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\\Scripts\\activate
pip install -r requirements.txt
export OPENAI_API_KEY="..."
export USDA_API_KEY="DEMO_KEY"  # replace with your FoodData Central key for production
./run.sh
```

Then check `http://localhost:8000/health`.

### Security

Do **not** put an OpenAI API key in the Android app. The included design sends the image to your backend, and only the backend calls OpenAI/USDA.

Before production, add authentication, request-size limits, rate limiting, image-retention/deletion rules, a privacy policy, abuse monitoring, and HTTPS. Lock CORS down to your production needs.

## Health Connect / Samsung

The app requests read access only to steps and uses aggregated step totals to reduce double-counting. Samsung Health users must allow Samsung Health to write/share the relevant data into Health Connect and grant this app permission to read steps.

For a Play Store release, complete the Health Connect data-access declarations and privacy requirements in Play Console. Health data should be minimized to what the feature needs.

## Food scanning behavior

The backend deliberately separates responsibilities:

1. OpenAI vision identifies visible foods and estimates edible grams.
2. USDA FoodData Central is searched for nutrition values.
3. The user reviews and can edit grams before adding the meal.

Photo portion estimation can never be exact. A future version should add barcode scanning, manual search, saved foods/recipes, and optional scale/serving-unit entry.

## Weekly report logic

The report is deterministic rather than asking AI to invent metrics. It computes:

- 7-day calorie target vs logged calories.
- Daily calorie and protein averages.
- Step average and number of step-goal days.
- Completed fasting goals and average fasting duration.
- Which *latest logged foods* account for calories beyond the daily target on over-target days.
- Share of calories logged after 8 PM.
- Weekend vs weekday calorie pattern.
- Repeated logged foods.
- Improvement suggestions triggered by actual gaps in the data.

A later AI “coach” layer can rewrite these computed facts conversationally, but should receive the already-calculated summary so it cannot silently change the numbers.

## Important MVP limitations

- No cloud account/sync yet.
- No barcode scanner yet.
- No push reminders/weekly-report notifications yet.
- Food matches use the first suitable USDA search result; production needs better matching and serving-unit logic.
- Calorie targets and walking equivalents are general wellness estimates and should not be presented as medical advice or exact energy compensation.
- Special populations (for example pregnancy, minors, eating-disorder risk, or clinician-directed diets) need additional product/safety design before release.

## Suggested next production milestones

1. Add Supabase authentication and encrypted cloud sync.
2. Replace SharedPreferences with Room + repository sync.
3. Add barcode scanning and manual food search.
4. Add scheduled fasting and weekly-report notifications.
5. Add automated tests for ViewModel/report edge cases.
6. Add onboarding/consent/privacy screens and Play Store Health Connect declarations.
7. Add a production API gateway with auth, rate limiting and observability.
