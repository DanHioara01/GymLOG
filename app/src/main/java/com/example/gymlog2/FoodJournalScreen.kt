package com.example.gymlog2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymlog2.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class DailyMacros(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodJournalScreen(
    isDark: Boolean,
    strings: LanguageManager.Strings,
    entries: List<FoodEntity>,
    onDelete: (FoodEntity) -> Unit,
    onScanBarcode: () -> Unit,
    onAddManual: () -> Unit,
    onBack: () -> Unit
) {
    val surfaceBg = if (isDark) bgColor() else LightBackground
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val cardBg = if (isDark) cardColor() else LightCard
    val accent = if (isDark) accentColor() else LightPrimaryRed

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val tomorrow = remember { today + 86400000L }

    val todayEntries = remember(entries) {
        entries.filter { it.timestamp in today until tomorrow }
    }

    val todayMacros = remember(todayEntries) {
        DailyMacros(
            calories = todayEntries.sumOf { it.calories },
            protein = todayEntries.sumOf { it.proteinG },
            carbs = todayEntries.sumOf { it.carbsG },
            fat = todayEntries.sumOf { it.fatG },
            fiber = todayEntries.sumOf { it.fiberG }
        )
    }

    val mealGroups = remember(todayEntries) {
        todayEntries.groupBy { it.mealType }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(strings.foodJournal) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = textPrimary)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MacrosOverviewCard(
                    isDark = isDark,
                    strings = strings,
                    macros = todayMacros,
                    cardBg = cardBg,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onScanBarcode,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.scan, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onAddManual,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.add, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (mealGroups.isNotEmpty()) {
                val mealOrder = listOf("breakfast", "lunch", "dinner", "snack")
                mealOrder.forEach { mealType ->
                    val meals = mealGroups[mealType]
                    if (!meals.isNullOrEmpty()) {
                        item {
                            MealSection(
                                isDark = isDark,
                                strings = strings,
                                mealType = mealType,
                                entries = meals,
                                dateFormat = dateFormat,
                                timeFormat = timeFormat,
                                cardBg = cardBg,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                accent = accent,
                                onDelete = onDelete
                            )
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            strings.noFoodEntries,
                            color = textSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MacrosOverviewCard(
    isDark: Boolean,
    strings: LanguageManager.Strings,
    macros: DailyMacros,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                strings.todaysMacros,
                style = MaterialTheme.typography.titleMedium,
                color = textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroItem(strings.caloriesLabel, String.format("%.0f", macros.calories), "kcal", accent, textPrimary, textSecondary)
                MacroItem(strings.proteinLabel, String.format("%.1f", macros.protein), "g", Color(0xFF4CAF50), textPrimary, textSecondary)
                MacroItem(strings.carbsLabel, String.format("%.1f", macros.carbs), "g", Color(0xFFFF9800), textPrimary, textSecondary)
                MacroItem(strings.fatLabel, String.format("%.1f", macros.fat), "g", Color(0xFFE24B4A), textPrimary, textSecondary)
            }
        }
    }
}

@Composable
private fun MacroItem(
    label: String,
    value: String,
    unit: String,
    color: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                value,
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = textSecondary, style = MaterialTheme.typography.bodySmall)
        Text(unit, color = textSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MealSection(
    isDark: Boolean,
    strings: LanguageManager.Strings,
    mealType: String,
    entries: List<FoodEntity>,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    onDelete: (FoodEntity) -> Unit
) {
    val mealName = when (mealType) {
        "breakfast" -> strings.breakfast
        "lunch" -> strings.lunch
        "dinner" -> strings.dinner
        else -> strings.snack
    }

    val mealIcon = when (mealType) {
        "breakfast" -> Icons.Default.WbSunny
        "lunch" -> Icons.Default.WbCloudy
        "dinner" -> Icons.Default.NightsStay
        else -> Icons.Default.Cookie
    }

    var showDeleteDialog by remember { mutableStateOf<FoodEntity?>(null) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(mealIcon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(mealName, color = textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "${entries.sumOf { it.calories }} kcal",
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteDialog = entry }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.name, color = textPrimary, fontWeight = FontWeight.Medium)
                        if (entry.brand.isNotBlank()) {
                            Text(entry.brand, color = textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            "${timeFormat.format(Date(entry.timestamp))} · ${String.format("%.0f", entry.calories)} kcal · P:${String.format("%.1f", entry.proteinG)} · C:${String.format("%.1f", entry.carbsG)} · F:${String.format("%.1f", entry.fatG)}",
                            color = textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = entry }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    showDeleteDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(strings.delete) },
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
