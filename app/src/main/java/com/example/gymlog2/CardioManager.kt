package com.example.gymlog2

import java.util.Calendar

class CardioManager(private val db: AppDatabase) {

    private fun todayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    suspend fun getTodayActivity(userId: String): DailyActivityEntity {
        val dayStart = todayStartMillis()
        return db.dailyActivityDao().getForDate(userId, dayStart)
            ?: DailyActivityEntity(userId = userId, date = dayStart)
    }

    suspend fun updateTodayActivity(
        userId: String,
        steps: Int? = null,
        activeTimeMinutes: Int? = null,
        activityCalories: Double? = null,
        totalCaloriesBurned: Double? = null,
        distanceMeters: Double? = null
    ) {
        val dayStart = todayStartMillis()
        val existing = db.dailyActivityDao().getForDate(userId, dayStart)
            ?: DailyActivityEntity(userId = userId, date = dayStart)

        val updated = existing.copy(
            steps = steps ?: existing.steps,
            activeTimeMinutes = activeTimeMinutes ?: existing.activeTimeMinutes,
            activityCalories = activityCalories ?: existing.activityCalories,
            totalCaloriesBurned = totalCaloriesBurned ?: existing.totalCaloriesBurned,
            distanceMeters = distanceMeters ?: existing.distanceMeters
        )
        db.dailyActivityDao().upsert(updated)
    }

    suspend fun setGoals(
        userId: String,
        stepsGoal: Int? = null,
        activeTimeGoal: Int? = null,
        caloriesGoal: Double? = null
    ) {
        val dayStart = todayStartMillis()
        val existing = db.dailyActivityDao().getForDate(userId, dayStart)
            ?: DailyActivityEntity(userId = userId, date = dayStart)

        val updated = existing.copy(
            stepsGoal = stepsGoal ?: existing.stepsGoal,
            activeTimeGoal = activeTimeGoal ?: existing.activeTimeGoal,
            activityCaloriesGoal = caloriesGoal ?: existing.activityCaloriesGoal
        )
        db.dailyActivityDao().upsert(updated)
    }

    suspend fun saveCardioSession(
        userId: String,
        durationMs: Long,
        steps: Int,
        distanceMeters: Double,
        caloriesBurned: Double,
        avgHeartRate: Int = 0,
        routeJson: String = "",
        sessionType: String = "walk"
    ): Long {
        val session = CardioSessionEntity(
            userId = userId,
            durationMs = durationMs,
            steps = steps,
            distanceMeters = distanceMeters,
            activeTimeMinutes = (durationMs / 60000).toInt(),
            caloriesBurned = caloriesBurned,
            avgHeartRate = avgHeartRate,
            routeJson = routeJson,
            sessionType = sessionType
        )
        val sessionId = db.cardioSessionDao().insert(session)

        val today = getTodayActivity(userId)
        updateTodayActivity(
            userId = userId,
            steps = today.steps + steps,
            activeTimeMinutes = today.activeTimeMinutes + (durationMs / 60000).toInt(),
            activityCalories = today.activityCalories + caloriesBurned,
            totalCaloriesBurned = today.totalCaloriesBurned + caloriesBurned,
            distanceMeters = today.distanceMeters + distanceMeters
        )

        return sessionId
    }

    suspend fun getRecentSessions(userId: String, limit: Int = 10): List<CardioSessionEntity> {
        return db.cardioSessionDao().getAllForUser(userId).take(limit)
    }

    suspend fun getWeeklyActivity(userId: String): List<DailyActivityEntity> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val end = cal.timeInMillis + 86400000
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val start = cal.timeInMillis
        return db.dailyActivityDao().getForPeriod(userId, start, end)
    }
}
