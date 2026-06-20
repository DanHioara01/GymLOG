package com.example.gymlog2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog2.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private val GoldPR = Color(0xFFF5B942)

private data class ChartData(
    val values: List<Double>,
    val labels: List<String>,
    val label: String,
    val unit: String,
    val delta: String
)

@Composable
fun StatsScreen(
    isDark: Boolean,
    isLbs: Boolean,
    strings: LanguageManager.Strings,
    weeklyTopExercise: String?,
    weeklyTotalKg: Double,
    lastPR: PersonalRecordEntity?,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onExerciseHistoryClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val surfaceBg = if (isDark) bgColor() else LightBackground
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val cardBg = if (isDark) cardColor() else LightCard
    val accent = if (isDark) accentColor() else LightPrimaryRed
    val dividerBg = if (isDark) dividerColor() else Color(0xFF2A2A2A)

    var selectedMetric by remember { mutableIntStateOf(0) }
    var selectedPeriod by remember { mutableIntStateOf(0) }
    var chartData by remember { mutableStateOf<Map<Int, Map<Int, ChartData>>>(emptyMap()) }
    var distributionData by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val cal = Calendar.getInstance()
            val now = cal.timeInMillis

            val result = mutableMapOf<Int, MutableMap<Int, ChartData>>()

            for (periodIdx in 0..2) {
                val days = when (periodIdx) { 0 -> 7; 1 -> 30; 2 -> 90; else -> 7 }
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_YEAR, -days)
                val startTime = cal.timeInMillis

                val workouts = db.antrenamentDao().getWorkoutsInPeriod("simple", startTime, now)

                val dayBuckets = mutableMapOf<Int, MutableList<Double>>()
                val maxWeightBuckets = mutableMapOf<Int, MutableList<Double>>()
                val setsBuckets = mutableMapOf<Int, MutableList<Int>>()

                for (w in workouts) {
                    val dayCal = Calendar.getInstance().apply { timeInMillis = w.data }
                    val dayOfYear = dayCal.get(Calendar.DAY_OF_YEAR)
                    val year = dayCal.get(Calendar.YEAR)
                    val key = year * 1000 + dayOfYear

                    dayBuckets.getOrPut(key) { mutableListOf() }.add(w.totalWeight)
                    maxWeightBuckets.getOrPut(key) { mutableListOf() }.add(w.totalWeight)
                    setsBuckets.getOrPut(key) { mutableListOf() }.add(1)
                }

                val dateFmt = if (days <= 7) SimpleDateFormat("EEE", Locale.getDefault())
                    else SimpleDateFormat("dd", Locale.getDefault())

                val volumeValues = mutableListOf<Double>()
                val volumeLabels = mutableListOf<String>()
                val maxWeightValues = mutableListOf<Double>()
                val maxWeightLabels = mutableListOf<String>()
                val setsValues = mutableListOf<Double>()
                val setsLabels = mutableListOf<String>()

                val sortedKeys = dayBuckets.keys.sorted().takeLast(days)

                if (days <= 7) {
                    for (i in 0 until 7) {
                        val dayCal = Calendar.getInstance().apply {
                            timeInMillis = now
                            add(Calendar.DAY_OF_YEAR, -(6 - i))
                        }
                        val key = dayCal.get(Calendar.YEAR) * 1000 + dayCal.get(Calendar.DAY_OF_YEAR)
                        val dayWorkouts = workouts.filter {
                            val wc = Calendar.getInstance().apply { timeInMillis = it.data }
                            wc.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR) &&
                                    wc.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR)
                        }
                        volumeValues.add(dayWorkouts.sumOf { it.totalWeight })
                        volumeLabels.add(dateFmt.format(dayCal.time).take(3))
                        val maxW = dayWorkouts.maxOfOrNull { it.totalWeight } ?: 0.0
                        maxWeightValues.add(maxW)
                        maxWeightLabels.add(dateFmt.format(dayCal.time).take(3))
                        setsValues.add(dayWorkouts.size.toDouble())
                        setsLabels.add(dateFmt.format(dayCal.time).take(3))
                    }
                } else if (days <= 30) {
                    val weeks = (days / 7) + 1
                    for (w in 0 until weeks) {
                        val weekStartCal = Calendar.getInstance().apply {
                            timeInMillis = now
                            add(Calendar.DAY_OF_YEAR, -((weeks - 1 - w) * 7))
                        }
                        val weekEndCal = Calendar.getInstance().apply {
                            timeInMillis = weekStartCal.timeInMillis
                            add(Calendar.DAY_OF_YEAR, 7)
                        }
                        val weekWorkouts = workouts.filter { it.data >= weekStartCal.timeInMillis && it.data < weekEndCal.timeInMillis }
                        volumeValues.add(weekWorkouts.sumOf { it.totalWeight })
                        volumeLabels.add("S${w + 1}")
                        val maxW = weekWorkouts.maxOfOrNull { it.totalWeight } ?: 0.0
                        maxWeightValues.add(maxW)
                        maxWeightLabels.add("S${w + 1}")
                        setsValues.add(weekWorkouts.size.toDouble())
                        setsLabels.add("S${w + 1}")
                    }
                } else {
                    val months = 3
                    for (m in 0 until months) {
                        val monthCal = Calendar.getInstance().apply {
                            timeInMillis = now
                            add(Calendar.MONTH, -(months - 1 - m))
                        }
                        val mStart = Calendar.getInstance().apply {
                            timeInMillis = monthCal.timeInMillis
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        val mEnd = Calendar.getInstance().apply {
                            timeInMillis = mStart.timeInMillis
                            add(Calendar.MONTH, 1)
                        }
                        val monthWorkouts = workouts.filter { it.data >= mStart.timeInMillis && it.data < mEnd.timeInMillis }
                        volumeValues.add(monthWorkouts.sumOf { it.totalWeight })
                        val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
                        volumeLabels.add(monthFmt.format(mStart.time).take(3))
                        val maxW = monthWorkouts.maxOfOrNull { it.totalWeight } ?: 0.0
                        maxWeightValues.add(maxW)
                        maxWeightLabels.add(monthFmt.format(mStart.time).take(3))
                        setsValues.add(monthWorkouts.size.toDouble())
                        setsLabels.add(monthFmt.format(mStart.time).take(3))
                    }
                }

                result[periodIdx] = mutableMapOf(
                    0 to ChartData(volumeValues, volumeLabels, strings.volume, if (isLbs) "lbs" else "kg",
                        if (volumeValues.size >= 2) {
                            val prev = volumeValues.dropLast(1).average()
                            val curr = volumeValues.last()
                            val pct = if (prev > 0) ((curr - prev) / prev * 100).toInt() else 0
                            if (pct >= 0) "+$pct% ${strings.thisWeek.lowercase()}" else "$pct% ${strings.thisWeek.lowercase()}"
                        } else strings.thisWeek
                    ),
                    1 to ChartData(maxWeightValues, maxWeightLabels, strings.maxWeight, if (isLbs) "lbs" else "kg",
                        if (maxWeightValues.size >= 2) {
                            val prev = maxWeightValues.dropLast(1).average()
                            val curr = maxWeightValues.last()
                            val pct = if (prev > 0) ((curr - prev) / prev * 100).toInt() else 0
                            if (pct >= 0) "+$pct% ${strings.thisWeek.lowercase()}" else "$pct% ${strings.thisWeek.lowercase()}"
                        } else strings.thisWeek
                    ),
                    2 to ChartData(setsValues, setsLabels, strings.sets, "",
                        if (setsValues.size >= 2) {
                            val prev = setsValues.dropLast(1).average()
                            val curr = setsValues.last()
                            val pct = if (prev > 0) ((curr - prev) / prev * 100).toInt() else 0
                            if (pct >= 0) "+$pct% ${strings.thisWeek.lowercase()}" else "$pct% ${strings.thisWeek.lowercase()}"
                        } else strings.thisWeek
                    )
                )
            }

            chartData = result

            val cal2 = Calendar.getInstance()
            cal2.set(Calendar.DAY_OF_MONTH, 1)
            cal2.set(Calendar.HOUR_OF_DAY, 0); cal2.set(Calendar.MINUTE, 0)
            cal2.set(Calendar.SECOND, 0); cal2.set(Calendar.MILLISECOND, 0)
            val monthStart = cal2.timeInMillis
            val workoutsMonth = db.antrenamentDao().getWorkoutsInPeriod("simple", monthStart, now)
            val counts = mutableMapOf<String, Int>()
            for (w in workoutsMonth) {
                counts[w.grupaMusculara] = (counts[w.grupaMusculara] ?: 0) + 1
            }
            distributionData = counts
        }
    }

    val currentData = chartData[selectedPeriod]?.get(selectedMetric)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceBg)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding() + 8.dp,
            bottom = paddingValues.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, accent, RoundedCornerShape(24.dp))
                        .padding(horizontal = 32.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = strings.stats.uppercase(),
                        color = accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val metricLabels = listOf(strings.volume, strings.maxWeight, strings.sets)
                        metricLabels.forEachIndexed { idx, label ->
                            val isSelected = selectedMetric == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isSelected) accent else dividerBg,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .background(
                                        if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 7.dp)
                                    .let { mod ->
                                        mod.then(
                                            Modifier.padding(horizontal = 4.dp)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(onClick = { selectedMetric = idx }) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) accent else textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = currentData?.label?.uppercase() ?: "",
                                color = textSecondary,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = formatChartValue(currentData?.values?.lastOrNull() ?: 0.0),
                                    color = textPrimary,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (currentData?.unit?.isNotEmpty() == true) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentData.unit,
                                        color = textSecondary,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentData?.delta ?: "",
                                color = accent,
                                fontSize = 12.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val periodLabels = listOf("7${strings.days.take(1)}", "30${strings.days.take(1)}", "3l")
                            periodLabels.forEachIndexed { idx, label ->
                                val isSelected = selectedPeriod == idx
                                Box(
                                    modifier = Modifier
                                        .border(
                                            1.dp,
                                            if (isSelected) accent else dividerBg,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .background(
                                            if (isSelected) accent.copy(alpha = 0.12f) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TextButton(onClick = { selectedPeriod = idx }) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) accent else textSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentData != null && currentData.values.isNotEmpty() && currentData.values.any { it > 0 }) {
                        val values = currentData.values
                        val labels = currentData.labels
                        val maxVal = values.max()
                        val minVal = values.min()
                        val range = (maxVal - minVal).coerceAtLeast(1.0)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(78.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val padTop = 10f
                            val padBottom = 15f
                            val usableH = h - padTop - padBottom
                            val step = if (values.size > 1) w / (values.size - 1) else w

                            val path = Path()
                            values.forEachIndexed { i, v ->
                                val x = i * step
                                val y = padTop + usableH - ((v - minVal) / range * usableH).toFloat()
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            drawPath(
                                path = path,
                                color = accent,
                                style = Stroke(width = 2.5.dp.toPx())
                            )

                            if (values.isNotEmpty()) {
                                val lastX = (values.size - 1) * step
                                val lastY = padTop + usableH - ((values.last() - minVal) / range * usableH).toFloat()
                                drawCircle(
                                    color = accent,
                                    radius = 4.dp.toPx(),
                                    center = Offset(lastX, lastY)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            labels.forEach { label ->
                                Text(
                                    text = label,
                                    color = textSecondary,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(78.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(strings.noChartData, color = textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(GoldPR.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏆", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.lastPR.uppercase(),
                            color = textSecondary,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lastPR?.exerciseName ?: "--",
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        Text(
                            text = if (lastPR != null) "${weightLabel(lastPR.weight, isLbs)} x ${lastPR.reps}" else "",
                            color = GoldPR,
                            fontSize = 13.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(textSecondary.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📜", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.startHere.uppercase(),
                            color = textSecondary,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = weeklyTopExercise ?: "--",
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        TextButton(
                            onClick = {
                                weeklyTopExercise?.let { onExerciseHistoryClick(it) }
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = strings.exerciseHistory,
                                color = textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        if (distributionData.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.workoutDistribution,
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.thisMonth,
                            color = textSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        val maxCount = distributionData.values.maxOrNull() ?: 1
                        val sortedGroups = distributionData.entries.sortedByDescending { it.value }

                        sortedGroups.forEach { (group, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = LanguageManager.translateMuscleGroup(group, strings),
                                    color = textPrimary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(46.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .background(dividerBg, RoundedCornerShape(6.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = count.toFloat() / maxCount)
                                            .background(accent, RoundedCornerShape(6.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "$count",
                                    color = textSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(16.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatChartValue(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
