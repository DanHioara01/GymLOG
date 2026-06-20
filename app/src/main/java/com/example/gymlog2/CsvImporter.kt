package com.example.gymlog2

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

class CsvImporter(private val context: Context) {
    fun importWorkouts(uri: Uri): List<TrainingSession> {
        val sessions = mutableListOf<TrainingSession>()
        val exercisesByName = mutableMapOf<String, MutableList<ExercitiuEntity>>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val reader = BufferedReader(InputStreamReader(stream))
            val lines = reader.readLines()
            if (lines.size <= 1) return emptyList()
            for (i in 1 until lines.size) {
                val parts = lines[i].split(",")
                if (parts.size >= 6) {
                    val group = parts[1].trim()
                    val exerciseName = parts[2].trim()
                    val weight = parts[4].trim().toDoubleOrNull() ?: 0.0
                    val reps = parts[5].trim().toIntOrNull() ?: 0
                    val key = exerciseName
                    exercisesByName.getOrPut(key) { mutableListOf() }
                        .add(ExercitiuEntity(antrenamentId = 0, numeExercitiu = exerciseName, setIndex = exercisesByName[key]!!.size, greutateKg = weight, repetari = reps))
                }
            }
        }
        val grouped = exercisesByName.entries.groupBy { it.value.firstOrNull()?.numeExercitiu ?: "Unknown" }
        for ((name, _) in grouped) {
            val exList = exercisesByName[name] ?: emptyList()
            val group = exList.firstOrNull()?.let { "Mixed" } ?: "Unknown"
            sessions.add(TrainingSession(grupaMusculara = group, exercitii = listOf(
                ExerciseEntry(numeExercitiu = name, seturi = exList.map { SetEntry(it.greutateKg, it.repetari) })
            )))
        }
        return sessions
    }
}
