package com.example.gymlog2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val antrenamentRepo = AntrenamentRepository(db)
    private val socialRepo = SocialRepository(db)
    private val streakManager = StreakManager(db)
    private val badgeEngine = BadgeEngine(db)
    private val featureFlagRepo = FeatureFlagRepository(db)
    private val exerciseSeeder = ExerciseSeeder(db)

    private val _workouts = MutableStateFlow<List<AntrenamentEntity>>(emptyList())
    val workouts: StateFlow<List<AntrenamentEntity>> = _workouts

    private val _exercises = MutableStateFlow<List<ExerciseDefinitionEntity>>(emptyList())
    val exercises: StateFlow<List<ExerciseDefinitionEntity>> = _exercises

    private val _exerciseHistory = MutableStateFlow<List<ExercitiuEntity>>(emptyList())
    val exerciseHistory: StateFlow<List<ExercitiuEntity>> = _exerciseHistory

    private val _bestSet = MutableStateFlow<ExercitiuEntity?>(null)
    val bestSet: StateFlow<ExercitiuEntity?> = _bestSet

    private val _personalRecords = MutableStateFlow<List<PersonalRecordEntity>>(emptyList())
    val personalRecords: StateFlow<List<PersonalRecordEntity>> = _personalRecords

    private val _dailyVolume = MutableStateFlow(0.0)
    val dailyVolume: StateFlow<Double> = _dailyVolume

    private val _weeklyVolume = MutableStateFlow(0.0)
    val weeklyVolume: StateFlow<Double> = _weeklyVolume

    private val _mostFrequentExercise = MutableStateFlow<String?>(null)
    val mostFrequentExercise: StateFlow<String?> = _mostFrequentExercise

    private val _weeklyTotalWeight = MutableStateFlow(0.0)
    val weeklyTotalWeight: StateFlow<Double> = _weeklyTotalWeight

    private val _monthlyVolume = MutableStateFlow(0.0)
    val monthlyVolume: StateFlow<Double> = _monthlyVolume

    private val _workoutExercises = MutableStateFlow<List<ExercitiuEntity>>(emptyList())
    val workoutExercises: StateFlow<List<ExercitiuEntity>> = _workoutExercises

    init {
        viewModelScope.launch {
            exerciseSeeder.seedIfEmpty()
            badgeEngine.seedBadges()
            seedFeatureFlags()
        }
    }

    private suspend fun seedFeatureFlags() {
        val flags = listOf("social", "gamification", "leaderboard", "advanced_charts", "csv_import", "custom_templates", "dark_mode", "multi_lang", "subscription")
        flags.forEach { featureFlagRepo.setFlag(it, true) }
    }

    fun loadWorkouts(userId: String) {
        viewModelScope.launch {
            _workouts.value = antrenamentRepo.getAntrenamente(userId)
        }
    }

    fun loadExercises() {
        viewModelScope.launch {
            _exercises.value = antrenamentRepo.getAllExercises()
        }
    }

    fun saveWorkout(userId: String, session: TrainingSession, notes: String = "") {
        viewModelScope.launch {
            antrenamentRepo.saveAntrenament(userId, session, notes)
            streakManager.recordWorkout(userId)
            badgeEngine.checkAndAward(userId)
            loadWorkouts(userId)
            loadExercises()
            computeVolumes(userId)
            loadPersonalRecords(userId)
        }
    }

    fun deleteWorkout(antrenamentId: Long, userId: String) {
        viewModelScope.launch {
            antrenamentRepo.deleteAntrenament(antrenamentId)
            loadWorkouts(userId)
            computeVolumes(userId)
        }
    }

    fun loadExerciseHistory(userId: String, exerciseName: String) {
        viewModelScope.launch {
            _exerciseHistory.value = antrenamentRepo.getExerciseHistory(userId, exerciseName)
            _bestSet.value = antrenamentRepo.getBestSet(userId, exerciseName)
        }
    }

    fun loadPersonalRecords(userId: String) {
        viewModelScope.launch {
            _personalRecords.value = antrenamentRepo.getPersonalRecordsSorted(userId)
        }
    }

    fun computeVolumes(userId: String) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = cal.timeInMillis

            cal.timeInMillis = dayStart
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            val weekStart = cal.timeInMillis
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            val weekEnd = cal.timeInMillis

            cal.timeInMillis = dayStart
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val monthEnd = cal.timeInMillis

            _dailyVolume.value = antrenamentRepo.getTotalVolume(userId, dayStart, dayEnd)
            _weeklyVolume.value = antrenamentRepo.getTotalVolume(userId, weekStart, weekEnd)
            _monthlyVolume.value = antrenamentRepo.getTotalVolume(userId, monthStart, monthEnd)
            _weeklyTotalWeight.value = antrenamentRepo.getTotalVolume(userId, weekStart, weekEnd)

            val mostFrequent = antrenamentRepo.getMostFrequentExercise(userId, weekStart, weekEnd)
            _mostFrequentExercise.value = mostFrequent?.numeExercitiu
        }
    }

    fun addCustomExercise(name: String, group: String) {
        viewModelScope.launch {
            antrenamentRepo.addCustomExercise(name, group)
            loadExercises()
        }
    }

    fun toggleFavorite(name: String) {
        viewModelScope.launch {
            antrenamentRepo.toggleFavorite(name)
            loadExercises()
        }
    }

    fun loadWorkoutExercises(antrenamentId: Long) {
        viewModelScope.launch {
            _workoutExercises.value = antrenamentRepo.getExercitii(antrenamentId)
        }
    }

    fun updateExerciseSet(exerciseId: Long, weight: Double, reps: Int) {
        viewModelScope.launch {
            antrenamentRepo.updateExerciseSet(exerciseId, weight, reps)
        }
    }

    fun deleteExerciseSet(exerciseId: Long) {
        viewModelScope.launch {
            antrenamentRepo.deleteExerciseSet(exerciseId)
        }
    }

    fun updateWorkoutNotes(antrenamentId: Long, notes: String) {
        viewModelScope.launch {
            antrenamentRepo.updateWorkoutNotes(antrenamentId, notes)
        }
    }

    fun updateExerciseNotes(exerciseId: Long, notes: String) {
        viewModelScope.launch {
            antrenamentRepo.updateExerciseNotes(exerciseId, notes)
        }
    }

    fun createPost(userId: String, content: String) {
        viewModelScope.launch {
            socialRepo.createPost(userId, content)
        }
    }

    // ========== Simple callback-style methods for dark-theme UI ==========

    fun getExercitiiPentruGrupa(grupa: String, callback: (List<ExerciseListItem>) -> Unit) {
        viewModelScope.launch {
            val result = antrenamentRepo.getExercitiiPentruGrupaSimple(grupa)
            callback(result)
        }
    }

    fun adaugaExercitiuCustom(grupa: String, nume: String, callback: () -> Unit) {
        viewModelScope.launch {
            antrenamentRepo.adaugaExercitiuCustom(grupa, nume)
            callback()
        }
    }

    fun setFavorite(grupa: String, numeExercitiu: String, isFavorite: Boolean, callback: () -> Unit) {
        viewModelScope.launch {
            antrenamentRepo.setFavoriteSimple(grupa, numeExercitiu, isFavorite)
            callback()
        }
    }

    fun salveazaAntrenament(grupaMusculara: String, numeExercitiu: String, seturi: List<SetEntry>, note: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isNewPR = antrenamentRepo.salveazaAntrenamentSimple(grupaMusculara, numeExercitiu, seturi, note)
            callback(isNewPR)
        }
    }

    fun getIstoricExercitiu(exerciseName: String, callback: (List<ExercitiuEntity>) -> Unit) {
        viewModelScope.launch {
            val result = antrenamentRepo.getIstoricExercitiu(exerciseName)
            callback(result)
        }
    }

    fun getStatisticiExercitiu(exerciseName: String, callback: (ExerciseStats) -> Unit) {
        viewModelScope.launch {
            val result = antrenamentRepo.getStatisticiExercitiu(exerciseName)
            callback(result)
        }
    }

    fun getVolumeSummary(callback: (VolumeSummary) -> Unit) {
        viewModelScope.launch {
            val result = antrenamentRepo.getVolumeSummary()
            callback(result)
        }
    }

    fun getRecuperareMusculara(grupa: String, callback: (Double) -> Unit) {
        viewModelScope.launch {
            val result = antrenamentRepo.getRecuperareMusculara(grupa)
            callback(result)
        }
    }

    fun getToateRecuperarile(callback: (List<Pair<String, Double>>) -> Unit) {
        viewModelScope.launch {
            val result = antrenamentRepo.getToateRecuperarile()
            callback(result)
        }
    }

    fun incarcaUltimulAntrenament(exerciseName: String, callback: (List<SetEntry>) -> Unit) {
        viewModelScope.launch {
            val result = antrenamentRepo.incarcaUltimulAntrenament(exerciseName)
            callback(result)
        }
    }

    fun updateSet(updated: ExercitiuEntity, callback: () -> Unit) {
        viewModelScope.launch {
            antrenamentRepo.updateSetSimple(updated)
            callback()
        }
    }

    fun deleteSet(set: ExercitiuEntity, callback: () -> Unit) {
        viewModelScope.launch {
            antrenamentRepo.deleteSetSimple(set)
            callback()
        }
    }

    fun getProgresLunar(exerciseName: String, callback: (List<ProgresLunar>) -> Unit) {
        viewModelScope.launch {
            val result = antrenamentRepo.getProgresLunar(exerciseName)
            callback(result)
        }
    }

    companion object {
        private val recoveryHoursMap = mapOf(
            "Abdomen" to 48L,
            "Biceps" to 36L,
            "Triceps" to 48L,
            "Umeri" to 48L,
            "Piept" to 48L,
            "Spate" to 72L,
            "Picioare" to 72L,
            "Fese" to 72L,
            "Gambe" to 36L
        )

        fun calculeazaTimpRamas(level: Double, grupa: String): Long {
            if (level <= 0.05) return 0
            val hours = recoveryHoursMap[grupa] ?: 48L
            val recoveryMs = hours * 3_600_000
            return (level * recoveryMs).toLong()
        }
    }
}
