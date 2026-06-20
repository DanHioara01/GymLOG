package com.example.gymlog2

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog2.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WelcomeScreen(
    userName: String,
    strings: LanguageManager.Strings,
    onFinished: () -> Unit
) {
    var phase by remember { mutableIntStateOf(0) }

    val motivationalMessages = listOf(
        "Every rep counts.",
        "Stronger than yesterday.",
        "Your body, your rules.",
        "Push your limits.",
        "Consistency beats talent.",
        "Discipline is freedom.",
        "No shortcuts.",
        "Earned, not given."
    )
    val message = remember { motivationalMessages.random() }

    // Pulsing rings
    val infiniteTransition = rememberInfiniteTransition(label = "rings")
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "ring1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, delayMillis = 800, easing = LinearEasing), RepeatMode.Restart),
        label = "ring2"
    )
    val ring3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, delayMillis = 1600, easing = LinearEasing), RepeatMode.Restart),
        label = "ring3"
    )

    // Logo glow pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "glow"
    )

    // Main fade in/out
    val contentAlpha by animateFloatAsState(
        targetValue = when (phase) {
            0 -> 0f; 1 -> 1f; else -> 0f
        },
        animationSpec = tween(
            durationMillis = when (phase) { 0 -> 500; 1 -> 800; else -> 600 },
            easing = EaseInOutCubic
        ),
        label = "alpha"
    )

    val contentScale by animateFloatAsState(
        targetValue = when (phase) {
            0 -> 0.6f; 1 -> 1f; else -> 1.08f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // KINETIC typewriter
    val kineticText = "KINETIC"
    var visibleChars by remember { mutableIntStateOf(0) }

    // Gradient hue shift
    val gradientHue by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "hue"
    )

    LaunchedEffect(Unit) {
        phase = 1
        // Typewriter for KINETIC
        for (i in 1..kineticText.length) {
            visibleChars = i
            delay(120)
        }
        delay(4000)
        phase = 2
        delay(700)
        onFinished()
    }

    // Particles
    data class Particle(val x: Float, val y: Float, val size: Float, val speed: Float, val alpha: Float, val offset: Float)
    val particles = remember {
        List(30) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 3f + 1f,
                speed = Random.nextFloat() * 0.5f + 0.2f,
                alpha = Random.nextFloat() * 0.4f + 0.1f,
                offset = Random.nextFloat() * 100f
            )
        }
    }
    val particleTime by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "particleTime"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(contentAlpha)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.hsl((gradientHue + 340) % 360, 0.3f, 0.05f),
                        Color.hsl((gradientHue + 350) % 360, 0.4f, 0.08f),
                        Color.hsl(gradientHue % 360, 0.35f, 0.06f),
                        Color.hsl((gradientHue + 10) % 360, 0.3f, 0.04f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Floating particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val px = p.x * size.width
                val rawY = (p.y * size.height - particleTime * p.speed * 20) % size.height
                val py = if (rawY < 0) rawY + size.height else rawY
                drawCircle(
                    color = AccentRed.copy(alpha = p.alpha * contentAlpha),
                    radius = p.size,
                    center = Offset(px + sin(particleTime * 0.5f + p.offset) * 15f, py)
                )
            }
        }

        // Pulsing rings — behind motivational quote
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-100).dp)
        ) {
            listOf(ring1, ring2, ring3).forEachIndexed { index, progress ->
                val maxRadius = 100f + index * 30f
                val radius = progress * maxRadius
                val alpha = (1f - progress) * 0.3f * contentAlpha
                drawCircle(
                    color = AccentRed.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .scale(contentScale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // KINETIC typewriter - sus
            Row {
                kineticText.forEachIndexed { index, char ->
                    if (index < visibleChars) {
                        Text(
                            text = char.toString(),
                            fontSize = 64.sp,
                            letterSpacing = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // Logo with glow
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = AccentRed.copy(alpha = glowAlpha),
                    modifier = Modifier.size(100.dp)
                )
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(70.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "${strings.welcome},",
                fontSize = 22.sp,
                letterSpacing = 3.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                userName,
                fontSize = 32.sp,
                letterSpacing = 4.sp,
                color = AccentRed
            )

            Spacer(Modifier.height(20.dp))

            // Decorative line
            Canvas(modifier = Modifier.width(120.dp).height(2.dp)) {
                drawLine(
                    color = AccentRed.copy(alpha = 0.5f),
                    start = Offset(0f, 1f),
                    end = Offset(size.width * (contentAlpha.coerceIn(0f, 1f)), 1f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            Spacer(Modifier.height(100.dp))

            Text(
                message,
                fontSize = 22.sp,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}
