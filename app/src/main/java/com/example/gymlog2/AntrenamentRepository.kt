package com.example.gymlog2

import java.util.Calendar

class AntrenamentRepository(private val db: AppDatabase) {

    suspend fun saveAntrenament(userId: String, session: TrainingSession, notes: String = ""): Long {
        val totalWeight = session.exercitii.sumOf { ex ->
            ex.seturi.sumOf { it.greutateKg * it.repetari }
        }
        val antrenamentId = db.antrenamentDao().insert(
            AntrenamentEntity(userId = userId, grupaMusculara = session.grupaMusculara, data = session.data.time, totalWeight = totalWeight, notes = notes)
        )
        val entries = mutableListOf<ExercitiuEntity>()
        session.exercitii.forEach { ex ->
            ex.seturi.forEachIndexed { idx, set ->
                entries.add(ExercitiuEntity(antrenamentId = antrenamentId, numeExercitiu = ex.numeExercitiu, setIndex = idx, greutateKg = set.greutateKg, repetari = set.repetari, notes = ex.notes))
            }
        }
        db.exercitiuDao().insertAll(entries)

        session.exercitii.forEach { ex ->
            val bestSet = ex.seturi.maxByOrNull { it.greutateKg }
            if (bestSet != null) {
                val volume = ex.seturi.sumOf { it.greutateKg * it.repetari }
                val existingPr = db.personalRecordDao().getBest(userId, ex.numeExercitiu)
                if (existingPr == null || bestSet.greutateKg > existingPr.weight) {
                    db.personalRecordDao().upsert(
                        PersonalRecordEntity(
                            userId = userId,
                            exerciseName = ex.numeExercitiu,
                            weight = bestSet.greutateKg,
                            reps = bestSet.repetari,
                            volume = volume,
                            date = System.currentTimeMillis()
                        )
                    )
                }
            }
            db.exerciseDefinitionDao().incrementUsage(ex.numeExercitiu)
        }

        return antrenamentId
    }

    suspend fun getAntrenamente(userId: String): List<AntrenamentEntity> {
        return db.antrenamentDao().getAllForUser(userId)
    }

    suspend fun getExercitii(antrenamentId: Long): List<ExercitiuEntity> {
        return db.exercitiuDao().getForAntrenament(antrenamentId)
    }

    suspend fun deleteAntrenament(antrenamentId: Long) {
        db.exercitiuDao().deleteForAntrenament(antrenamentId)
        db.antrenamentDao().deleteById(antrenamentId)
    }

    suspend fun getAllExercises(): List<ExerciseDefinitionEntity> {
        return db.exerciseDefinitionDao().getAll()
    }

    suspend fun getExercisesByGroup(group: String): List<ExerciseDefinitionEntity> {
        return db.exerciseDefinitionDao().getByGroup(group)
    }

    suspend fun getExerciseHistory(userId: String, exerciseName: String): List<ExercitiuEntity> {
        return db.exercitiuDao().getHistoryForExercise(userId, exerciseName)
    }

    suspend fun getBestSet(userId: String, exerciseName: String): ExercitiuEntity? {
        return db.exercitiuDao().getBestSetForExercise(userId, exerciseName)
    }

    suspend fun getAllPersonalRecords(userId: String): List<PersonalRecordEntity> {
        return db.personalRecordDao().getAllForUser(userId)
    }

    suspend fun getPersonalRecordsSorted(userId: String): List<PersonalRecordEntity> {
        return db.personalRecordDao().getAllSortedByWeight(userId)
    }

    suspend fun getTotalVolume(userId: String, startTime: Long, endTime: Long): Double {
        return db.antrenamentDao().getTotalVolume(userId, startTime, endTime) ?: 0.0
    }

    suspend fun getMostFrequentExercise(userId: String, startTime: Long, endTime: Long): MostFrequentExercise? {
        return db.exercitiuDao().getMostFrequentExercise(userId, startTime, endTime)
    }

    suspend fun getWorkoutsInPeriod(userId: String, startTime: Long, endTime: Long): List<AntrenamentEntity> {
        return db.antrenamentDao().getWorkoutsInPeriod(userId, startTime, endTime)
    }

    suspend fun addCustomExercise(name: String, group: String) {
        db.exerciseDefinitionDao().upsert(
            ExerciseDefinitionEntity(name = name, group = group, isDefault = false)
        )
    }

    suspend fun toggleFavorite(name: String) {
        val exercise = db.exerciseDefinitionDao().getByName(name)
        if (exercise != null) {
            db.exerciseDefinitionDao().setFavorite(name, !exercise.isFavorite)
        }
    }

    suspend fun updateWorkoutNotes(antrenamentId: Long, notes: String) {
        val antrenament = db.antrenamentDao().getById(antrenamentId)
        if (antrenament != null) {
            db.antrenamentDao().update(antrenament.copy(notes = notes))
        }
    }

    suspend fun updateExerciseNotes(exerciseId: Long, notes: String) {
        val exercise = db.exercitiuDao().getForAntrenament(0).find { it.id == exerciseId }
        if (exercise != null) {
            db.exercitiuDao().update(exercise.copy(notes = notes))
        }
    }

    suspend fun deleteExerciseSet(exerciseId: Long) {
        val exercise = db.exercitiuDao().getForAntrenament(0).find { it.id == exerciseId }
        if (exercise != null) {
            db.exercitiuDao().delete(exercise)
        }
    }

    suspend fun updateExerciseSet(exerciseId: Long, weight: Double, reps: Int) {
        val exercise = db.exercitiuDao().getForAntrenament(0).find { it.id == exerciseId }
        if (exercise != null) {
            db.exercitiuDao().update(exercise.copy(greutateKg = weight, repetari = reps))
        }
    }

    // ========== Simple callback-style methods for dark-theme UI ==========

    suspend fun getExercitiiPentruGrupaSimple(grupa: String): List<ExerciseListItem> {
        val dbExercises = db.exerciseDefinitionDao().getByGroup(grupa)
        val dbExMap = dbExercises.associateBy { it.name }
        val metadata = db.exerciseMetadataDao().getByGroup(grupa)
        val metaMap = metadata.associateBy { it.exerciseName }

        val hardcoded = DataProvider.exercitiiPeGrupa[grupa] ?: listOf()
        val customFromMeta = metadata.filter { it.isCustom }

        val allItems = mutableListOf<ExerciseListItem>()
        hardcoded.forEach { ex ->
            val meta = metaMap[ex.name]
            val equipment = dbExMap[ex.name]?.equipment ?: ex.equipment
            allItems.add(ExerciseListItem(ex, isFavorite = meta?.isFavorite ?: false, equipment = equipment))
        }
        customFromMeta.forEach { me ->
            if (hardcoded.none { it.name == me.exerciseName }) {
                val equipment = dbExMap[me.exerciseName]?.equipment ?: ""
                allItems.add(
                    ExerciseListItem(
                        ExerciseDefinition(me.exerciseName, me.grupaMusculara),
                        isFavorite = me.isFavorite,
                        isCustom = true,
                        equipment = equipment
                    )
                )
            }
        }
        return allItems.sortedByDescending { it.isFavorite }
    }

    suspend fun adaugaExercitiuCustom(grupa: String, nume: String) {
        val existing = db.exerciseMetadataDao().getByName(nume)
        if (existing == null) {
            db.exerciseMetadataDao().upsert(
                ExerciseMetadataEntity(exerciseName = nume, grupaMusculara = grupa, isFavorite = false, isCustom = true)
            )
        }
    }

    suspend fun setFavoriteSimple(grupa: String, numeExercitiu: String, isFavorite: Boolean) {
        val existing = db.exerciseMetadataDao().getByName(numeExercitiu)
        if (existing != null) {
            db.exerciseMetadataDao().upsert(existing.copy(isFavorite = isFavorite))
        } else {
            db.exerciseMetadataDao().upsert(
                ExerciseMetadataEntity(exerciseName = numeExercitiu, grupaMusculara = grupa, isFavorite = isFavorite)
            )
        }
    }

    suspend fun salveazaAntrenamentSimple(grupaMusculara: String, numeExercitiu: String, seturi: List<SetEntry>, note: String): Boolean {
        val totalWeight = seturi.sumOf { it.greutateKg * it.repetari }
        val antrenamentId = db.antrenamentDao().insert(
            AntrenamentEntity(userId = "simple", grupaMusculara = grupaMusculara, data = System.currentTimeMillis(), totalWeight = totalWeight, notes = note)
        )
        val entries = seturi.mapIndexed { idx, set ->
            ExercitiuEntity(antrenamentId = antrenamentId, numeExercitiu = numeExercitiu, setIndex = idx, greutateKg = set.greutateKg, repetari = set.repetari, notes = note)
        }
        db.exercitiuDao().insertAll(entries)

        val bestSet = seturi.maxByOrNull { it.greutateKg }
        var isNewPR = false
        if (bestSet != null) {
            val volume = seturi.sumOf { it.greutateKg * it.repetari }
            val existingPr = db.personalRecordDao().getBest("simple", numeExercitiu)
            if (existingPr == null || bestSet.greutateKg > existingPr.weight) {
                db.personalRecordDao().upsert(
                    PersonalRecordEntity(userId = "simple", exerciseName = numeExercitiu, weight = bestSet.greutateKg, reps = bestSet.repetari, volume = volume, date = System.currentTimeMillis())
                )
                isNewPR = true
            }
        }
        db.exerciseDefinitionDao().incrementUsage(numeExercitiu)

        val lastSet = seturi.lastOrNull()
        if (lastSet != null) {
            updateMuscleRecovery(grupaMusculara, seturi.size)
        }

        return isNewPR
    }

    suspend fun getIstoricExercitiu(exerciseName: String): List<ExercitiuEntity> {
        return db.exercitiuDao().getHistoryForExerciseSimple(exerciseName)
    }

    suspend fun getStatisticiExercitiu(exerciseName: String): ExerciseStats {
        val history = db.exercitiuDao().getHistoryForExerciseSimple(exerciseName)
        val maxGreutate = history.maxOfOrNull { it.greutateKg } ?: 0.0
        val maxRepetari = history.maxOfOrNull { it.repetari } ?: 0
        val maxVolumSet = history.maxOfOrNull { it.greutateKg * it.repetari } ?: 0.0
        return ExerciseStats(maxGreutate, maxRepetari, maxVolumSet)
    }

    suspend fun getVolumeSummary(): VolumeSummary {
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

        val azi = db.antrenamentDao().getTotalVolume("simple", dayStart, dayEnd) ?: 0.0
        val saptamana = db.antrenamentDao().getTotalVolume("simple", weekStart, weekEnd) ?: 0.0
        val luna = db.antrenamentDao().getTotalVolume("simple", monthStart, monthEnd) ?: 0.0
        return VolumeSummary(azi, saptamana, luna)
    }

    suspend fun getRecuperareMusculara(grupa: String): Double {
        val entity = db.muscleRecoveryDao().getByGroup(grupa) ?: return 0.0
        val recoveryHours = getRecoveryHoursForGroup(grupa)
        val elapsedMs = System.currentTimeMillis() - entity.lastUpdated
        val recoveryMs = recoveryHours * 3_600_000
        val drain = elapsedMs.toDouble() / recoveryMs
        return (entity.level - drain).coerceIn(0.0, 1.0)
    }

    suspend fun getToateRecuperarile(): List<Pair<String, Double>> {
        val allGroups = DataProvider.grupeMusculare
        return allGroups.map { grupa ->
            grupa to getRecuperareMusculara(grupa)
        }
    }

    suspend fun updateMuscleRecovery(grupa: String, numSets: Int) {
        val currentLevel = getRecuperareMusculara(grupa)
        val fatiguePerSet = 0.12
        val newLevel = (currentLevel + numSets * fatiguePerSet).coerceAtMost(1.0)
        db.muscleRecoveryDao().upsert(
            MuscleRecoveryEntity(grupaMusculara = grupa, level = newLevel, lastUpdated = System.currentTimeMillis())
        )
    }

    suspend fun incarcaUltimulAntrenament(exerciseName: String): List<SetEntry> {
        val history = db.exercitiuDao().getHistoryForExerciseSimple(exerciseName)
        if (history.isEmpty()) return listOf(SetEntry(0.0, 0))

        val latestAntrenamentId = history.firstOrNull()?.antrenamentId ?: return listOf(SetEntry(0.0, 0))
        val latestSets = db.exercitiuDao().getForAntrenament(latestAntrenamentId)
        return latestSets.map { SetEntry(it.greutateKg, it.repetari) }
    }

    suspend fun updateSetSimple(updated: ExercitiuEntity) {
        db.exercitiuDao().update(updated)
    }

    suspend fun deleteSetSimple(set: ExercitiuEntity) {
        db.exercitiuDao().delete(set)
    }

    suspend fun getProgresLunar(exerciseName: String): List<ProgresLunar> {
        val history = db.exercitiuDao().getHistoryForExerciseSimple(exerciseName)
        val grouped = mutableMapOf<String, Double>()
        for (entity in history) {
            val cal = Calendar.getInstance().apply { timeInMillis = entity.antrenamentId.let { _ ->
                val antrenament = db.antrenamentDao().getById(entity.antrenamentId)
                antrenament?.data ?: 0L
            }}
            val monthKey = "%d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            val current = grouped[monthKey] ?: 0.0
            if (entity.greutateKg > current) {
                grouped[monthKey] = entity.greutateKg
            }
        }
        return grouped.map { (luna, greutate) -> ProgresLunar(luna, greutate) }.sortedBy { it.luna }
    }

    private fun getRecoveryHoursForGroup(grupa: String): Long {
        return when (grupa) {
            "Abdomen" -> 48
            "Biceps" -> 36
            "Triceps" -> 48
            "Umeri" -> 48
            "Piept" -> 48
            "Spate" -> 72
            "Picioare" -> 72
            "Fese" -> 72
            "Gambe" -> 36
            "Antebrate" -> 36
            "Gat & Trapezi" -> 48
            else -> 48
        }
    }
}
