package com.example.gymlog2

import android.content.Context
import org.json.JSONObject
import java.util.UUID

class UserProfileManager(context: Context) {
    private val prefs = context.getSharedPreferences("user_profiles", Context.MODE_PRIVATE)

    data class UserProfile(
        val userId: String,
        val name: String,
        val photoUri: String = ""
    )

    fun getOwnProfile(): UserProfile? {
        val id = prefs.getString("own_user_id", null) ?: return null
        val name = prefs.getString("own_name", "") ?: ""
        val photo = prefs.getString("own_photo", "") ?: ""
        return UserProfile(id, name, photo)
    }

    fun isProfileComplete(): Boolean {
        val name = prefs.getString("own_name", "") ?: ""
        return name.isNotBlank()
    }

    fun saveOwnProfile(name: String, photoUri: String = "") {
        var userId = prefs.getString("own_user_id", null)
        if (userId == null) {
            userId = "user_${UUID.randomUUID().toString().take(8)}"
            prefs.edit().putString("own_user_id", userId).apply()
        }
        prefs.edit()
            .putString("own_name", name)
            .putString("own_photo", photoUri)
            .apply()
    }

    fun getOwnUserId(): String {
        return prefs.getString("own_user_id", null) ?: "local_user"
    }

    fun saveProfile(profile: UserProfile) {
        val json = prefs.getString("known_profiles", "{}") ?: "{}"
        val obj = JSONObject(json)
        val entry = JSONObject().put("name", profile.name).put("photo", profile.photoUri)
        obj.put(profile.userId, entry)
        prefs.edit().putString("known_profiles", obj.toString()).apply()
    }

    fun getProfile(userId: String): UserProfile? {
        val json = prefs.getString("known_profiles", "{}") ?: "{}"
        val obj = JSONObject(json)
        if (!obj.has(userId)) return null
        val entry = obj.getJSONObject(userId)
        return UserProfile(userId, entry.optString("name", ""), entry.optString("photo", ""))
    }

    fun getAllKnownProfiles(): List<UserProfile> {
        val json = prefs.getString("known_profiles", "{}") ?: "{}"
        val obj = JSONObject(json)
        val result = mutableListOf<UserProfile>()
        for (key in obj.keys()) {
            val entry = obj.getJSONObject(key)
            result.add(UserProfile(key, entry.optString("name", ""), entry.optString("photo", "")))
        }
        return result
    }

    fun createOrUpdateProfile(name: String, photoUri: String = "", userId: String = getOwnUserId()) {
        val json = prefs.getString("known_profiles", "{}") ?: "{}"
        val obj = JSONObject(json)
        val entry = JSONObject().put("name", name).put("photo", photoUri)
        obj.put(userId, entry)
        prefs.edit()
            .putString("known_profiles", obj.toString())
            .putString("own_user_id", userId)
            .putString("own_name", name)
            .putString("own_photo", photoUri)
            .apply()
    }

    fun searchProfiles(query: String): List<UserProfile> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return getAllKnownProfiles().filter {
            it.name.lowercase().contains(q) || it.userId.lowercase().contains(q)
        }
    }
}
