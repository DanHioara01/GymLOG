package com.example.gymlog2

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class FirestoreHelper {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveFcmToken(userId: String) {
        val token = FirebaseMessaging.getInstance().token.await()
        if (token.isNotBlank()) {
            db.collection("users").document(userId)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .await()
        }
    }

    suspend fun saveUserProfile(userId: String, name: String, photoUri: String) {
        val data = mutableMapOf<String, Any>(
            "name" to name,
            "userId" to userId
        )
        if (photoUri.isNotBlank()) data["photoUri"] = photoUri
        db.collection("users").document(userId).set(data, SetOptions.merge()).await()
    }

    suspend fun searchUsers(query: String): List<Pair<String, String>> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        val results = mutableListOf<Pair<String, String>>()
        try {
            val snapshot = db.collection("users").get().await()
            for (doc in snapshot.documents) {
                val userId = doc.id
                val name = (doc.getString("name") ?: "").lowercase()
                if (name.contains(q) || userId.lowercase().contains(q)) {
                    results.add(userId to (doc.getString("name") ?: userId))
                }
            }
        } catch (_: Exception) { }
        return results
    }

    suspend fun getUserProfile(userId: String): Map<String, Any>? {
        return try {
            db.collection("users").document(userId).get().await().data
        } catch (_: Exception) { null }
    }

    suspend fun syncUserStats(userId: String, totalVolume: Double, workoutCount: Int) {
        try {
            db.collection("users").document(userId)
                .set(
                    mapOf(
                        "totalVolume" to totalVolume,
                        "workoutCount" to workoutCount,
                        "lastSeen" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
        } catch (_: Exception) { }
    }
}
