package com.example.gymlog2

import androidx.room.*

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val provider: String = "stripe",
    val subscriptionId: String = "",
    val planId: String = "",
    val status: String = "inactive",
    val currentPeriodEnd: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "feature_flags")
data class FeatureFlagEntity(
    @PrimaryKey val key: String,
    val enabled: Boolean = false
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val loginKey: String,
    val name: String,
    val photoUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles WHERE loginKey = :loginKey LIMIT 1")
    suspend fun getByLoginKey(loginKey: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE name LIKE '%' || :query || '%' OR userId LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<UserProfileEntity>

    @Query("SELECT * FROM user_profiles")
    suspend fun getAll(): List<UserProfileEntity>
}

@Dao
interface SubscriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subscription: SubscriptionEntity)

    @Query("SELECT * FROM subscriptions WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getForUser(userId: String): SubscriptionEntity?

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface FeatureFlagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(flag: FeatureFlagEntity)

    @Query("SELECT * FROM feature_flags WHERE key = :key LIMIT 1")
    suspend fun getFlag(key: String): FeatureFlagEntity?

    @Query("SELECT * FROM feature_flags")
    suspend fun getAll(): List<FeatureFlagEntity>
}
