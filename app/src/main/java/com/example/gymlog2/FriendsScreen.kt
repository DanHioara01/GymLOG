package com.example.gymlog2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog2.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    isDark: Boolean,
    isLbs: Boolean,
    strings: LanguageManager.Strings,
    onBackClick: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val socialRepository = remember { SocialRepository(db) }
    val userProfileManager = remember { UserProfileManager(context) }
    val currentUserId = userProfileManager.getOwnUserId()
    val scope = rememberCoroutineScope()

    val accent = if (isDark) LightRed else LightPrimaryRed
    val textPrimary = if (isDark) WhiteText else LightTextPrimary
    val textSecondary = if (isDark) GrayText else LightTextSecondary
    val cardBg = if (isDark) cardColor() else LightCard
    val bg = if (isDark) bgColor() else LightBackground
    val surfaceBg = if (isDark) surfaceColor() else LightSurface

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var searchLoading by remember { mutableStateOf(false) }

    var friends by remember { mutableStateOf(listOf<FriendshipEntity>()) }
    var incomingRequests by remember { mutableStateOf(listOf<FriendshipEntity>()) }
    var friendsVolume by remember { mutableStateOf(mapOf<String, Pair<Double, Int>>()) }

    var loading by remember { mutableStateOf(true) }
    var requestSentMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        friends = socialRepository.getFriends(currentUserId)
        incomingRequests = socialRepository.getIncomingRequests(currentUserId)
        friends.forEach { f ->
            val volData = socialRepository.getUserVolume(f.friendId)
            friendsVolume = friendsVolume + (f.friendId to volData)
        }
        loading = false
    }

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
                actions = {
                    IconButton(onClick = onOpenLeaderboard) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Leaderboard", tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg, titleContentColor = textPrimary),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                PageTitle(strings.friends, modifier = Modifier.padding(top = 0.dp, bottom = 0.dp))
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchByNameOrId, color = textSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = textSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = textSecondary.copy(alpha = 0.3f),
                        focusedBorderColor = accent,
                        cursorColor = accent,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (searchQuery.isNotBlank()) {
                item {
                    Button(
                        onClick = {
                            searchLoading = true
                            scope.launch {
                                searchResults = socialRepository.searchUsersOnline(searchQuery)
                                    .filter { it.first != currentUserId }
                                searchLoading = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.search, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (searchLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                if (searchResults.isNotEmpty()) {
                    item {
                        Text(strings.searchUsers, color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    }
                    items(searchResults) { (userId, userName) ->
                        SearchResultCard(
                            userId = userId,
                            userName = userName,
                            isDark = isDark,
                            accent = accent,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            cardBg = cardBg,
                            surfaceBg = surfaceBg,
                            currentUserId = currentUserId,
                            userProfileManager = userProfileManager,
                            socialRepository = socialRepository,
                            isLbs = isLbs,
                            strings = strings,
                            onSent = {
                                requestSentMessage = "${strings.friendRequestSent}: $userName"
                                searchQuery = ""
                                searchResults = emptyList()
                            }
                        )
                    }
                }
            }

            if (incomingRequests.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(strings.incomingRequests.uppercase(), color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                }
                items(incomingRequests) { req ->
                    IncomingRequestCard(
                        request = req,
                        isDark = isDark,
                        accent = accent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        cardBg = cardBg,
                        surfaceBg = surfaceBg,
                        userProfileManager = userProfileManager,
                        socialRepository = socialRepository,
                        onAccepted = {
                            scope.launch {
                                friends = socialRepository.getFriends(currentUserId)
                            }
                        },
                        onRejected = {
                            scope.launch {
                                incomingRequests = socialRepository.getIncomingRequests(currentUserId)
                            }
                        },
                        strings = strings
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(strings.yourFriends.uppercase(), color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            }

            if (friends.isEmpty() && !loading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.People, contentDescription = null, tint = textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(strings.noFriends, color = textSecondary, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            items(friends) { friendship ->
                val friendId = friendship.friendId
                val profile = userProfileManager.getProfile(friendId)
                val vol = friendsVolume[friendId]?.first ?: 0.0
                val wc = friendsVolume[friendId]?.second ?: 0

                val bg = cardBg
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = bg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (profile?.photoUri?.isNotBlank() == true) {
                            coil.compose.AsyncImage(
                                model = profile.photoUri,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(CircleShape).border(2.dp, accent.copy(alpha = 0.3f), CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (profile?.name ?: friendId).take(1).uppercase(),
                                    color = accent,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                profile?.name ?: friendId,
                                color = textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${String.format("%.0f", if (isLbs) vol * 2.20462 else vol)} ${if (isLbs) "lbs" else "kg"} · $wc ${strings.workoutsLabel.lowercase()}",
                                color = textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = textSecondary, modifier = Modifier.size(20.dp))
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(strings.removeFriend, color = Color(0xFFE24B4A)) },
                                    leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = Color(0xFFE24B4A)) },
                                    onClick = {
                                        showMenu = false
                                        scope.launch {
                                            socialRepository.unfollow(currentUserId, friendId)
                                            friends = socialRepository.getFriends(currentUserId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        requestSentMessage?.let { msg ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Snackbar(
                    modifier = Modifier.padding(16.dp).padding(bottom = paddingValues.calculateBottomPadding()),
                    containerColor = accent,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(msg, fontWeight = FontWeight.Medium)
                }
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(2500)
                    requestSentMessage = null
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    userId: String,
    userName: String,
    isDark: Boolean,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    surfaceBg: Color,
    currentUserId: String,
    userProfileManager: UserProfileManager,
    socialRepository: SocialRepository,
    isLbs: Boolean,
    strings: LanguageManager.Strings,
    onSent: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var sending by remember { mutableStateOf(false) }
    var alreadyFriend by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        val friends = socialRepository.getFriends(currentUserId)
        alreadyFriend = friends.any { it.friendId == userId }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(userName.take(1).uppercase(), color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(userName, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(userId, color = textSecondary, fontSize = 11.sp)
            }
            if (!alreadyFriend) {
                Button(
                    onClick = {
                        sending = true
                        scope.launch {
                            val ownProfile = userProfileManager.getOwnProfile()
                            socialRepository.sendFriendRequest(currentUserId, userId, ownProfile?.name ?: currentUserId, ownProfile?.photoUri ?: "")
                            sending = false
                            onSent()
                        }
                    },
                    enabled = !sending,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (sending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(strings.sendRequest, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Text(strings.friendRequestSent, color = textSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: FriendshipEntity,
    isDark: Boolean,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    surfaceBg: Color,
    userProfileManager: UserProfileManager,
    socialRepository: SocialRepository,
    onAccepted: () -> Unit,
    onRejected: () -> Unit,
    strings: LanguageManager.Strings
) {
    val scope = rememberCoroutineScope()
    val profile = userProfileManager.getProfile(request.userId)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (profile?.photoUri?.isNotBlank() == true) {
                coil.compose.AsyncImage(
                    model = profile.photoUri,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp).clip(CircleShape).border(2.dp, accent.copy(alpha = 0.3f), CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (profile?.name ?: request.userId).take(1).uppercase(),
                        color = accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile?.name ?: request.userId, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            socialRepository.acceptFriendRequest(request.id)
                            onAccepted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.accept, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            socialRepository.rejectFriendRequest(request.id)
                            onRejected()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary),
                    border = ButtonDefaults.outlinedButtonBorder,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.reject, color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
