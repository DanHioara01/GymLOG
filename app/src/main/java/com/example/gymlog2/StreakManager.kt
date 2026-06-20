package com.example.gymlog2

import java.util.concurrent.TimeUnit

class StreakManager(private val db: AppDatabase) {
    suspend fun recordWorkout(userId: String) {
        val now = System.currentTimeMillis()
        val existing = db.streakDao().getForUser(userId)
        if (existing == null) {
            db.streakDao().upsert(StreakEntity(userId = userId, currentStreak = 1, bestStreak = 1, lastDate = now))
            return
        }
        val hoursSinceLast = TimeUnit.MILLISECONDS.toHours(now - existing.lastDate)
        val updated = when {
            hoursSinceLast < 24 -> existing
            hoursSinceLast < 48 -> {
                val newCurrent = existing.currentStreak + 1
                existing.copy(
                    currentStreak = newCurrent,
                    bestStreak = maxOf(existing.bestStreak, newCurrent),
                    lastDate = now
                )
            }
            else -> existing.copy(currentStreak = 1, lastDate = now)
        }
        db.streakDao().upsert(updated)
    }
}
