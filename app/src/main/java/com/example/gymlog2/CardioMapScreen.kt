package com.example.gymlog2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.gymlog2.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class GpsPoint(
    val lat: Double,
    val lon: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardioMapScreen(
    isDark: Boolean,
    strings: LanguageManager.Strings,
    userId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val cardioManager = remember { CardioManager(db) }

    val scope = rememberCoroutineScope()
    val surfaceBg = if (isDark) bgColor() else LightBackground
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val cardBg = if (isDark) cardColor() else LightCard
    val accent = if (isDark) accentColor() else LightPrimaryRed

    var isTracking by remember { mutableStateOf(false) }
    var trackingStartTime by remember { mutableStateOf(0L) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var currentSteps by remember { mutableStateOf(0) }
    var currentDistance by remember { mutableStateOf(0.0) }
    var currentCalories by remember { mutableStateOf(0.0) }
    var currentSpeed by remember { mutableStateOf(0.0) }
    var currentPace by remember { mutableStateOf(0.0) }
    var lastLocationTime by remember { mutableStateOf(0L) }
    var lastLocationPoint by remember { mutableStateOf<GpsPoint?>(null) }
    var routePoints by remember { mutableStateOf<List<GpsPoint>>(emptyList()) }
    var recentSessions by remember { mutableStateOf<List<CardioSessionEntity>>(emptyList()) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var locationManager by remember { mutableStateOf<LocationManager?>(null) }
    var locationListener by remember { mutableStateOf<LocationListener?>(null) }
    var currentLocation by remember { mutableStateOf<GpsPoint?>(null) }
    var showGpsDisabledDialog by remember { mutableStateOf(false) }
    var isGpsEnabled by remember {
        mutableStateOf(
            (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
                .isProviderEnabled(LocationManager.GPS_PROVIDER)
        )
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                hasLocationPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            recentSessions = cardioManager.getRecentSessions(userId)
        }
    }

    LaunchedEffect(hasLocationPermission, isGpsEnabled) {
        if (hasLocationPermission && isGpsEnabled) {
            fetchCurrentLocation(context) { lat, lon ->
                currentLocation = GpsPoint(lat, lon)
            }
        } else if (hasLocationPermission && !isGpsEnabled) {
            showGpsDisabledDialog = true
        }
    }

    LaunchedEffect(isTracking) {
        if (isTracking) {
            while (isTracking) {
                delay(500)
                elapsedMs = System.currentTimeMillis() - trackingStartTime
                currentCalories = currentDistance / 1000.0 * 60.0
                if (currentDistance > 0 && elapsedMs > 0) {
                    val elapsedHours = elapsedMs / 3600000.0
                    currentSpeed = (currentDistance / 1000.0) / elapsedHours
                    val distKm = currentDistance / 1000.0
                    currentPace = if (distKm > 0) (elapsedMs / 60000.0) / distKm else 0.0
                }
            }
        }
    }

    fun startTracking() {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            isGpsEnabled = false
            showGpsDisabledDialog = true
            return
        }

        trackingStartTime = System.currentTimeMillis()
        elapsedMs = 0
        currentSteps = 0
        currentDistance = 0.0
        currentCalories = 0.0
        currentSpeed = 0.0
        currentPace = 0.0
        lastLocationTime = 0L
        lastLocationPoint = null
        routePoints = emptyList()
        isTracking = true

        startGpsTracking(context) { lat, lon, speed ->
            val newPoint = GpsPoint(lat, lon)
            val now = System.currentTimeMillis()
            currentLocation = newPoint
            val currentList = routePoints
            if (currentList.isNotEmpty()) {
                val lastPoint = currentList.last()
                val dist = haversineDistance(lastPoint.lat, lastPoint.lon, lat, lon)
                currentDistance += dist
                currentSteps += (dist / 0.75).toInt()

                if (lastLocationTime > 0 && lastLocationPoint != null) {
                    val timeDiffSec = (now - lastLocationTime) / 1000.0
                    if (timeDiffSec > 0) {
                        val segmentDist = haversineDistance(lastLocationPoint!!.lat, lastLocationPoint!!.lon, lat, lon)
                        val instantSpeed = (segmentDist / timeDiffSec) * 3.6
                        currentSpeed = if (speed > 0) speed * 3.6 else instantSpeed
                        val distKm = segmentDist / 1000.0
                        currentPace = if (distKm > 0) (timeDiffSec / 60.0) / distKm else currentPace
                    }
                } else if (speed > 0) {
                    currentSpeed = speed * 3.6
                }
            }
            lastLocationTime = now
            lastLocationPoint = newPoint
            routePoints = currentList + newPoint
        }.let { (mgr, listener) ->
            locationManager = mgr
            locationListener = listener
        }
    }

    fun stopTracking() {
        isTracking = false
        locationListener?.let { listener ->
            locationManager?.removeUpdates(listener)
        }
        locationManager = null
        locationListener = null
    }

    DisposableEffect(Unit) {
        onDispose {
            locationListener?.let { listener ->
                locationManager?.removeUpdates(listener)
            }
        }
    }

    if (showGpsDisabledDialog) {
        AlertDialog(
            onDismissRequest = { showGpsDisabledDialog = false },
            containerColor = cardBg,
            icon = {
                Icon(
                    Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "GPS " + strings.gpsTracking,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    strings.locationPermissionRequired,
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGpsDisabledDialog = false
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.gpsTracking)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsDisabledDialog = false }) {
                    Text(strings.back__, color = textSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = surfaceBg,
        topBar = {
            TopAppBar(
                title = { Text(strings.cardioMap, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isTracking) {
                            stopTracking()
                        }
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = strings.back__, tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceBg,
                    titleContentColor = textPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        if (!hasLocationPermission) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accent.copy(alpha = 0.1f))
                                    .clickable {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOff,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    strings.locationPermissionRequired,
                                    fontSize = 12.sp,
                                    color = textSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        } else if (!isGpsEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RecoveryOrange.copy(alpha = 0.1f))
                                    .clickable {
                                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.GpsOff,
                                    contentDescription = null,
                                    tint = RecoveryOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "GPS " + strings.gpsTracking,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RecoveryOrange
                                    )
                                    Text(
                                        strings.locationPermissionRequired,
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = RecoveryOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (routePoints.size >= 2) {
                                GpsRouteCanvas(
                                    points = routePoints,
                                    lineColor = accent,
                                    isDark = isDark,
                                    currentLocation = currentLocation
                                )
                            } else if (currentLocation != null) {
                                CurrentLocationCanvas(
                                    location = currentLocation!!,
                                    isTracking = isTracking,
                                    isDark = isDark,
                                    accent = accent
                                )
                            } else if (!hasLocationPermission) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.LocationOff,
                                        contentDescription = null,
                                        tint = textSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        strings.locationPermissionRequired,
                                        fontSize = 13.sp,
                                        color = textSecondary
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = accent,
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        strings.gpsTracking,
                                        fontSize = 13.sp,
                                        color = textSecondary
                                    )
                                }
                            }
                        }

                        if (currentLocation != null) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    String.format(Locale.US, "%.5f, %.5f", currentLocation!!.lat, currentLocation!!.lon),
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TrackingStat(
                                label = strings.duration,
                                value = formatDuration(elapsedMs),
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                            TrackingStat(
                                label = strings.stepsLabel,
                                value = "$currentSteps",
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                            TrackingStat(
                                label = strings.distanceWhileActive,
                                value = String.format(Locale.getDefault(), "%.2f ${strings.km}", currentDistance / 1000.0),
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                            TrackingStat(
                                label = strings.cal,
                                value = "${currentCalories.toInt()}",
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TrackingStat(
                                label = "Speed (km/h)",
                                value = String.format(Locale.getDefault(), "%.1f", currentSpeed),
                                textPrimary = if (currentSpeed > 0) Color(0xFF4CAF50) else textPrimary,
                                textSecondary = textSecondary
                            )
                            TrackingStat(
                                label = strings.pace + " (min/km)",
                                value = formatPace(currentPace),
                                textPrimary = if (currentPace > 0) Color(0xFF2196F3) else textPrimary,
                                textSecondary = textSecondary
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isTracking) {
                                    stopTracking()
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            val routeJson = routePoints.joinToString(";") { "${it.lat},${it.lon}" }
                                            cardioManager.saveCardioSession(
                                                userId = userId,
                                                durationMs = elapsedMs,
                                                steps = currentSteps,
                                                distanceMeters = currentDistance,
                                                caloriesBurned = currentCalories,
                                                routeJson = routeJson,
                                                sessionType = "run"
                                            )
                                            recentSessions = cardioManager.getRecentSessions(userId)
                                        }
                                    }
                                } else {
                                    startTracking()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTracking) RecoveryRed else accent
                            )
                        ) {
                            Icon(
                                if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isTracking) strings.stopCardio else strings.startCardio,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            if (recentSessions.isNotEmpty()) {
                item {
                    Text(
                        strings.cardioHistory.uppercase(),
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(recentSessions) { session ->
                    CardioSessionItem(
                        session = session,
                        isDark = isDark,
                        strings = strings,
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accent = accent
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            strings.noCardioSessions,
                            fontSize = 14.sp,
                            color = textSecondary
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TrackingStat(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontSize = 10.sp,
            color = textSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun CardioSessionItem(
    session: CardioSessionEntity,
    isDark: Boolean,
    strings: LanguageManager.Strings,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    val sdf = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DirectionsRun,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sdf.format(Date(session.timestamp)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${String.format(Locale.getDefault(), "%.2f", session.distanceMeters / 1000.0)} ${strings.km}",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                    Text(
                        formatDuration(session.durationMs),
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                    Text(
                        "${session.caloriesBurned.toInt()} ${strings.cal}",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }

            if (session.routeJson.isNotEmpty()) {
                val miniRoutePoints = remember(session.routeJson) {
                    session.routeJson.split(";").mapNotNull { part ->
                        val coords = part.split(",")
                        if (coords.size == 2) {
                            GpsPoint(coords[0].toDoubleOrNull() ?: return@mapNotNull null, coords[1].toDoubleOrNull() ?: return@mapNotNull null)
                        } else null
                    }
                }
                if (miniRoutePoints.size >= 2) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF1A1A1A) else Color(0xFFF0F0F0))
                    ) {
                        GpsRouteCanvas(
                            points = miniRoutePoints,
                            lineColor = accent,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentLocationCanvas(
    location: GpsPoint,
    isTracking: Boolean,
    isDark: Boolean,
    accent: Color
) {
    val pulseAnim by animateFloatAsState(
        targetValue = if (isTracking) 1f else 0.6f,
        animationSpec = tween(1000),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        drawCircle(
            color = Color(0xFF2196F3).copy(alpha = 0.08f),
            radius = 60f * pulseAnim,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color(0xFF2196F3).copy(alpha = 0.15f),
            radius = 35f * pulseAnim,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = 12f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color(0xFF2196F3),
            radius = 9f,
            center = Offset(centerX, centerY)
        )

        drawCircle(
            color = if (isDark) Color(0xFF333333) else Color(0xFFDDDDDD),
            radius = 4f,
            center = Offset(centerX, centerY + 40f)
        )
    }
}

@Composable
fun GpsRouteCanvas(
    points: List<GpsPoint>,
    lineColor: Color,
    isDark: Boolean,
    currentLocation: GpsPoint? = null
) {
    val pulseAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800),
        label = "routePulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (points.size < 2) return@Canvas

        val allPoints = if (currentLocation != null) points + currentLocation else points

        val minLat = allPoints.minOf { it.lat }
        val maxLat = allPoints.maxOf { it.lat }
        val minLon = allPoints.minOf { it.lon }
        val maxLon = allPoints.maxOf { it.lon }

        val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.0001)

        val padding = 24f
        val drawWidth = size.width - padding * 2
        val drawHeight = size.height - padding * 2

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = padding + ((point.lon - minLon) / lonRange * drawWidth).toFloat()
            val y = padding + ((maxLat - point.lat) / latRange * drawHeight).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        val firstPoint = points.first()
        val startX = padding + ((firstPoint.lon - minLon) / lonRange * drawWidth).toFloat()
        val startY = padding + ((maxLat - firstPoint.lat) / latRange * drawHeight).toFloat()
        drawCircle(Color(0xFF4CAF50), radius = 7f, center = Offset(startX, startY))

        if (currentLocation != null) {
            val curX = padding + ((currentLocation.lon - minLon) / lonRange * drawWidth).toFloat()
            val curY = padding + ((maxLat - currentLocation.lat) / latRange * drawHeight).toFloat()
            drawCircle(
                color = Color(0xFF2196F3).copy(alpha = 0.15f),
                radius = 25f * pulseAnim,
                center = Offset(curX, curY)
            )
            drawCircle(
                color = Color.White,
                radius = 9f,
                center = Offset(curX, curY)
            )
            drawCircle(
                color = Color(0xFF2196F3),
                radius = 7f,
                center = Offset(curX, curY)
            )
        } else {
            val lastPoint = points.last()
            val endX = padding + ((lastPoint.lon - minLon) / lonRange * drawWidth).toFloat()
            val endY = padding + ((maxLat - lastPoint.lat) / latRange * drawHeight).toFloat()
            drawCircle(lineColor, radius = 7f, center = Offset(endX, endY))
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

private fun formatPace(paceMinPerKm: Double): String {
    if (paceMinPerKm <= 0 || paceMinPerKm > 99) return "0:00"
    val totalSeconds = (paceMinPerKm * 60).toInt()
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
}

private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

@SuppressLint("MissingPermission")
private fun startGpsTracking(
    context: Context,
    onLocationUpdate: (Double, Double, Float) -> Unit
): Pair<LocationManager, LocationListener> {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val speed = if (location.hasSpeed()) location.speed else 0f
            onLocationUpdate(location.latitude, location.longitude, speed)
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    val provider = when {
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> LocationManager.GPS_PROVIDER
    }

    lm.requestLocationUpdates(provider, 1000L, 2f, listener)
    return Pair(lm, listener)
}

@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    context: Context,
    onLocation: (Double, Double) -> Unit
) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val provider = when {
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> LocationManager.GPS_PROVIDER
    }

    val lastKnown = lm.getLastKnownLocation(provider)
        ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)

    if (lastKnown != null) {
        onLocation(lastKnown.latitude, lastKnown.longitude)
    }

    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onLocation(location.latitude, location.longitude)
            lm.removeUpdates(this)
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
    lm.requestLocationUpdates(provider, 0L, 0f, listener)
}
