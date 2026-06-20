package com.example.gymlog2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class FoodManager(private val db: AppDatabase) {
    private val dao = db.foodDao()

    suspend fun addFood(
        userId: String,
        barcode: String,
        name: String,
        brand: String,
        mealType: String,
        servingSize: Double,
        servingUnit: String,
        calories: Double,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        fiberG: Double
    ): Long = withContext(Dispatchers.IO) {
        dao.insert(
            FoodEntity(
                userId = userId,
                barcode = barcode,
                name = name,
                brand = brand,
                mealType = mealType,
                servingSize = servingSize,
                servingUnit = servingUnit,
                calories = calories,
                proteinG = proteinG,
                carbsG = carbsG,
                fatG = fatG,
                fiberG = fiberG
            )
        )
    }

    suspend fun getTodayEntries(userId: String): List<FoodEntity> = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = start + 86400000L
        dao.getForDay(userId, start, end)
    }

    suspend fun getAll(userId: String): List<FoodEntity> = withContext(Dispatchers.IO) {
        dao.getRecent(userId, 500)
    }

    suspend fun getDailyMacros(userId: String): DailyMacros = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = start + 86400000L
        DailyMacros(
            calories = dao.getTotalCalories(userId, start, end) ?: 0.0,
            protein = dao.getTotalProtein(userId, start, end) ?: 0.0,
            carbs = dao.getTotalCarbs(userId, start, end) ?: 0.0,
            fat = dao.getTotalFat(userId, start, end) ?: 0.0,
            fiber = 0.0
        )
    }

    suspend fun delete(entry: FoodEntity) = withContext(Dispatchers.IO) {
        dao.delete(entry)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }
}
