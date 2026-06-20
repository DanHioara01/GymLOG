package com.example.gymlog2

import androidx.compose.foundation.clickable
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
import com.example.gymlog2.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    isDark: Boolean,
    strings: LanguageManager.Strings,
    prefilledProduct: FoodProduct?,
    onSave: (name: String, brand: String, mealType: String, servingSize: Double, calories: Double, protein: Double, carbs: Double, fat: Double, fiber: Double) -> Unit,
    onBack: () -> Unit
) {
    val surfaceBg = if (isDark) bgColor() else LightBackground
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val cardBg = if (isDark) cardColor() else LightCard
    val accent = if (isDark) accentColor() else LightPrimaryRed

    var name by remember { mutableStateOf(prefilledProduct?.name ?: "") }
    var brand by remember { mutableStateOf(prefilledProduct?.brand ?: "") }
    var mealType by remember { mutableStateOf("lunch") }
    var servingSize by remember { mutableStateOf(prefilledProduct?.servingSize?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else String.format("%.1f", it) } ?: "100") }
    var calories by remember { mutableStateOf(prefilledProduct?.calories?.let { String.format("%.1f", it) } ?: "") }
    var protein by remember { mutableStateOf(prefilledProduct?.proteinG?.let { String.format("%.1f", it) } ?: "") }
    var carbs by remember { mutableStateOf(prefilledProduct?.carbsG?.let { String.format("%.1f", it) } ?: "") }
    var fat by remember { mutableStateOf(prefilledProduct?.fatG?.let { String.format("%.1f", it) } ?: "") }
    var fiber by remember { mutableStateOf(prefilledProduct?.fiberG?.let { String.format("%.1f", it) } ?: "") }

    var mealDropdownExpanded by remember { mutableStateOf(false) }
    val mealTypes = listOf("breakfast" to strings.breakfast, "lunch" to strings.lunch, "dinner" to strings.dinner, "snack" to strings.snack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.manualFoodEntry, color = textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onSave(
                            name,
                            brand,
                            mealType,
                            servingSize.toDoubleOrNull() ?: 100.0,
                            calories.toDoubleOrNull() ?: 0.0,
                            protein.toDoubleOrNull() ?: 0.0,
                            carbs.toDoubleOrNull() ?: 0.0,
                            fat.toDoubleOrNull() ?: 0.0,
                            fiber.toDoubleOrNull() ?: 0.0
                        )
                    }) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceBg)
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

            FoodInputCard(isDark, strings.foodName, name, { name = it }, "Ex: Chicken Breast", cardBg, textPrimary, textSecondary, accent)
            FoodInputCard(isDark, strings.brandLabel, brand, { brand = it }, "Ex: Farm Foods", cardBg, textPrimary, textSecondary, accent)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.selectMealType, style = MaterialTheme.typography.titleMedium, color = textPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mealTypes.find { it.first == mealType }?.second ?: "",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mealDropdownExpanded = true },
                        readOnly = true,
                        enabled = false,
                        trailingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = textSecondary.copy(alpha = 0.3f),
                            disabledTextColor = textPrimary,
                            disabledTrailingIconColor = textSecondary
                        )
                    )
                    DropdownMenu(
                        expanded = mealDropdownExpanded,
                        onDismissRequest = { mealDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        mealTypes.forEach { (type, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = textPrimary) },
                                onClick = {
                                    mealType = type
                                    mealDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.todaysMacros, style = MaterialTheme.typography.titleMedium, color = textPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    MacroInputField(strings.caloriesLabel, calories, { calories = it }, textPrimary, textSecondary, accent)
                    MacroInputField(strings.proteinLabel, protein, { protein = it }, textPrimary, textSecondary, accent)
                    MacroInputField(strings.carbsLabel, carbs, { carbs = it }, textPrimary, textSecondary, accent)
                    MacroInputField(strings.fatLabel, fat, { fat = it }, textPrimary, textSecondary, accent)
                    MacroInputField(strings.fiber, fiber, { fiber = it }, textPrimary, textSecondary, accent)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FoodInputCard(
    isDark: Boolean,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    cardBg: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = textPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = textSecondary.copy(alpha = 0.5f)) },
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
        }
    }
}

@Composable
private fun MacroInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            color = textSecondary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(80.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("0", color = textSecondary.copy(alpha = 0.5f)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = textSecondary.copy(alpha = 0.3f),
                cursorColor = accent,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary
            )
        )
    }
}
