package com.example.gymlog2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FoodProduct(
    val barcode: String = "",
    val name: String = "",
    val brand: String = "",
    val servingSize: Double = 100.0,
    val servingUnit: String = "g",
    val calories: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val found: Boolean = false
)

object OpenFoodFactsApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getProduct(barcode: String): FoodProduct = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://world.openfoodfacts.org/api/v2/product/$barcode.json")
                .addHeader("User-Agent", "Kinetic/1.0 (gym app)")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext FoodProduct(barcode = barcode)
            val json = JSONObject(body)

            if (json.optInt("status", 0) != 1) {
                return@withContext FoodProduct(barcode = barcode)
            }

            val product = json.getJSONObject("product")

            val name = product.optString("product_name", "")
            val brand = product.optString("brands", "")

            val nutriments = product.optJSONObject("nutriments") ?: JSONObject()

            val calories = nutriments.optDouble("energy-kcal_100g", 0.0)
            val protein = nutriments.optDouble("proteins_100g", 0.0)
            val carbs = nutriments.optDouble("carbohydrates_100g", 0.0)
            val fat = nutriments.optDouble("fat_100g", 0.0)
            val fiber = nutriments.optDouble("fiber_100g", 0.0)

            val servingSizeStr = product.optString("serving_size", "100g")
            val servingParsed = parseServingSize(servingSizeStr)

            FoodProduct(
                barcode = barcode,
                name = name,
                brand = brand,
                servingSize = servingParsed.first,
                servingUnit = servingParsed.second,
                calories = calories,
                proteinG = protein,
                carbsG = carbs,
                fatG = fat,
                fiberG = fiber,
                found = name.isNotBlank()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            FoodProduct(barcode = barcode)
        }
    }

    private fun parseServingSize(servingSize: String): Pair<Double, String> {
        val regex = Regex("""([\d.,]+)\s*(g|ml|kg|l|oz|cup|piece|serving)s?""", RegexOption.IGNORE_CASE)
        val match = regex.find(servingSize)
        return if (match != null) {
            val amount = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 100.0
            val unit = match.groupValues[2].lowercase()
            Pair(amount, if (unit == "ml" || unit == "l") "ml" else "g")
        } else {
            Pair(100.0, "g")
        }
    }
}
