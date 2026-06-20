package com.example.gymlog2

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CsvExporter(private val context: Context) {
    fun exportWorkouts(workouts: List<AntrenamentEntity>, exercises: Map<Long, List<ExercitiuEntity>>): Uri {
        val file = File(context.cacheDir, "kinetic_export_${System.currentTimeMillis()}.csv")
        file.bufferedWriter().use { w ->
            w.write("Date,Group,Exercise,Set,WeightKg,Reps")
            w.newLine()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            for (aw in workouts) {
                val exList = exercises[aw.id] ?: emptyList()
                for (ex in exList) {
                    w.write("${sdf.format(Date(aw.data))},${aw.grupaMusculara},${ex.numeExercitiu},${ex.setIndex + 1},${ex.greutateKg},${ex.repetari}")
                    w.newLine()
                }
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
