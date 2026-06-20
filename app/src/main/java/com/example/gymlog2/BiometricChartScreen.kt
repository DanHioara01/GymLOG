package com.example.gymlog2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymlog2.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricChartScreen(
    isDark: Boolean,
    strings: LanguageManager.Strings,
    entries: List<BiometricEntity>,
    onDelete: (BiometricEntity) -> Unit,
    onBack: () -> Unit
) {
    val surfaceBg = if (isDark) bgColor() else LightBackground
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val cardBg = if (isDark) cardColor() else LightCard
    val accent = if (isDark) accentColor() else LightPrimaryRed

    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf<BiometricEntity?>(null) }

    val sortedEntries = remember(entries) { entries.sortedBy { it.timestamp } }
    val last4 = remember(sortedEntries) { sortedEntries.takeLast(4) }

    val weightData = remember(last4) { last4.map { dateFormat.format(Date(it.timestamp)) to it.weightKg } }
    val bodyFatData = remember(last4) { last4.filter { it.bodyFatPercent > 0 }.map { dateFormat.format(Date(it.timestamp)) to it.bodyFatPercent } }
    val waistData = remember(last4) { last4.filter { it.waistCm > 0 }.map { dateFormat.format(Date(it.timestamp)) to it.waistCm } }
    val hipsData = remember(last4) { last4.filter { it.hipsCm > 0 }.map { dateFormat.format(Date(it.timestamp)) to it.hipsCm } }
    val thighsData = remember(last4) { last4.filter { it.thighsCm > 0 }.map { dateFormat.format(Date(it.timestamp)) to it.thighsCm } }
    val chestData = remember(last4) { last4.filter { it.chestCm > 0 }.map { dateFormat.format(Date(it.timestamp)) to it.chestCm } }
    val armsData = remember(last4) { last4.filter { it.armsCm > 0 }.map { dateFormat.format(Date(it.timestamp)) to it.armsCm } }

    val circumferenceData = remember(last4) {
        last4.filter { it.waistCm > 0 || it.hipsCm > 0 || it.chestCm > 0 || it.armsCm > 0 }
            .map { entry ->
                dateFormat.format(Date(entry.timestamp)) to
                        listOfNotNull(
                            entry.waistCm.takeIf { it > 0 },
                            entry.hipsCm.takeIf { it > 0 },
                            entry.chestCm.takeIf { it > 0 },
                            entry.armsCm.takeIf { it > 0 }
                        ).average()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(strings.biometricTracking) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceBg,
                    titleContentColor = textPrimary
                )
            )
        },
        containerColor = surfaceBg
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(strings.noMeasurements, color = textSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (weightData.size >= 2) {
                    item {
                        ChartCard(
                            isDark = isDark,
                            title = strings.weightChart,
                            data = weightData,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            accent = accent,
                            strings = strings
                        )
                    }
                }

                if (bodyFatData.size >= 2) {
                    item {
                        ChartCard(
                            isDark = isDark,
                            title = strings.bodyFatChart,
                            data = bodyFatData,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            accent = accent,
                            strings = strings
                        )
                    }
                }

                if (circumferenceData.size >= 2) {
                    item {
                        ChartCard(
                            isDark = isDark,
                            title = strings.circumferenceChart,
                            data = circumferenceData,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            accent = accent,
                            strings = strings
                        )
                    }
                }

                item {
                    Text(
                        strings.biometricHistory,
                        style = MaterialTheme.typography.titleMedium,
                        color = textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(sortedEntries.reversed()) { entry ->
                    BiometricHistoryCard(
                        isDark = isDark,
                        entry = entry,
                        dateFormat = dateFormat,
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accent = accent,
                        strings = strings,
                        onDelete = { showDeleteDialog = entry }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    showDeleteDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(strings.deleteMeasurement) },
            text = { Text(strings.confirm + "?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(entry)
                    showDeleteDialog = null
                }) {
                    Text(strings.delete, color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(strings.cancel, color = accent)
                }
            }
        )
    }
}

@Composable
private fun ChartCard(
    isDark: Boolean,
    title: String,
    data: List<Pair<String, Double>>,
    cardBg: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color,
    strings: LanguageManager.Strings
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LineChart(
                data = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                lineColor = accent,
                dotColor = accent
            )
        }
    }
}

@Composable
private fun BiometricHistoryCard(
    isDark: Boolean,
    entry: BiometricEntity,
    dateFormat: SimpleDateFormat,
    cardBg: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color,
    strings: LanguageManager.Strings,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateFormat.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.titleMedium,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = textSecondary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (entry.weightKg > 0) BiometricRow(strings.weight, "${entry.weightKg} kg", textSecondary, textPrimary)
            if (entry.bodyFatPercent > 0) BiometricRow(strings.bodyFat, "${entry.bodyFatPercent}%", textSecondary, textPrimary)
            if (entry.waistCm > 0) BiometricRow(strings.waistCirc, "${entry.waistCm} cm", textSecondary, textPrimary)
            if (entry.hipsCm > 0) BiometricRow(strings.hipsCirc, "${entry.hipsCm} cm", textSecondary, textPrimary)
            if (entry.thighsCm > 0) BiometricRow(strings.thighsCirc, "${entry.thighsCm} cm", textSecondary, textPrimary)
            if (entry.chestCm > 0) BiometricRow(strings.chestCirc, "${entry.chestCm} cm", textSecondary, textPrimary)
            if (entry.armsCm > 0) BiometricRow(strings.armsCirc, "${entry.armsCm} cm", textSecondary, textPrimary)
        }
    }
}

@Composable
private fun BiometricRow(
    label: String,
    value: String,
    labelColor: androidx.compose.ui.graphics.Color,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = labelColor, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}
