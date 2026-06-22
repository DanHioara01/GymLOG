package com.example.gymlog2

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog2.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ActivitySummaryCard(
    activity: DailyActivityEntity,
    isDark: Boolean,
    strings: LanguageManager.Strings,
    onCardioMapClick: () -> Unit
) {
    val cardBg = if (isDark) cardColor() else LightCard
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val accent = if (isDark) accentColor() else LightPrimaryRed
    val divider = if (isDark) dividerColor() else LightDividerGray

    val stepsProgress = if (activity.stepsGoal > 0) activity.steps.toFloat() / activity.stepsGoal else 0f
    val activeTimeProgress = if (activity.activeTimeGoal > 0) activity.activeTimeMinutes.toFloat() / activity.activeTimeGoal else 0f
    val caloriesProgress = if (activity.activityCaloriesGoal > 0) (activity.activityCalories / activity.activityCaloriesGoal).toFloat() else 0f

    val animatedSteps by animateFloatAsState(
        targetValue = stepsProgress.coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "steps"
    )
    val animatedTime by animateFloatAsState(
        targetValue = activeTimeProgress.coerceIn(0f, 1f),
        animationSpec = tween(1200, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "time"
    )
    val animatedCalories by animateFloatAsState(
        targetValue = caloriesProgress.coerceIn(0f, 1f),
        animationSpec = tween(1200, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "calories"
    )

    val stepsColor = Color(0xFF4CAF50)
    val activeTimeColor = Color(0xFF2196F3)
    val caloriesColor = Color(0xFFE91E63)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    strings.activitySummary.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = accent,
                    letterSpacing = 2.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .clickable { onCardioMapClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            strings.cardioMap,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                HeartActivityRings(
                    stepsProgress = animatedSteps,
                    activeTimeProgress = animatedTime,
                    caloriesProgress = animatedCalories,
                    stepsColor = stepsColor,
                    activeTimeColor = activeTimeColor,
                    caloriesColor = caloriesColor,
                    isDark = isDark
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActivityMetric(
                    label = strings.stepsLabel,
                    value = NumberFormat.getNumberInstance(Locale.getDefault()).format(activity.steps),
                    goal = "/${NumberFormat.getNumberInstance(Locale.getDefault()).format(activity.stepsGoal)}",
                    icon = Icons.Default.DirectionsRun,
                    color = stepsColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
                ActivityMetric(
                    label = strings.activeTimeLabel,
                    value = "${activity.activeTimeMinutes}",
                    goal = "/${activity.activeTimeGoal} ${strings.mins}",
                    icon = Icons.Default.Timer,
                    color = activeTimeColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
                ActivityMetric(
                    label = strings.activityCaloriesLabel,
                    value = "${activity.activityCalories.toInt()}",
                    goal = "/${activity.activityCaloriesGoal.toInt()} ${strings.cal}",
                    icon = Icons.Default.LocalFireDepartment,
                    color = caloriesColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = divider)
            Spacer(Modifier.height(12.dp))

            DetailStatRow(
                label = strings.totalBurnedCalories,
                value = "${NumberFormat.getNumberInstance(Locale.getDefault()).format(activity.totalCaloriesBurned.toInt())}",
                unit = strings.cal,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
            Spacer(Modifier.height(8.dp))
            DetailStatRow(
                label = strings.distanceWhileActive,
                value = String.format(Locale.getDefault(), "%.2f", activity.distanceMeters / 1000.0),
                unit = strings.km,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }
    }
}

@Composable
private fun ActivityMetric(
    label: String,
    value: String,
    goal: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize = 11.sp,
            color = textSecondary,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }
        Text(
            goal,
            fontSize = 11.sp,
            color = textSecondary
        )
    }
}

@Composable
private fun DetailStatRow(
    label: String,
    value: String,
    unit: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = textSecondary
        )
        Row {
            Text(
                value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(Modifier.width(4.dp))
            Text(
                unit,
                fontSize = 13.sp,
                color = textSecondary
            )
        }
    }
}

@Composable
private fun HeartActivityRings(
    stepsProgress: Float,
    activeTimeProgress: Float,
    caloriesProgress: Float,
    stepsColor: Color,
    activeTimeColor: Color,
    caloriesColor: Color,
    isDark: Boolean
) {
    val trackColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE8E8E8)

    Canvas(modifier = Modifier.size(180.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val heartScale = min(size.width, size.height) / 2f

        fun heartX(t: Float): Float {
            val rad = t * 2 * PI
            return (16 * sin(rad) * sin(rad) * sin(rad)).toFloat()
        }

        fun heartY(t: Float): Float {
            val rad = t * 2 * PI
            return -(13 * cos(rad) - 5 * cos(2 * rad) - 2 * cos(3 * rad) - cos(4 * rad)).toFloat()
        }

        fun drawHeartRing(scale: Float, progress: Float, trackCol: Color, ringColor: Color, strokeWidth: Float) {
            val steps = 200
            val trackPath = Path()
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                val hx = centerX + heartX(t) * scale
                val hy = centerY + heartY(t) * scale
                if (i == 0) trackPath.moveTo(hx, hy) else trackPath.lineTo(hx, hy)
            }
            trackPath.close()

            drawPath(
                path = trackPath,
                color = trackCol,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            if (progress > 0f) {
                val progressSteps = (steps * progress.coerceIn(0f, 1f)).toInt()
                val progressPath = Path()
                for (i in 0..progressSteps) {
                    val t = i.toFloat() / steps
                    val hx = centerX + heartX(t) * scale
                    val hy = centerY + heartY(t) * scale
                    if (i == 0) progressPath.moveTo(hx, hy) else progressPath.lineTo(hx, hy)
                }
                drawPath(
                    path = progressPath,
                    color = ringColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        val outerScale = heartScale * 0.048f
        val middleScale = heartScale * 0.038f
        val innerScale = heartScale * 0.028f
        val ringWidth = heartScale * 0.065f

        drawHeartRing(outerScale, stepsProgress, trackColor, stepsColor, ringWidth)
        drawHeartRing(middleScale, activeTimeProgress, trackColor, activeTimeColor, ringWidth)
        drawHeartRing(innerScale, caloriesProgress, trackColor, caloriesColor, ringWidth)
    }
}
