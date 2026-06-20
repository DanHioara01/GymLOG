package com.example.gymlog2

data class UserOnboardingProfile(
    val goal: String = "",
    val experience: String = "",
    val equipment: String = "",
    val sessionsPerWeek: Int = 3,
    val limitations: String = "",
    val selectedGroups: List<String> = emptyList()
)

data class ExerciseRecommendation(
    val name: String,
    val group: String,
    val sets: Int,
    val reps: String,
    val note: String = ""
)

data class FitnessTip(
    val text: String
)

object FitnessAssistant {

    fun generateWorkout(profile: UserOnboardingProfile): List<ExerciseRecommendation> {
        val exercises = mutableListOf<ExerciseRecommendation>()
        val groups = profile.selectedGroups.ifEmpty { listOf("chest", "back", "legs") }

        for (group in groups) {
            val groupExercises = getExercisesForGroup(group, profile.goal, profile.equipment, profile.experience)
            exercises.addAll(groupExercises)
        }

        return distributeAcrossGroups(exercises, groups)
    }

    private fun getExercisesForGroup(
        group: String,
        goal: String,
        equipment: String,
        experience: String
    ): List<ExerciseRecommendation> {
        val isStrength = goal == "strength"
        val isWeightLoss = goal == "weight_loss"
        val isMass = goal == "mass"

        val sets = when {
            isStrength && experience == "advanced" -> 5
            isStrength && experience == "intermediate" -> 4
            isStrength -> 3
            isMass && experience == "advanced" -> 4
            isMass -> 3
            isWeightLoss -> 3
            else -> 3
        }

        val repRange = when {
            isStrength && experience == "advanced" -> "3-5"
            isStrength && experience == "intermediate" -> "4-6"
            isStrength -> "5-8"
            isMass && experience == "advanced" -> "8-12"
            isMass -> "10-12"
            isWeightLoss -> "12-15"
            else -> "8-12"
        }

        return when (group) {
            "chest" -> listOf(
                ExerciseRecommendation("Barbell Bench Press", "chest", sets, repRange, if (isStrength) "Focus on progressive overload" else "Control the eccentric"),
                ExerciseRecommendation("Incline Dumbbell Press", "chest", sets, repRange),
                ExerciseRecommendation("Cable Flyes", "chest", sets - 1, if (isWeightLoss) "15-20" else "10-12", "Squeeze at the top")
            )
            "back" -> listOf(
                ExerciseRecommendation("Barbell Rows", "back", sets, repRange, "Keep back straight"),
                ExerciseRecommendation("Pull-ups / Lat Pulldown", "back", sets, repRange),
                ExerciseRecommendation("Seated Cable Row", "back", sets - 1, if (isWeightLoss) "12-15" else "10-12", "Retract shoulder blades")
            )
            "legs" -> listOf(
                ExerciseRecommendation("Barbell Squat", "legs", sets, repRange, "Depth below parallel"),
                ExerciseRecommendation("Romanian Deadlift", "legs", sets, repRange, "Feel the hamstring stretch"),
                ExerciseRecommendation("Leg Press", "legs", sets - 1, repRange),
                ExerciseRecommendation("Leg Curls", "legs", sets - 1, if (isWeightLoss) "15-20" else "10-12")
            )
            "shoulders" -> listOf(
                ExerciseRecommendation("Overhead Press", "shoulders", sets, repRange, "Brace core"),
                ExerciseRecommendation("Lateral Raises", "shoulders", sets - 1, if (isWeightLoss) "15-20" else "12-15", "Light weight, controlled"),
                ExerciseRecommendation("Face Pulls", "shoulders", sets - 1, "15-20", "Great for posture")
            )
            "arms" -> listOf(
                ExerciseRecommendation("Barbell Curls", "biceps", sets - 1, repRange),
                ExerciseRecommendation("Tricep Pushdowns", "triceps", sets - 1, repRange),
                ExerciseRecommendation("Hammer Curls", "biceps", sets - 1, if (isWeightLoss) "12-15" else "10-12"),
                ExerciseRecommendation("Overhead Tricep Extension", "triceps", sets - 1, repRange)
            )
            "core" -> listOf(
                ExerciseRecommendation("Hanging Leg Raises", "core", 3, if (isWeightLoss) "15-20" else "10-15"),
                ExerciseRecommendation("Cable Crunches", "core", 3, "12-15"),
                ExerciseRecommendation("Plank Hold", "core", 3, "30-60s", "Keep body straight")
            )
            "cardio" -> listOf(
                ExerciseRecommendation("Treadmill Intervals", "cardio", 1, "20-30 min", if (isWeightLoss) "Alternate 30s sprint / 60s walk" else "Moderate pace"),
                ExerciseRecommendation("Rowing Machine", "cardio", 1, "15-20 min", "Full body engagement")
            )
            else -> emptyList()
        }
    }

    private fun distributeAcrossGroups(
        exercises: List<ExerciseRecommendation>,
        groups: List<String>
    ): List<ExerciseRecommendation> {
        if (groups.size <= 2) return exercises
        val perGroup = exercises.size / groups.size
        return exercises.take(perGroup * groups.size)
    }

    fun generateTips(profile: UserOnboardingProfile): List<FitnessTip> {
        val tips = mutableListOf<FitnessTip>()

        when (profile.goal) {
            "strength" -> {
                tips.add(FitnessTip("Focus on progressive overload — add small weight each week to keep getting stronger."))
                tips.add(FitnessTip("Rest 2-3 minutes between heavy sets to fully recover for maximum power."))
            }
            "mass" -> {
                tips.add(FitnessTip("Aim for 1.6-2.2g of protein per kg of bodyweight daily to support muscle growth."))
                tips.add(FitnessTip("Control the eccentric (lowering) phase — 3 seconds down builds more muscle."))
            }
            "weight_loss" -> {
                tips.add(FitnessTip("Keep rest periods short (30-60 seconds) between sets to keep your heart rate up."))
                tips.add(FitnessTip("A slight calorie deficit of 300-500 kcal is enough — don't starve yourself."))
            }
            "maintenance" -> {
                tips.add(FitnessTip("Consistency beats perfection — 3 solid sessions per week is enough."))
                tips.add(FitnessTip("Mix compound movements with isolation work for balanced fitness."))
            }
        }

        when (profile.experience) {
            "beginner" -> {
                tips.add(FitnessTip("Master form with lighter weights first — good technique prevents injury and speeds up progress."))
            }
            "advanced" -> {
                tips.add(FitnessTip("Consider periodization — rotate between heavy/light weeks to avoid plateaus."))
            }
        }

        if (profile.selectedGroups.contains("legs")) {
            tips.add(FitnessTip("Don't skip leg day! Strong legs boost testosterone and overall strength."))
        }

        tips.add(FitnessTip("Sleep 7-9 hours per night — that's when your muscles actually grow and recover."))
        tips.add(FitnessTip("Drink at least 2-3 liters of water daily, more on training days."))

        return tips.take(4)
    }
}
