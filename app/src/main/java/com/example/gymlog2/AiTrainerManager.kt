package com.example.gymlog2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AiTrainerManager(private val db: AppDatabase) {

    companion object {
        var serverUrl: String = "https://ai-server-7tqx.onrender.com"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getWorkoutContext(userId: String, preferencesManager: PreferencesManager): String = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val cal = Calendar.getInstance()

        val profile = preferencesManager.getOnboardingProfile()
        val weight = preferencesManager.getUserWeight()
        val height = preferencesManager.getUserHeight()

        cal.add(Calendar.DAY_OF_YEAR, -30)
        val monthAgo = cal.timeInMillis
        val recentWorkouts = db.antrenamentDao().getWorkoutsInPeriod(userId, monthAgo, System.currentTimeMillis())

        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        cal.timeInMillis = todayStart
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val weekStart = cal.timeInMillis
        val weekWorkouts = db.antrenamentDao().getWorkoutsInPeriod(userId, weekStart, System.currentTimeMillis())

        val weekVolume = db.antrenamentDao().getTotalVolume(userId, weekStart, System.currentTimeMillis()) ?: 0.0
        cal.timeInMillis = todayStart
        cal.add(Calendar.MONTH, -1)
        val monthVolume = db.antrenamentDao().getTotalVolume(userId, cal.timeInMillis, System.currentTimeMillis()) ?: 0.0

        val recovery = AntrenamentRepository(db).getToateRecuperarile()
        val prs = db.personalRecordDao().getAllForUser(userId).take(10)
        val topExercise = db.exercitiuDao().getMostFrequentExercise(userId, weekStart, System.currentTimeMillis())

        StringBuilder().apply {
            appendLine("=== USER PROFILE ===")
            appendLine("Goal: ${profile.goal.ifBlank { "not set" }}")
            appendLine("Experience: ${profile.experience.ifBlank { "not set" }}")
            appendLine("Equipment: ${profile.equipment.ifBlank { "not set" }}")
            appendLine("Sessions/week target: ${profile.sessionsPerWeek}")
            appendLine("Limitations: ${profile.limitations.ifBlank { "none" }}")
            appendLine("Weight: ${weight}kg, Height: ${height}cm")
            appendLine()
            appendLine("=== THIS WEEK ===")
            appendLine("Workouts this week: ${weekWorkouts.size}/${profile.sessionsPerWeek}")
            appendLine("Weekly volume: ${String.format("%.0f", weekVolume)} kg")
            if (topExercise != null) {
                appendLine("Most trained exercise: ${topExercise.numeExercitiu} (${topExercise.cnt}x)")
            }
            appendLine()
            appendLine("=== LAST 30 DAYS ===")
            appendLine("Total workouts: ${recentWorkouts.size}")
            appendLine("Monthly volume: ${String.format("%.0f", monthVolume)} kg")
            val groups = recentWorkouts.groupBy { it.grupaMusculara }
            appendLine("Muscle groups trained: ${groups.keys.joinToString(", ")}")
            appendLine()
            appendLine("=== MUSCLE RECOVERY (0=fresh, 1=fatigued) ===")
            recovery.forEach { (group, level) ->
                val status = when {
                    level < 0.2 -> "Ready"
                    level < 0.5 -> "Moderate"
                    level < 0.8 -> "Tired"
                    else -> "Needs rest"
                }
                appendLine("$group: ${String.format("%.0f", level * 100)}% - $status")
            }
            appendLine()
            if (prs.isNotEmpty()) {
                appendLine("=== PERSONAL RECORDS ===")
                prs.forEach { pr ->
                    appendLine("${pr.exerciseName}: ${pr.weight}kg x ${pr.reps} reps (${dateFormat.format(Date(pr.date))})")
                }
                appendLine()
            }
            if (recentWorkouts.isNotEmpty()) {
                appendLine("=== RECENT WORKOUTS ===")
                recentWorkouts.take(5).forEach { workout ->
                    val exercises = db.exercitiuDao().getForAntrenament(workout.id)
                    appendLine("${dateFormat.format(Date(workout.data))} - ${workout.grupaMusculara}: ${exercises.size} exercises, ${String.format("%.0f", workout.totalWeight)} kg total")
                }
            }
        }.toString()
    }

    suspend fun chat(
        userId: String,
        message: String,
        conversationHistory: List<Pair<String, String>>,
        preferencesManager: PreferencesManager
    ): String = withContext(Dispatchers.IO) {
        try {
            val context = getWorkoutContext(userId, preferencesManager)

            val systemPrompt = """You are an expert AI personal trainer called "Kinetic Trainer". You help users with their fitness journey.

You have access to the user's workout data, recovery status, personal records, and profile. Use this data to give personalized, actionable advice.

Rules:
- Be concise and motivational
- Give specific exercise suggestions when appropriate
- Consider muscle recovery status before suggesting workouts
- If a muscle group is fatigued (>70%), suggest alternatives or rest
- Adjust advice based on the user's experience level and goals
- Use the metric system (kg, cm)
- If the user hasn't trained enough this week, encourage them
- Suggest deload when appropriate (volume too high, recovery too low)
- Respond in the same language the user writes in
- Keep responses under 200 words

Workout Data:
$context"""

            val historyArray = JSONArray()
            conversationHistory.forEach { (role, content) ->
                val msg = JSONObject().apply {
                    put("role", role)
                    put("content", content)
                }
                historyArray.put(msg)
            }

            val body = JSONObject().apply {
                put("message", message)
                put("system_prompt", systemPrompt)
                put("history", historyArray)
            }

            val request = Request.Builder()
                .url("$serverUrl/chat")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                json.getString("reply")
            } else {
                "Error: Server returned ${response.code}. Check your server URL and API key."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message ?: "Cannot connect to server"}. Make sure the AI server is running at $serverUrl"
        }
    }
}
