package com.example.gymlog2

import androidx.room.*

@Entity(tableName = "cardio_sessions")
data class CardioSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val activeTimeMinutes: Int = 0,
    val caloriesBurned: Double = 0.0,
    val avgHeartRate: Int = 0,
    val routeJson: String = "",
    val sessionType: String = "walk"
)

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: Long = 0,
    val steps: Int = 0,
    val stepsGoal: Int = 7000,
    val activeTimeMinutes: Int = 0,
    val activeTimeGoal: Int = 90,
    val activityCalories: Double = 0.0,
    val activityCaloriesGoal: Double = 500.0,
    val totalCaloriesBurned: Double = 0.0,
    val distanceMeters: Double = 0.0
)

@Dao
interface CardioSessionDao {
    @Insert
    suspend fun insert(session: CardioSessionEntity): Long

    @Update
    suspend fun update(session: CardioSessionEntity)

    @Delete
    suspend fun delete(session: CardioSessionEntity)

    @Query("SELECT * FROM cardio_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllForUser(userId: String): List<CardioSessionEntity>

    @Query("SELECT * FROM cardio_sessions WHERE userId = :userId AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getForPeriod(userId: String, start: Long, end: Long): List<CardioSessionEntity>

    @Query("SELECT * FROM cardio_sessions WHERE id = :id")
    suspend fun getById(id: Long): CardioSessionEntity?

    @Query("SELECT SUM(distanceMeters) FROM cardio_sessions WHERE userId = :userId AND timestamp BETWEEN :start AND :end")
    suspend fun getTotalDistance(userId: String, start: Long, end: Long): Double?

    @Query("SELECT SUM(caloriesBurned) FROM cardio_sessions WHERE userId = :userId AND timestamp BETWEEN :start AND :end")
    suspend fun getTotalCalories(userId: String, start: Long, end: Long): Double?
}

@Dao
interface DailyActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(activity: DailyActivityEntity)

    @Query("SELECT * FROM daily_activity WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getForDate(userId: String, date: Long): DailyActivityEntity?

    @Query("SELECT * FROM daily_activity WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(userId: String, limit: Int): List<DailyActivityEntity>

    @Query("SELECT * FROM daily_activity WHERE userId = :userId AND date BETWEEN :start AND :end ORDER BY date DESC")
    suspend fun getForPeriod(userId: String, start: Long, end: Long): List<DailyActivityEntity>
}
