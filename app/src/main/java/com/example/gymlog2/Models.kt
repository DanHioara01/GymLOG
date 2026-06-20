package com.example.gymlog2

import java.util.Date

data class SetEntry(
    val greutateKg: Double,
    val repetari: Int
)

data class ExerciseListItem(
    val exercise: ExerciseDefinition,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    val equipment: String = ""
)

data class TemplateExercise(
    val grupaMusculara: String,
    val exercise: ExerciseDefinition
)

data class WorkoutTemplate(
    val nume: String,
    val exercitii: List<TemplateExercise>
)

data class ExerciseEntry(
    val numeExercitiu: String,
    val seturi: List<SetEntry> = listOf(),
    val notes: String = ""
)

data class TrainingSession(
    val grupaMusculara: String,
    val data: Date = Date(),
    val exercitii: List<ExerciseEntry> = listOf()
)

data class ExerciseStats(
    val maxGreutate: Double,
    val maxRepetari: Int,
    val maxVolumSet: Double
)

data class VolumeSummary(
    val azi: Double,
    val saptamana: Double,
    val luna: Double
)

data class ProgresLunar(
    val luna: String,
    val greutateMaxima: Double
)
