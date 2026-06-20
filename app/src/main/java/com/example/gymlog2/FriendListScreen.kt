package com.example.gymlog2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog2.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun fetchUserName(userId: String): String {
    return try {
        val user = NetworkClient.api.getUser(userId)
        (user["name"] as? String)?.takeIf { it.isNotBlank() } ?: userId
    } catch (e: Exception) {
        userId
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendListScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val socialRepository = SocialRepository(db)
    val userProfileManager = remember { UserProfileManager(context) }
    val currentUserId = userProfileManager.getOwnUserId()
    val strings = LanguageManager.getStrings(context)

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var friends by remember { mutableStateOf(listOf<FriendshipEntity>()) }
    var incomingRequests by remember { mutableStateOf(listOf<FriendshipEntity>()) }
    var userNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        val profile = userProfileManager.getOwnProfile()
        if (profile != null && profile.name.isNotBlank()) {
            socialRepository.syncUserProfile(currentUserId, profile.name, profile.photoUri)
        }
        friends = socialRepository.getFriends(currentUserId)
        incomingRequests = socialRepository.getIncomingRequests(currentUserId)
        val allUserIds = (friends.map { it.friendId } + incomingRequests.map { it.userId }).distinct()
        val names = mutableMapOf<String, String>()
        for (id in allUserIds) {
            names[id] = withContext(Dispatchers.IO) { fetchUserName(id) }
        }
        userNames = names
    }

    var searchJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(searchQuery) {
        searchJob?.cancel()
        if (searchQuery.trim().length >= 2) {
            searchJob = launch {
                delay(400)
                searchResults = withContext(Dispatchers.IO) {
                    try {
                        val results = NetworkClient.api.searchUsers(searchQuery.trim())
                        results.map { (it["id"] as? String ?: "") to (it["name"] as? String ?: "") }
                            .filter { it.first != currentUserId }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }
                }
            }
        } else {
            searchResults = emptyList()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = bgColor(),
        topBar = {
            TopAppBar(
                title = { Text(strings.friends) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = accentColor())
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor(), titleContentColor = textColor())
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(strings.searchByNameOrId, color = secondaryTextColor()) },
                trailingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = accentColor())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor(),
                    unfocusedBorderColor = dividerColor(),
                    cursorColor = accentColor(),
                    focusedTextColor = textColor(),
                    unfocusedTextColor = textColor(),
                    focusedLabelColor = accentColor(),
                    unfocusedLabelColor = secondaryTextColor()
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            if (searchResults.isNotEmpty()) {
                Text(strings.searchUsers, color = textColor(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                searchResults.forEach { (uid, name) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor()),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, color = textColor(), modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                scope.launch {
                                    val ownProfile = userProfileManager.getOwnProfile()
                                    socialRepository.sendFriendRequest(currentUserId, uid, ownProfile?.name ?: "User")
                                    searchResults = emptyList()
                                    searchQuery = ""
                                }
                            }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = strings.sendFriendRequest, tint = accentColor())
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (incomingRequests.isNotEmpty()) {
                Text(strings.incomingRequests, color = textColor(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                incomingRequests.forEach { req ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor()),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            val senderName = userNames[req.userId] ?: req.userId
                            Text(senderName, color = textColor(), modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                scope.launch {
                                    socialRepository.acceptFriendRequest(req.id)
                                    incomingRequests = socialRepository.getIncomingRequests(currentUserId)
                                    friends = socialRepository.getFriends(currentUserId)
                                    val newIds = friends.map { it.friendId }.distinct().filter { it !in userNames }
                                    if (newIds.isNotEmpty()) {
                                        val names = userNames.toMutableMap()
                                        for (id in newIds) {
                                            names[id] = withContext(Dispatchers.IO) { fetchUserName(id) }
                                        }
                                        userNames = names
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Check, contentDescription = strings.accept, tint = RecoveryGreen)
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    socialRepository.rejectFriendRequest(req.id)
                                    incomingRequests = socialRepository.getIncomingRequests(currentUserId)
                                }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = strings.reject, tint = accentColor())
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Text(strings.yourFriends, color = textColor(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            val acceptedFriends = friends.filter { it.status == "accepted" }
            if (acceptedFriends.isEmpty()) {
                Text(strings.noFriends, color = secondaryTextColor(), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn {
                    items(acceptedFriends) { f ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = cardColor()),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                val friendName = userNames[f.friendId] ?: f.friendId
                                Text(friendName, color = textColor(), modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    scope.launch {
                                        socialRepository.unfollow(currentUserId, f.friendId)
                                        friends = socialRepository.getFriends(currentUserId)
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = strings.removeFriend, tint = accentColor())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
