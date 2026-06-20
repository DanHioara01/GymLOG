package com.example.gymlog2

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gymlog2.ui.theme.*

enum class DrawerPage { RECOVERY, CALENDAR, FOOD_JOURNAL, AI_TRAINER, FRIENDS, SERVER_SETTINGS }

data class LanguageOption(
    val code: String,
    val name: String,
    val flag: String
)

fun getLanguageOptions(strings: LanguageManager.Strings) = listOf(
    LanguageOption("en", strings.englishUS, "\uD83C\uDDEC\uD83C\uDDE7"),
    LanguageOption("ro", strings.romana, "\uD83C\uDDF7\uD83C\uDDF4"),
    LanguageOption("ru", strings.russkiy, "\uD83C\uDDF7\uD83C\uDDFA"),
    LanguageOption("uk", strings.ukrainska, "\uD83C\uDDFA\uD83C\uDDE6"),
    LanguageOption("fr", strings.francais, "\uD83C\uDDEB\uD83C\uDDF7"),
    LanguageOption("de", strings.deutsch, "\uD83C\uDDE9\uD83C\uDDEA"),
    LanguageOption("es", strings.espanol, "\uD83C\uDDEA\uD83C\uDDF8"),
    LanguageOption("it", strings.italiano, "\uD83C\uDDEE\uD83C\uDDF9"),
    LanguageOption("tr", strings.turkce, "\uD83C\uDDF9\uD83C\uDDF7"),
    LanguageOption("pt", strings.portugues, "\uD83C\uDDE7\uD83C\uDDF7"),
    LanguageOption("pl", strings.polski, "\uD83C\uDDF5\uD83C\uDDF1")
)

@Composable
fun DrawerMenu(
    profileName: String,
    profilePhotoUri: String,
    userId: String,
    currentPage: DrawerPage?,
    isLbs: Boolean,
    isDark: Boolean,
    currentLanguage: String,
    badgeCount: Int = 0,
    currentStreak: Int = 0,
    onNavigate: (DrawerPage) -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onLogout: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onOpenLanguageDialog: () -> Unit,
    onOpenUnitsDialog: () -> Unit,
    strings: LanguageManager.Strings,
    onClose: () -> Unit,
    onOpenServerSettings: () -> Unit = {}
) {
    val bg = if (isDark) DarkBackground else LightBackground
    val textPrimary = if (isDark) WhiteText else LightTextPrimary
    val textSecondary = if (isDark) GrayText else LightTextSecondary
    val divider = if (isDark) DividerGray else LightDividerGray
    val accent = if (isDark) LightRed else LightPrimaryRed
    val selectedBg = if (isDark) DrawerItemSelectedDark else DrawerItemSelectedLight
    val iconBg = if (isDark) IconBackground else LightIconBackground

    ModalDrawerSheet(
        drawerContainerColor = bg,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp)
        ) {
            // Profile Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (profilePhotoUri.isNotBlank()) {
                        AsyncImage(
                            model = profilePhotoUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, accent, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                profileName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            profileName.ifBlank { strings.guest },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            userId,
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                        if (badgeCount > 0 || currentStreak > 0) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentStreak > 0) {
                                    Text("\uD83D\uDD25", fontSize = 14.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "$currentStreak",
                                        color = accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (badgeCount > 0) {
                                    if (currentStreak > 0) Spacer(Modifier.width(12.dp))
                                    Text("\uD83C\uDFC6", fontSize = 14.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "$badgeCount",
                                        color = accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = divider, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))

            // Navigation Items
            DrawerNavItem(
                icon = Icons.Default.Healing,
                label = strings.recovery,
                selected = currentPage == DrawerPage.RECOVERY,
                accent = accent,
                selectedBg = selectedBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                iconBg = iconBg,
                onClick = { onNavigate(DrawerPage.RECOVERY); onClose() }
            )
            DrawerNavItem(
                icon = Icons.Default.CalendarMonth,
                label = strings.calendarView,
                selected = currentPage == DrawerPage.CALENDAR,
                accent = accent,
                selectedBg = selectedBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                iconBg = iconBg,
                onClick = { onNavigate(DrawerPage.CALENDAR); onClose() }
            )
            DrawerNavItem(
                icon = Icons.Default.Restaurant,
                label = strings.foodJournal,
                selected = currentPage == DrawerPage.FOOD_JOURNAL,
                accent = accent,
                selectedBg = selectedBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                iconBg = iconBg,
                onClick = { onNavigate(DrawerPage.FOOD_JOURNAL); onClose() }
            )
            DrawerNavItem(
                icon = Icons.Default.Psychology,
                label = strings.aiTrainer,
                selected = currentPage == DrawerPage.AI_TRAINER,
                accent = accent,
                selectedBg = selectedBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                iconBg = iconBg,
                onClick = { onNavigate(DrawerPage.AI_TRAINER); onClose() }
            )
            DrawerNavItem(
                icon = Icons.Default.People,
                label = strings.friends,
                selected = currentPage == DrawerPage.FRIENDS,
                accent = accent,
                selectedBg = selectedBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                iconBg = iconBg,
                onClick = { onNavigate(DrawerPage.FRIENDS); onClose() }
            )
            HorizontalDivider(color = divider, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            // Export/Import
            DrawerSectionHeader(label = strings.settingsAndMore, textSecondary = textSecondary)
            DrawerNavItem(
                icon = Icons.Default.FileDownload,
                label = strings.exportCsv,
                selected = false,
                accent = accent,
                selectedBg = selectedBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                iconBg = iconBg,
                onClick = { onExportCsv(); onClose() }
            )
            DrawerNavItem(
                icon = Icons.Default.FileUpload,
                label = strings.importCsv,
                selected = false,
                accent = accent,
                selectedBg = selectedBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                iconBg = iconBg,
                onClick = { onImportCsv(); onClose() }
            )
            DrawerNavItem(
                icon = Icons.Default.Dns,
                label = "Server URL",
                selected = false,
                accent = accent,
                selectedBg = selectedBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                iconBg = iconBg,
                onClick = { onOpenServerSettings() }
            )

            HorizontalDivider(color = divider, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            // Logout
            DrawerNavItem(
                icon = Icons.Default.Logout,
                label = strings.logout,
                selected = false,
                accent = RecoveryRed,
                selectedBg = RecoveryRed.copy(alpha = 0.1f),
                textPrimary = RecoveryRed,
                textSecondary = RecoveryRed.copy(alpha = 0.7f),
                iconBg = RecoveryRed.copy(alpha = 0.1f),
                onClick = { onLogout(); onClose() }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    isDark: Boolean,
    currentLanguage: String,
    strings: LanguageManager.Strings,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cardBg = if (isDark) cardColor() else LightCard
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val accent = if (isDark) accentColor() else LightPrimaryRed

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        titleContentColor = textPrimary,
        title = { Text(strings.selectLanguage, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                getLanguageOptions(strings).forEach { lang ->
                    val isSelected = lang.code == currentLanguage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) accent.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(lang.code) }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(lang.flag, fontSize = 22.sp)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            lang.name,
                            color = if (isSelected) accent else textPrimary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close, color = accent)
            }
        }
    )
}

@Composable
fun ThemeSelectionDialog(
    isDark: Boolean,
    currentThemeMode: ThemeMode,
    strings: LanguageManager.Strings,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val cardBg = if (isDark) cardColor() else LightCard
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val accent = if (isDark) accentColor() else LightPrimaryRed

    val themeOptions = listOf(
        ThemeMode.LIGHT to strings.light,
        ThemeMode.DARK to strings.dark,
        ThemeMode.SYSTEM to strings.system
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        titleContentColor = textPrimary,
        title = { Text(strings.selectTheme, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                themeOptions.forEach { (mode, label) ->
                    val isSelected = mode == currentThemeMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) accent.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(mode) }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (mode) {
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                            },
                            contentDescription = null,
                            tint = if (isSelected) accent else textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            label,
                            color = if (isSelected) accent else textPrimary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close, color = accent)
            }
        }
    )
}

@Composable
fun UnitsSelectionDialog(
    isDark: Boolean,
    isLbs: Boolean,
    strings: LanguageManager.Strings,
    onSelect: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val cardBg = if (isDark) cardColor() else LightCard
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val accent = if (isDark) accentColor() else LightPrimaryRed

    val unitOptions = listOf(
        false to strings.kg,
        true to strings.lbs
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        titleContentColor = textPrimary,
        title = { Text(strings.units, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                unitOptions.forEach { (lbsValue, label) ->
                    val isSelected = lbsValue == isLbs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) accent.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(lbsValue) }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = if (isSelected) accent else textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            label,
                            color = if (isSelected) accent else textPrimary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close, color = accent)
            }
        }
    )
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    selectedBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) selectedBg else Color.Transparent,
        animationSpec = tween(200),
        label = "bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) accent.copy(alpha = 0.15f) else iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) accent else textSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            color = if (selected) accent else textPrimary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}

@Composable
private fun DrawerSectionHeader(label: String, textSecondary: Color) {
    Text(
        label,
        color = textSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun DrawerSettingItem(
    icon: ImageVector,
    label: String,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = textSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun ServerUrlDialog(
    isDark: Boolean,
    currentUrl: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cardBg = if (isDark) cardColor() else LightCard
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val accent = if (isDark) accentColor() else LightPrimaryRed

    var url by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        titleContentColor = textPrimary,
        title = { Text("Server URL", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Enter the backend server address:",
                    color = textSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("http://192.168.100.5:4242", color = textSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = textSecondary.copy(alpha = 0.3f),
                        cursorColor = accent,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Leave empty for default (local network). Tap Save to apply.",
                    color = textSecondary,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(url.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
                TextButton(onClick = onDismiss) {
                Text("Close", color = accent)
            }
        }
    )
}
