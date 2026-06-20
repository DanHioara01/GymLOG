package com.example.gymlog2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog2.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun WorkoutDistributionCard(isDark: Boolean = true) {
    val context = LocalContext.current
    val strings = LanguageManager.getStrings(context)
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val cardBg = if (isDark) cardColor() else LightCard
    val accent = if (isDark) accentColor() else LightPrimaryRed
    var groupWorkoutCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val monthStart = cal.timeInMillis
            val monthEnd = System.currentTimeMillis()
            val workouts = db.antrenamentDao().getWorkoutsInPeriod("simple", monthStart, monthEnd)
            val counts = mutableMapOf<String, Int>()
            for (w in workouts) {
                counts[w.grupaMusculara] = (counts[w.grupaMusculara] ?: 0) + 1
            }
            groupWorkoutCounts = counts
        }
    }

    if (groupWorkoutCounts.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    strings.workoutDistribution,
                    style = MaterialTheme.typography.titleMedium,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    strings.thisMonth,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
                Spacer(Modifier.height(12.dp))
                val maxCount = groupWorkoutCounts.values.maxOrNull() ?: 1
                val sortedGroups = groupWorkoutCounts.entries.sortedByDescending { it.value }
                sortedGroups.forEach { (group, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            LanguageManager.translateMuscleGroup(group, strings),
                            color = textPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.width(70.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(dividerColor())
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = count.toFloat() / maxCount)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accent)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$count",
                            color = textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.width(24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
