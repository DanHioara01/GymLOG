package com.example.gymlog2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog2.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    isDark: Boolean,
    isLbs: Boolean,
    strings: LanguageManager.Strings,
    onBackClick: () -> Unit
) {
    val accent = if (isDark) LightRed else LightPrimaryRed
    val textPrimary = if (isDark) WhiteText else LightTextPrimary
    val textSecondary = if (isDark) GrayText else LightTextSecondary
    val cardBg = if (isDark) cardColor() else LightCard
    val bg = if (isDark) bgColor() else LightBackground

    val context = LocalContext.current
    val socialRepository = remember { SocialRepository(AppDatabase.getDatabase(context)) }
    val userProfileManager = remember { UserProfileManager(context) }
    val currentUserId = userProfileManager.getOwnUserId()
    val scope = rememberCoroutineScope()

    var allEntries by remember { mutableStateOf(listOf<LeaderboardEntry>()) }
    var friendEntries by remember { mutableStateOf(listOf<LeaderboardEntry>()) }
    var loading by remember { mutableStateOf(true) }
    var showFriendsOnly by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        allEntries = socialRepository.getLeaderboardFirestore(50)
        friendEntries = socialRepository.getFriendLeaderboard(currentUserId, 50)
        loading = false
    }

    val displayEntries = if (showFriendsOnly) friendEntries else allEntries

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "KINETIC",
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 3.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg, titleContentColor = textPrimary),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(strings.leaderboard.uppercase(), color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showFriendsOnly,
                    onClick = { showFriendsOnly = false },
                    label = { Text(strings.all, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent,
                        selectedLabelColor = Color.White,
                        containerColor = cardBg,
                        labelColor = textSecondary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = showFriendsOnly,
                    onClick = { showFriendsOnly = true },
                    label = { Text(strings.friends, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent,
                        selectedLabelColor = Color.White,
                        containerColor = cardBg,
                        labelColor = textSecondary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent, modifier = Modifier.size(40.dp))
                }
            } else if (displayEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(strings.noDataYet, color = textSecondary)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(displayEntries) { index, entry ->
                        val isSelf = entry.userId == currentUserId
                        val bg = when {
                            isSelf -> accent.copy(alpha = 0.15f)
                            index == 0 -> GoldPR.copy(alpha = 0.12f)
                            index == 1 -> RecoveryGreen.copy(alpha = 0.08f)
                            index == 2 -> accent.copy(alpha = 0.08f)
                            else -> cardBg
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = bg),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (index < 3) {
                                        val trophyColor = when (index) {
                                            0 -> GoldPR
                                            1 -> RecoveryGreen
                                            2 -> Color(0xFFCD7F32)
                                            else -> textSecondary
                                        }
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = trophyColor, modifier = Modifier.size(28.dp))
                                    } else {
                                        Text("${index + 1}", color = textSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))

                                val profile = userProfileManager.getProfile(entry.userId)
                                if (entry.photoUri.isNotBlank()) {
                                    coil.compose.AsyncImage(
                                        model = entry.photoUri,
                                        contentDescription = null,
                                        modifier = Modifier.size(42.dp).clip(CircleShape).border(2.dp, accent.copy(alpha = 0.3f), CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            entry.name.take(1).uppercase(),
                                            color = accent,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${if (isSelf) "\u2605 " else ""}${entry.name}",
                                        color = if (isSelf) accent else textPrimary,
                                        fontWeight = if (isSelf) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        "${entry.workoutCount} ${strings.workoutsLabel.lowercase()}",
                                        color = textSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                val displayVol = if (isLbs) entry.totalVolume * 2.20462 else entry.totalVolume
                                val unit = if (isLbs) "lbs" else "kg"
                                Text(
                                    "${String.format("%.0f", displayVol)} $unit",
                                    color = if (isSelf) accent else textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
