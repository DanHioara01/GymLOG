package com.example.gymlog2

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog2.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterTrackingScreen(
    preferencesManager: PreferencesManager,
    strings: LanguageManager.Strings,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    surfaceBg: Color,
    paddingValues: PaddingValues
) {
    var todayWaterMl by remember { mutableIntStateOf(preferencesManager.getTodayWaterMl()) }
    val waterGoal = preferencesManager.getWaterGoalMl()
    val progress = if (waterGoal > 0) (todayWaterMl.toFloat() / waterGoal).coerceIn(0f, 1f) else 0f
    var customMl by remember { mutableStateOf("") }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "water_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val dividerBg = dividerColor()

    var waterHistory by remember { mutableStateOf(preferencesManager.getWaterHistory7Days()) }

    val context = LocalContext.current
    var alarms by remember { mutableStateOf(preferencesManager.getWaterReminders()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editingAlarmId by remember { mutableIntStateOf(-1) }
    var pickerHour by remember { mutableIntStateOf(9) }
    var pickerMinute by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, accent, RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = strings.waterIntake.uppercase(),
                    color = accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 3.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(
                    modifier = Modifier
                        .width(80.dp)
                        .height(180.dp)
                ) {
                    val bottleWidth = size.width * 0.8f
                    val bottleHeight = size.height * 0.85f
                    val neckWidth = bottleWidth * 0.4f
                    val neckHeight = size.height * 0.05f
                    val cornerRadius = bottleWidth * 0.12f

                    val bottleLeft = (size.width - bottleWidth) / 2f
                    val bottleTop = neckHeight + 4.dp.toPx()
                    val capHeight = 8.dp.toPx()

                    drawRoundRect(
                        color = textSecondary.copy(alpha = 0.5f),
                        topLeft = Offset((size.width - neckWidth) / 2f, 0f),
                        size = Size(neckWidth, capHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    val bodyPath = androidx.compose.ui.graphics.Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = bottleLeft,
                                top = bottleTop,
                                right = bottleLeft + bottleWidth,
                                bottom = bottleTop + bottleHeight,
                                topLeftCornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                topRightCornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                bottomLeftCornerRadius = CornerRadius(cornerRadius * 1.5f, cornerRadius * 1.5f),
                                bottomRightCornerRadius = CornerRadius(cornerRadius * 1.5f, cornerRadius * 1.5f)
                            )
                        )
                    }
                    drawPath(bodyPath, color = textSecondary.copy(alpha = 0.4f), style = Stroke(width = 1.5.dp.toPx()))

                    if (animatedProgress > 0f) {
                        val waveAmplitude = 1.5.dp.toPx()
                        val waterTop = bottleTop + bottleHeight * (1f - animatedProgress)
                        val waterPath = androidx.compose.ui.graphics.Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = bottleLeft + 2.dp.toPx(),
                                    top = waterTop - waveAmplitude,
                                    right = bottleLeft + bottleWidth - 2.dp.toPx(),
                                    bottom = bottleTop + bottleHeight - 2.dp.toPx(),
                                    bottomLeftCornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                    bottomRightCornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                    topLeftCornerRadius = CornerRadius(0f, 0f),
                                    topRightCornerRadius = CornerRadius(0f, 0f)
                                )
                            )
                        }

                        val wavePath = androidx.compose.ui.graphics.Path().apply {
                            val waveW = bottleWidth - 4.dp.toPx()
                            val waveL = bottleLeft + 2.dp.toPx()
                            moveTo(waveL, waterTop)
                            for (x in 0..100) {
                                val xR = x / 100f
                                val xPos = waveL + waveW * xR
                                val yOff = kotlin.math.sin(
                                    Math.toRadians((xR * 360 + wavePhase).toDouble())
                                ).toFloat() * waveAmplitude
                                lineTo(xPos, waterTop + yOff)
                            }
                            lineTo(waveL + waveW, waterTop - 20.dp.toPx())
                            lineTo(waveL, waterTop - 20.dp.toPx())
                            close()
                        }

                        val waterColor = lerp(Color(0xFF2196F3), Color(0xFF00BCD4), animatedProgress)
                        clipPath(bodyPath) {
                            drawPath(waterPath, color = waterColor.copy(alpha = 0.7f))
                            drawPath(wavePath, color = surfaceBg)
                        }
                    }

                    for (i in 1..3) {
                        val lineY = bottleTop + bottleHeight * (1f - i * 0.25f)
                        val waterTopLine = if (animatedProgress > 0f) bottleTop + bottleHeight * (1f - animatedProgress) else bottleTop + bottleHeight
                        val isUnderWater = lineY >= waterTopLine
                        drawLine(
                            color = if (isUnderWater) Color.White.copy(alpha = 0.7f) else textSecondary.copy(alpha = 0.4f),
                            start = Offset(bottleLeft + 3.dp.toPx(), lineY),
                            end = Offset(bottleLeft + 10.dp.toPx(), lineY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$todayWaterMl",
                        color = accent,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "/ ${waterGoal} ${strings.ml}",
                        color = textSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val percent = ((progress * 100).toInt()).coerceIn(0, 100)
                    Text(
                        "${percent}%",
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = accent,
                        trackColor = textSecondary.copy(alpha = 0.2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        preferencesManager.addWaterMl(250)
                        todayWaterMl = preferencesManager.getTodayWaterMl()
                        waterHistory = preferencesManager.getWaterHistory7Days()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text("+250 ${strings.ml}", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        preferencesManager.addWaterMl(500)
                        todayWaterMl = preferencesManager.getTodayWaterMl()
                        waterHistory = preferencesManager.getWaterHistory7Days()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text("+500 ${strings.ml}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customMl,
                    onValueChange = { customMl = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("${strings.ml} personalizat", color = textSecondary.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = textSecondary.copy(alpha = 0.3f),
                        cursorColor = accent,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    )
                )
                Button(
                    onClick = {
                        val ml = customMl.toIntOrNull() ?: 0
                        if (ml > 0) {
                            preferencesManager.addWaterMl(ml)
                            todayWaterMl = preferencesManager.getTodayWaterMl()
                            waterHistory = preferencesManager.getWaterHistory7Days()
                            customMl = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(12.dp),
                    enabled = customMl.toIntOrNull()?.let { it > 0 } == true,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    preferencesManager.addWaterMl(100)
                    todayWaterMl = preferencesManager.getTodayWaterMl()
                    waterHistory = preferencesManager.getWaterHistory7Days()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("+100 ${strings.ml}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = {
                    editingAlarmId = -1
                    pickerHour = 9
                    pickerMinute = 0
                    showTimePicker = true
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.reminder, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (waterHistory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.waterHistory,
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.last7Days,
                        color = textSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val maxMl = waterHistory.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                    val todayIndex = waterHistory.size - 1

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        waterHistory.forEachIndexed { idx, (dayName, ml) ->
                            val isToday = idx == todayIndex
                            val barColor = if (isToday) accent else accent.copy(alpha = 0.35f)
                            val barHeightFraction = if (maxMl > 0) (ml.toFloat() / maxMl) else 0f

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (ml > 0) {
                                    Text(
                                        "${ml}",
                                        color = textSecondary,
                                        fontSize = 9.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .fillMaxHeight(fraction = barHeightFraction.coerceAtLeast(0.04f))
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(barColor)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    dayName,
                                    color = if (isToday) accent else textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        if (alarms.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            alarms.forEach { alarm ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    editingAlarmId = alarm.id
                                    pickerHour = alarm.hour
                                    pickerMinute = alarm.minute
                                    showTimePicker = true
                                }
                        ) {
                            Text(
                                "${alarm.hour.toString().padStart(2, '0')}:${alarm.minute.toString().padStart(2, '0')}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Light,
                                color = if (alarm.enabled) textPrimary else textSecondary.copy(alpha = 0.5f)
                            )
                            Text(
                                strings.everyDay,
                                fontSize = 12.sp,
                                color = if (alarm.enabled) accent else textSecondary.copy(alpha = 0.5f)
                            )
                        }

                        Switch(
                            checked = alarm.enabled,
                            onCheckedChange = { enabled ->
                                preferencesManager.toggleWaterReminder(alarm.id, enabled)
                                alarms = preferencesManager.getWaterReminders()
                                val receiver = WaterReminderReceiver()
                                if (enabled) receiver.scheduleAlarm(context, alarm.id)
                                else receiver.cancelAlarm(context, alarm.id)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accent
                            )
                        )
                    }
                }
            }
        }

        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = pickerHour,
                initialMinute = pickerMinute,
                is24Hour = true
            )

            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                containerColor = cardBg,
                title = { Text(strings.selectTime, color = textPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            selectorColor = accent,
                            containerColor = surfaceBg
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editingAlarmId >= 0) {
                                preferencesManager.updateWaterReminder(editingAlarmId, timePickerState.hour, timePickerState.minute)
                            } else {
                                preferencesManager.addWaterReminder(timePickerState.hour, timePickerState.minute)
                            }
                            alarms = preferencesManager.getWaterReminders()
                            WaterReminderReceiver().scheduleAllEnabledAlarms(context)
                            showTimePicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.confirm, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    if (editingAlarmId >= 0) {
                        TextButton(onClick = {
                            preferencesManager.deleteWaterReminder(editingAlarmId)
                            alarms = preferencesManager.getWaterReminders()
                            WaterReminderReceiver().cancelAlarm(context, editingAlarmId)
                            showTimePicker = false
                        }) {
                            Text(strings.delete, color = Color.Red)
                        }
                    }
                    TextButton(onClick = { showTimePicker = false }) {
                        Text(strings.cancel, color = textSecondary)
                    }
                }
            )
        }
    }
}
