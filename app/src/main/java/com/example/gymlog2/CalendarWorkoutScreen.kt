package com.example.gymlog2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog2.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarWorkoutScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val strings = LanguageManager.getStrings(context)

    var currentMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedDay by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    var workoutDays by remember { mutableStateOf(setOf<Int>()) }
    var selectedDayWorkouts by remember { mutableStateOf<List<AntrenamentEntity>>(emptyList()) }
    var workoutGroupMap by remember { mutableStateOf<Map<Int, Set<String>>>(emptyMap()) }

    val monthNames = listOf(
        strings.jan, strings.feb, strings.mar, strings.apr, strings.may, strings.jun,
        strings.jul, strings.aug, strings.sep, strings.oct, strings.nov, strings.dec
    )

    LaunchedEffect(currentMonth, currentYear) {
        withContext(Dispatchers.IO) {
            val cal = Calendar.getInstance()
            cal.set(currentYear, currentMonth, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val monthStart = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val monthEnd = cal.timeInMillis

            val workouts = db.antrenamentDao().getWorkoutsInPeriod("simple", monthStart, monthEnd)
            val days = mutableSetOf<Int>()
            val groupMap = mutableMapOf<Int, MutableSet<String>>()
            for (w in workouts) {
                val dayCal = Calendar.getInstance().apply { timeInMillis = w.data }
                val day = dayCal.get(Calendar.DAY_OF_MONTH)
                days.add(day)
                groupMap.getOrPut(day) { mutableSetOf() }.add(w.grupaMusculara)
            }
            workoutDays = days
            workoutGroupMap = groupMap.mapValues { it.value.toSet() }
        }
    }

    LaunchedEffect(selectedDay, currentMonth, currentYear) {
        withContext(Dispatchers.IO) {
            val cal = Calendar.getInstance()
            cal.set(currentYear, currentMonth, selectedDay, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val dayEnd = cal.timeInMillis
            selectedDayWorkouts = db.antrenamentDao().getWorkoutsInPeriod("simple", dayStart, dayEnd)
        }
    }

    Scaffold(
        containerColor = bgColor(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(strings.calendarView) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = accentColor())
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor(), titleContentColor = textColor())
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor()),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (currentMonth == 0) { currentMonth = 11; currentYear-- }
                                else currentMonth--
                            }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = accentColor())
                            }
                            Text(
                                text = "${monthNames[currentMonth]} $currentYear",
                                style = MaterialTheme.typography.titleLarge,
                                color = textColor(),
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = {
                                if (currentMonth == 11) { currentMonth = 0; currentYear++ }
                                else currentMonth++
                            }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = accentColor())
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf(strings.mon, strings.tue, strings.wed, strings.thu, strings.fri, strings.sat, strings.sun).forEach { dayName ->
                                Text(
                                    text = dayName,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    color = secondaryTextColor(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val cal = Calendar.getInstance().apply {
                            set(currentYear, currentMonth, 1)
                        }
                        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val today = Calendar.getInstance()
                        val isCurrentMonth = today.get(Calendar.MONTH) == currentMonth && today.get(Calendar.YEAR) == currentYear
                        val todayDay = today.get(Calendar.DAY_OF_MONTH)

                        var dayCounter = 1
                        for (week in 0..5) {
                            if (dayCounter > daysInMonth) break
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (dow in 0..6) {
                                    if (week == 0 && dow < firstDayOfWeek || dayCounter > daysInMonth) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    } else {
                                        val day = dayCounter
                                        val hasWorkout = workoutDays.contains(day)
                                        val isSelected = day == selectedDay && isCurrentMonth
                                        val isToday = day == todayDay && isCurrentMonth

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .then(
                                                    if (isSelected) Modifier.background(accentColor())
                                                    else if (hasWorkout) Modifier.background(AccentRed.copy(alpha = 0.12f)).border(1.5.dp, AccentRed, CircleShape)
                                                    else if (isToday) Modifier.background(accentColor().copy(alpha = 0.1f))
                                                    else Modifier
                                                )
                                                .clickable {
                                                    selectedDay = day
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "$day",
                                                    fontSize = 14.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (hasWorkout) AccentRed else textColor(),
                                                    fontWeight = if (isToday || hasWorkout) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                        dayCounter++
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "${selectedDay} ${monthNames[currentMonth]}",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor(),
                    fontWeight = FontWeight.Bold
                )
            }

            if (selectedDayWorkouts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = secondaryTextColor().copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = strings.noWorkouts,
                                color = secondaryTextColor(),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(selectedDayWorkouts) { workout ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor()),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(AccentRed.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = AccentRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = LanguageManager.translateMuscleGroup(workout.grupaMusculara, strings),
                                        color = textColor(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${String.format("%.0f", workout.totalWeight)} kg total",
                                        color = secondaryTextColor(),
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            val exercises = remember(workout.id) {
                                kotlinx.coroutines.runBlocking {
                                    db.exercitiuDao().getForAntrenament(workout.id)
                                }
                            }
                            if (exercises.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                HorizontalDivider(color = dividerColor())
                                Spacer(Modifier.height(8.dp))
                                exercises.forEach { ex ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = ex.numeExercitiu,
                                            fontSize = 13.sp,
                                            color = textColor(),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${ex.setIndex + 1}x${ex.repetari} @ ${String.format("%.0f", ex.greutateKg)}kg",
                                            fontSize = 12.sp,
                                            color = AccentRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (workoutDays.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = strings.monthlyDetails,
                                color = textColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            val daysInMonth = java.util.Calendar.getInstance().apply {
                                set(currentYear, currentMonth, 1)
                            }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                            Text(
                                text = "${workoutDays.size} / $daysInMonth ${strings.days.toLowerCase(java.util.Locale.getDefault())}",
                                color = secondaryTextColor(),
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            val barFraction = workoutDays.size.toFloat() / daysInMonth
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(dividerColor())
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = barFraction.coerceIn(0f, 1f))
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(RecoveryGreen)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
