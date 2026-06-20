package com.example.gymlog2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog2.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricInputScreen(
    isDark: Boolean,
    strings: LanguageManager.Strings,
    latestEntry: BiometricEntity?,
    onSave: (
        Double, Double, Double, Double, Double, Double, Double
    ) -> Unit,
    onBack: () -> Unit
) {
    val surfaceBg = if (isDark) bgColor() else LightBackground
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val cardBg = if (isDark) cardColor() else LightCard
    val accent = if (isDark) accentColor() else LightPrimaryRed

    var weight by remember { mutableStateOf(latestEntry?.weightKg?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else String.format("%.1f", it) } ?: "") }
    var bodyFat by remember { mutableStateOf(latestEntry?.bodyFatPercent?.let { if (it > 0) String.format("%.1f", it) else "" } ?: "") }
    var waist by remember { mutableStateOf(latestEntry?.waistCm?.let { if (it > 0) String.format("%.1f", it) else "" } ?: "") }
    var hips by remember { mutableStateOf(latestEntry?.hipsCm?.let { if (it > 0) String.format("%.1f", it) else "" } ?: "") }
    var thighs by remember { mutableStateOf(latestEntry?.thighsCm?.let { if (it > 0) String.format("%.1f", it) else "" } ?: "") }
    var chest by remember { mutableStateOf(latestEntry?.chestCm?.let { if (it > 0) String.format("%.1f", it) else "" } ?: "") }
    var arms by remember { mutableStateOf(latestEntry?.armsCm?.let { if (it > 0) String.format("%.1f", it) else "" } ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(strings.addMeasurement) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = accent)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val w = weight.toDoubleOrNull() ?: 0.0
                        val bf = bodyFat.toDoubleOrNull() ?: 0.0
                        val wa = waist.toDoubleOrNull() ?: 0.0
                        val hi = hips.toDoubleOrNull() ?: 0.0
                        val th = thighs.toDoubleOrNull() ?: 0.0
                        val ch = chest.toDoubleOrNull() ?: 0.0
                        val ar = arms.toDoubleOrNull() ?: 0.0
                        onSave(w, bf, wa, hi, th, ch, ar)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = accent)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            BiometricInputCard(
                isDark = isDark,
                title = strings.weight,
                value = weight,
                onValueChange = { weight = it },
                unit = if (isDark) "kg" else "kg",
                keyboardType = KeyboardType.Decimal,
                accent = accent,
                cardBg = cardBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                strings = strings
            )

            BiometricInputCard(
                isDark = isDark,
                title = strings.bodyFat,
                value = bodyFat,
                onValueChange = { bodyFat = it },
                unit = strings.percent,
                keyboardType = KeyboardType.Decimal,
                accent = accent,
                cardBg = cardBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                strings = strings
            )

            BiometricInputCard(
                isDark = isDark,
                title = strings.waistCirc,
                value = waist,
                onValueChange = { waist = it },
                unit = strings.cm,
                keyboardType = KeyboardType.Decimal,
                accent = accent,
                cardBg = cardBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                strings = strings
            )

            BiometricInputCard(
                isDark = isDark,
                title = strings.hipsCirc,
                value = hips,
                onValueChange = { hips = it },
                unit = strings.cm,
                keyboardType = KeyboardType.Decimal,
                accent = accent,
                cardBg = cardBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                strings = strings
            )

            BiometricInputCard(
                isDark = isDark,
                title = strings.thighsCirc,
                value = thighs,
                onValueChange = { thighs = it },
                unit = strings.cm,
                keyboardType = KeyboardType.Decimal,
                accent = accent,
                cardBg = cardBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                strings = strings
            )

            BiometricInputCard(
                isDark = isDark,
                title = strings.chestCirc,
                value = chest,
                onValueChange = { chest = it },
                unit = strings.cm,
                keyboardType = KeyboardType.Decimal,
                accent = accent,
                cardBg = cardBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                strings = strings
            )

            BiometricInputCard(
                isDark = isDark,
                title = strings.armsCirc,
                value = arms,
                onValueChange = { arms = it },
                unit = strings.cm,
                keyboardType = KeyboardType.Decimal,
                accent = accent,
                cardBg = cardBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                strings = strings
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BiometricInputCard(
    isDark: Boolean,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    keyboardType: KeyboardType,
    accent: androidx.compose.ui.graphics.Color,
    cardBg: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("0.0", color = textSecondary.copy(alpha = 0.5f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = textSecondary.copy(alpha = 0.3f),
                        cursorColor = accent,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    )
                )
                Text(
                    unit,
                    color = textSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
