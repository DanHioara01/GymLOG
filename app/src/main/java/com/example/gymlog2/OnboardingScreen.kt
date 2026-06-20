package com.example.gymlog2

import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog2.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    strings: LanguageManager.Strings,
    onProfileComplete: (UserOnboardingProfile) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var goal by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }
    var sessionsPerWeek by remember { mutableIntStateOf(3) }
    var limitations by remember { mutableStateOf("") }
    var selectedGroups by remember { mutableStateOf(setOf<String>()) }

    val accent = AccentRed
    val bg = DarkBackground
    val cardBg = DarkCard
    val textPrimary = TextWarmWhite
    val textSecondary = TextGrayRed
    val border = DarkDivider

    val totalSteps = 5

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                "KINETIC",
                fontSize = 28.sp,
                letterSpacing = 8.sp,
                color = textPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                strings.appTagline,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                color = textSecondary
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0 until totalSteps) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i <= step) accent else border)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                },
                label = "step"
            ) { currentStep ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (currentStep) {
                        0 -> GoalStep(
                            selectedGoal = goal,
                            onSelect = { goal = it },
                            strings = strings,
                            accent = accent,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            border = border
                        )
                        1 -> ExperienceStep(
                            selected = experience,
                            onSelect = { experience = it },
                            accent = accent,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            border = border
                        )
                        2 -> EquipmentStep(
                            selected = equipment,
                            onSelect = { equipment = it },
                            accent = accent,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            border = border
                        )
                        3 -> FrequencyStep(
                            sessions = sessionsPerWeek,
                            onSessionsChange = { sessionsPerWeek = it },
                            limitations = limitations,
                            onLimitationsChange = { limitations = it },
                            accent = accent,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            border = border
                        )
                        4 -> MuscleGroupStep(
                            selectedGroups = selectedGroups,
                            onToggle = { group ->
                                selectedGroups = if (group in selectedGroups) selectedGroups - group
                                else selectedGroups + group
                            },
                            accent = accent,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            border = border
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, border)
                    ) {
                        Text(strings.back, letterSpacing = 2.sp)
                    }
                }

                Button(
                    onClick = {
                        if (step < totalSteps - 1) {
                            step++
                        } else {
                            onProfileComplete(
                                UserOnboardingProfile(
                                    goal = goal,
                                    experience = experience,
                                    equipment = equipment,
                                    sessionsPerWeek = sessionsPerWeek,
                                    limitations = limitations,
                                    selectedGroups = selectedGroups.toList()
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .then(if (step > 0) Modifier.weight(1f) else Modifier.fillMaxWidth())
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    enabled = when (step) {
                        0 -> goal.isNotEmpty()
                        1 -> experience.isNotEmpty()
                        2 -> equipment.isNotEmpty()
                        3 -> true
                        4 -> selectedGroups.isNotEmpty()
                        else -> true
                    }
                ) {
                    Text(
                        if (step == totalSteps - 1) strings.finish else strings.next,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                }
            }

            if (step > 0 || step == 0) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {
                    onProfileComplete(
                        UserOnboardingProfile(
                            goal = goal.ifEmpty { "maintenance" },
                            experience = experience.ifEmpty { "beginner" },
                            equipment = equipment.ifEmpty { "full_gym" },
                            sessionsPerWeek = sessionsPerWeek,
                            limitations = limitations,
                            selectedGroups = selectedGroups.toList().ifEmpty { listOf("chest", "back", "legs") }
                        )
                    )
                }) {
                    Text(strings.skip, color = textSecondary, fontSize = 13.sp, letterSpacing = 1.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GoalStep(
    selectedGoal: String,
    onSelect: (String) -> Unit,
    strings: LanguageManager.Strings,
    accent: Color,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    border: Color
) {
    data class Goal(val key: String, val label: String, val icon: ImageVector)
    val goals = listOf(
        Goal("strength", strings.goalStrength, Icons.Default.FitnessCenter),
        Goal("mass", strings.goalMass, Icons.Default.FavoriteBorder),
        Goal("weight_loss", strings.goalWeightLoss, Icons.Default.Speed),
        Goal("maintenance", strings.goalMaintenance, Icons.Default.SelfImprovement)
    )

    Text(
        strings.selectGoal,
        style = MaterialTheme.typography.headlineMedium,
        color = textPrimary,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(6.dp))
    Text(
        "Step 1 of 5",
        fontSize = 12.sp,
        color = textSecondary
    )
    Spacer(Modifier.height(20.dp))

    goals.forEach { goal ->
        val isSelected = selectedGoal == goal.key
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) accent.copy(alpha = 0.15f) else cardBg)
                .border(
                    1.dp,
                    if (isSelected) accent else border,
                    RoundedCornerShape(14.dp)
                )
                .clickable { onSelect(goal.key) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                goal.icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (isSelected) accent else textSecondary
            )
            Spacer(Modifier.width(14.dp))
            Text(
                goal.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) accent else textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun ExperienceStep(
    selected: String,
    onSelect: (String) -> Unit,
    accent: Color,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    border: Color
) {
    data class Level(val key: String, val label: String, val icon: ImageVector, val desc: String)
    val levels = listOf(
        Level("beginner", "Beginner", Icons.Default.ChildCare, "0-1 years of training"),
        Level("intermediate", "Intermediate", Icons.Default.Person, "1-3 years of consistent training"),
        Level("advanced", "Advanced", Icons.Default.EmojiEvents, "3+ years of serious training")
    )

    Text(
        "What's your experience level?",
        style = MaterialTheme.typography.headlineMedium,
        color = textPrimary,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(6.dp))
    Text("Step 2 of 5", fontSize = 12.sp, color = textSecondary)
    Spacer(Modifier.height(20.dp))

    levels.forEach { level ->
        val isSelected = selected == level.key
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) accent.copy(alpha = 0.15f) else cardBg)
                .border(1.dp, if (isSelected) accent else border, RoundedCornerShape(14.dp))
                .clickable { onSelect(level.key) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(level.icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = if (isSelected) accent else textSecondary)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(level.label, style = MaterialTheme.typography.titleMedium, color = if (isSelected) accent else textPrimary)
                Text(level.desc, fontSize = 11.sp, color = textSecondary)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun EquipmentStep(
    selected: String,
    onSelect: (String) -> Unit,
    accent: Color,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    border: Color
) {
    data class Equip(val key: String, val label: String, val icon: ImageVector, val desc: String)
    val options = listOf(
        Equip("home_no_equipment", "Home - No Equipment", Icons.Default.Home, "Bodyweight exercises only"),
        Equip("home_dumbbells", "Home - Dumbbells/Bands", Icons.Default.FitnessCenter, "Basic home equipment"),
        Equip("full_gym", "Full Gym", Icons.Default.SportsGymnastics, "Complete gym access")
    )

    Text(
        "What equipment do you have?",
        style = MaterialTheme.typography.headlineMedium,
        color = textPrimary,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(6.dp))
    Text("Step 3 of 5", fontSize = 12.sp, color = textSecondary)
    Spacer(Modifier.height(20.dp))

    options.forEach { equip ->
        val isSelected = selected == equip.key
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) accent.copy(alpha = 0.15f) else cardBg)
                .border(1.dp, if (isSelected) accent else border, RoundedCornerShape(14.dp))
                .clickable { onSelect(equip.key) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(equip.icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = if (isSelected) accent else textSecondary)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(equip.label, style = MaterialTheme.typography.titleMedium, color = if (isSelected) accent else textPrimary)
                Text(equip.desc, fontSize = 11.sp, color = textSecondary)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun FrequencyStep(
    sessions: Int,
    onSessionsChange: (Int) -> Unit,
    limitations: String,
    onLimitationsChange: (String) -> Unit,
    accent: Color,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    border: Color
) {
    Text(
        "Training frequency",
        style = MaterialTheme.typography.headlineMedium,
        color = textPrimary,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(6.dp))
    Text("Step 4 of 5", fontSize = 12.sp, color = textSecondary)
    Spacer(Modifier.height(20.dp))

    Text("Sessions per week", color = textSecondary, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 1..6) {
            val isSelected = sessions == i
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) accent else cardBg)
                    .border(1.dp, if (isSelected) accent else border, RoundedCornerShape(12.dp))
                    .clickable { onSessionsChange(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$i",
                    color = if (isSelected) Color.White else textPrimary,
                    fontSize = 18.sp
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    Text("Any physical limitations or injuries?", color = textSecondary, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = limitations,
        onValueChange = onLimitationsChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("e.g. knee pain, back issues (or leave empty)", color = textSecondary, fontSize = 12.sp) },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = border,
            focusedContainerColor = cardBg,
            unfocusedContainerColor = cardBg,
            focusedTextColor = textPrimary,
            unfocusedTextColor = textPrimary,
            cursorColor = accent
        ),
        minLines = 2
    )
}

@Composable
private fun MuscleGroupStep(
    selectedGroups: Set<String>,
    onToggle: (String) -> Unit,
    accent: Color,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    border: Color
) {
    data class MuscleGroup(val key: String, val label: String, val icon: ImageVector)
    val groups = listOf(
        MuscleGroup("chest", "Chest", Icons.Default.FitnessCenter),
        MuscleGroup("back", "Back", Icons.Default.SportsMartialArts),
        MuscleGroup("legs", "Legs", Icons.Default.DirectionsRun),
        MuscleGroup("shoulders", "Shoulders", Icons.Default.AccessibilityNew),
        MuscleGroup("arms", "Arms", Icons.Default.FrontHand),
        MuscleGroup("core", "Core", Icons.Default.SelfImprovement),
        MuscleGroup("cardio", "Cardio", Icons.Default.Favorite)
    )

    Text(
        "Which muscle groups?",
        style = MaterialTheme.typography.headlineMedium,
        color = textPrimary,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(6.dp))
    Text("Step 5 of 5 — select at least one", fontSize = 12.sp, color = textSecondary)
    Spacer(Modifier.height(20.dp))

    groups.forEach { group ->
        val isSelected = group.key in selectedGroups
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) accent.copy(alpha = 0.15f) else cardBg)
                .border(1.dp, if (isSelected) accent else border, RoundedCornerShape(14.dp))
                .clickable { onToggle(group.key) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(group.icon, contentDescription = null, modifier = Modifier.size(26.dp), tint = if (isSelected) accent else textSecondary)
            Spacer(Modifier.width(14.dp))
            Text(
                group.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) accent else textPrimary,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle(group.key) },
                colors = CheckboxDefaults.colors(checkedColor = accent, uncheckedColor = border)
            )
        }
    }
}
