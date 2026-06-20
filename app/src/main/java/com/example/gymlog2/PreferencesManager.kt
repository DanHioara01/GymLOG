package com.example.gymlog2

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class WaterAlarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        return try {
            ThemeMode.valueOf(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun getLanguage(): String {
        return prefs.getString("language", "ro") ?: "ro"
    }

    fun setLanguage(code: String) {
        prefs.edit().putString("language", code).apply()
    }

    fun isLbs(): Boolean = prefs.getBoolean("use_lbs", false)
    fun setLbs(value: Boolean) { prefs.edit().putBoolean("use_lbs", value).apply() }

    fun isLoggedIn(): Boolean = prefs.getBoolean("logged_in", false)
    fun setLoggedIn(value: Boolean) { prefs.edit().putBoolean("logged_in", value).apply() }
    fun getLoginMethod(): String = prefs.getString("login_method", "") ?: ""
    fun setLoginMethod(method: String) { prefs.edit().putString("login_method", method).apply() }
    fun getGuestKey(): String = prefs.getString("guest_key", "") ?: ""
    fun setGuestKey(key: String) { prefs.edit().putString("guest_key", key).apply() }

    fun isOnboardingComplete(): Boolean = prefs.getBoolean("onboarding_complete", false)
    fun setOnboardingComplete(value: Boolean) { prefs.edit().putBoolean("onboarding_complete", value).apply() }
    fun getFitnessGoal(): String = prefs.getString("fitness_goal", "") ?: ""
    fun setFitnessGoal(goal: String) { prefs.edit().putString("fitness_goal", goal).apply() }

    fun getExperienceLevel(): String = prefs.getString("experience_level", "") ?: ""
    fun setExperienceLevel(level: String) { prefs.edit().putString("experience_level", level).apply() }

    fun getEquipmentAvailable(): String = prefs.getString("equipment_available", "") ?: ""
    fun setEquipmentAvailable(equipment: String) { prefs.edit().putString("equipment_available", equipment).apply() }

    fun getSessionsPerWeek(): Int = prefs.getInt("sessions_per_week", 3)
    fun setSessionsPerWeek(count: Int) { prefs.edit().putInt("sessions_per_week", count).apply() }

    fun getPhysicalLimitations(): String = prefs.getString("physical_limitations", "") ?: ""
    fun setPhysicalLimitations(limitations: String) { prefs.edit().putString("physical_limitations", limitations).apply() }

    fun getSelectedMuscleGroups(): String = prefs.getString("selected_muscle_groups", "") ?: ""
    fun setSelectedMuscleGroups(groups: List<String>) {
        prefs.edit().putString("selected_muscle_groups", groups.joinToString(",")).apply()
    }

    fun getOnboardingProfile(): UserOnboardingProfile {
        val groupsStr = getSelectedMuscleGroups()
        val groups = if (groupsStr.isBlank()) emptyList() else groupsStr.split(",")
        return UserOnboardingProfile(
            goal = getFitnessGoal(),
            experience = getExperienceLevel(),
            equipment = getEquipmentAvailable(),
            sessionsPerWeek = getSessionsPerWeek(),
            limitations = getPhysicalLimitations(),
            selectedGroups = groups
        )
    }

    fun getUserWeight(): Float = prefs.getFloat("user_weight", 70f)
    fun setUserWeight(kg: Float) { prefs.edit().putFloat("user_weight", kg).apply() }

    fun getUserHeight(): Float = prefs.getFloat("user_height", 170f)
    fun setUserHeight(cm: Float) { prefs.edit().putFloat("user_height", cm).apply() }

    fun getWaterGoalMl(): Int {
        val weight = getUserWeight()
        val raw = (weight * 33).toInt()
        return (raw / 50) * 50
    }

    fun getTodayWaterMl(): Int {
        val dayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        return prefs.getInt("water_$dayKey", 0)
    }

    fun addWaterMl(ml: Int) {
        val dayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val current = getTodayWaterMl()
        prefs.edit().putInt("water_$dayKey", current + ml).apply()
    }

    fun getWaterHistory7Days(): List<Pair<String, Int>> {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val dayFmt = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        val result = mutableListOf<Pair<String, Int>>()
        val cal = java.util.Calendar.getInstance()
        for (i in 6 downTo 0) {
            val c = java.util.Calendar.getInstance()
            c.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val key = fmt.format(c.time)
            val ml = prefs.getInt("water_$key", 0)
            val dayName = dayFmt.format(c.time).take(3)
            result.add(dayName to ml)
        }
        return result
    }

    fun isWaterReminderEnabled(): Boolean = prefs.getBoolean("water_reminder_enabled", false)
    fun setWaterReminderEnabled(enabled: Boolean) { prefs.edit().putBoolean("water_reminder_enabled", enabled).apply() }

    fun getWaterReminders(): List<WaterAlarm> {
        val json = prefs.getString("water_reminders_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                WaterAlarm(
                    id = obj.getInt("id"),
                    hour = obj.getInt("hour"),
                    minute = obj.getInt("minute"),
                    enabled = obj.getBoolean("enabled")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveWaterReminders(alarms: List<WaterAlarm>) {
        val arr = JSONArray()
        alarms.forEach { alarm ->
            arr.put(JSONObject().apply {
                put("id", alarm.id)
                put("hour", alarm.hour)
                put("minute", alarm.minute)
                put("enabled", alarm.enabled)
            })
        }
        prefs.edit().putString("water_reminders_json", arr.toString()).apply()
    }

    fun addWaterReminder(hour: Int, minute: Int): WaterAlarm {
        val alarms = getWaterReminders().toMutableList()
        val newId = (alarms.maxOfOrNull { it.id } ?: 0) + 1
        val alarm = WaterAlarm(id = newId, hour = hour, minute = minute, enabled = true)
        alarms.add(alarm)
        saveWaterReminders(alarms)
        return alarm
    }

    fun updateWaterReminder(id: Int, hour: Int, minute: Int) {
        val alarms = getWaterReminders().map {
            if (it.id == id) it.copy(hour = hour, minute = minute) else it
        }
        saveWaterReminders(alarms)
    }

    fun toggleWaterReminder(id: Int, enabled: Boolean) {
        val alarms = getWaterReminders().map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
        saveWaterReminders(alarms)
    }

    fun deleteWaterReminder(id: Int) {
        val alarms = getWaterReminders().filter { it.id != id }
        saveWaterReminders(alarms)
    }

    fun isBiometricReminderEnabled(): Boolean = prefs.getBoolean("biometric_reminder_enabled", false)
    fun setBiometricReminderEnabled(enabled: Boolean) { prefs.edit().putBoolean("biometric_reminder_enabled", enabled).apply() }

    fun getServerUrl(): String = prefs.getString("server_url", "") ?: ""
    fun setServerUrl(url: String) { prefs.edit().putString("server_url", url).apply() }
}
