package com.example.gymlog2

class FeatureFlagRepository(private val db: AppDatabase) {
    suspend fun isEnabled(key: String): Boolean = db.featureFlagDao().getFlag(key)?.enabled ?: false
    suspend fun setFlag(key: String, enabled: Boolean) {
        db.featureFlagDao().upsert(FeatureFlagEntity(key = key, enabled = enabled))
    }
    suspend fun getAll(): List<FeatureFlagEntity> = db.featureFlagDao().getAll()
}
