package com.example.gymlog2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BiometricManager(private val db: AppDatabase) {
    private val dao = db.biometricDao()

    suspend fun saveEntry(
        userId: String,
        weightKg: Double,
        bodyFatPercent: Double,
        waistCm: Double,
        hipsCm: Double,
        thighsCm: Double,
        chestCm: Double,
        armsCm: Double,
        notes: String = ""
    ): Long = withContext(Dispatchers.IO) {
        dao.insert(
            BiometricEntity(
                userId = userId,
                weightKg = weightKg,
                bodyFatPercent = bodyFatPercent,
                waistCm = waistCm,
                hipsCm = hipsCm,
                thighsCm = thighsCm,
                chestCm = chestCm,
                armsCm = armsCm,
                notes = notes
            )
        )
    }

    suspend fun getLatest(userId: String): BiometricEntity? = withContext(Dispatchers.IO) {
        dao.getLatest(userId)
    }

    suspend fun getAll(userId: String): List<BiometricEntity> = withContext(Dispatchers.IO) {
        dao.getAllForUser(userId)
    }

    suspend fun getRecent(userId: String, limit: Int): List<BiometricEntity> = withContext(Dispatchers.IO) {
        dao.getRecent(userId, limit)
    }

    suspend fun delete(entry: BiometricEntity) = withContext(Dispatchers.IO) {
        dao.delete(entry)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun getWeeksSinceLastMeasurement(userId: String): Int = withContext(Dispatchers.IO) {
        val latest = dao.getLatest(userId) ?: return@withContext -1
        val diffMs = System.currentTimeMillis() - latest.timestamp
        (diffMs / (7L * 24 * 60 * 60 * 1000)).toInt()
    }
}
