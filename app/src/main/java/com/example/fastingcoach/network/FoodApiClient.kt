package com.example.fastingcoach.network

import android.graphics.Bitmap
import android.util.Base64
import com.example.fastingcoach.data.FoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class FoodApiClient(private val baseUrl: String) {
    suspend fun analyze(bitmap: Bitmap): Result<List<FoodItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, stream)
            val image64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            val payload = JSONObject().put("image_base64", image64).toString()

            val connection = (URL("$baseUrl/analyze-food").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            connection.outputStream.use { it.write(payload.toByteArray()) }
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader().use { it.readText() }
            if (status !in 200..299) error("Food analysis failed ($status): $body")

            val json = JSONObject(body)
            val arr = json.getJSONArray("items")
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        FoodItem(
                            name = o.getString("name"),
                            grams = o.optDouble("grams", 0.0),
                            calories = o.optInt("calories", 0),
                            proteinG = o.optDouble("protein_g", 0.0),
                            carbsG = o.optDouble("carbs_g", 0.0),
                            fatG = o.optDouble("fat_g", 0.0),
                            confidence = if (o.has("confidence")) o.optDouble("confidence") else null
                        )
                    )
                }
            }
        }
    }
}
