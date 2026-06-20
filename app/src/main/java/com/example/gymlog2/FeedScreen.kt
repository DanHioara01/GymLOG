package com.example.gymlog2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gymlog2.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val socialRepository = SocialRepository(db)
    val userProfileManager = remember { UserProfileManager(context) }
    val currentUserId = userProfileManager.getOwnUserId()
    val strings = LanguageManager.getStrings(context)

    var posts by remember { mutableStateOf(listOf<FeedPostEntity>()) }
    var commentTexts by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var newPostText by remember { mutableStateOf("") }
    var commentVisibilities by remember { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }
    var loadedComments by remember { mutableStateOf<Map<Long, List<CommentEntity>>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    fun loadFeed() {
        scope.launch {
            posts = socialRepository.getFeed()
        }
    }

    LaunchedEffect(Unit) { loadFeed() }

    Scaffold(
        containerColor = bgColor(),
        topBar = {
            TopAppBar(
                title = { Text(strings.feed) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = accentColor())
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor(), titleContentColor = textColor())
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor()),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        OutlinedTextField(
                            value = newPostText,
                            onValueChange = { newPostText = it },
                            placeholder = { Text(strings.postPlaceholder, color = secondaryTextColor()) },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                focusedBorderColor = accentColor(),
                                cursorColor = accentColor(),
                                focusedTextColor = textColor(),
                                unfocusedTextColor = textColor()
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = {
                                    if (newPostText.isNotBlank()) {
                                        scope.launch {
                                            socialRepository.createPost(currentUserId, newPostText)
                                            newPostText = ""
                                            loadFeed()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor()),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(strings.post, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (posts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text(strings.feedEmpty, color = secondaryTextColor(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            items(posts) { post ->
                var authorName by remember { mutableStateOf(post.authorId) }
                LaunchedEffect(post.authorId) {
                    val profile = userProfileManager.getProfile(post.authorId)
                    if (profile != null) {
                        authorName = profile.name
                    } else {
                        authorName = withContext(Dispatchers.IO) { fetchUserName(post.authorId) }
                    }
                }
                val isLiked = remember { mutableStateOf(false) }
                val likeCount = remember { mutableIntStateOf(0) }
                LaunchedEffect(post.id) {
                    likeCount.intValue = socialRepository.likesCount(post.id)
                    val existingLikes = db.likeDao().countForPost(post.id)
                    isLiked.value = socialRepository.isLikedByUser(post.id, currentUserId)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor()),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(40.dp), tint = accentColor())
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(authorName, fontWeight = FontWeight.Bold, color = textColor(), style = MaterialTheme.typography.bodyMedium)
                                Text(post.content, color = Color.LightGray, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                scope.launch {
                                    if (!isLiked.value) {
                                        socialRepository.like(post.id, currentUserId)
                                        isLiked.value = true
                                        likeCount.intValue++
                                    } else {
                                        socialRepository.unlike(post.id, currentUserId)
                                        isLiked.value = false
                                        likeCount.intValue = (likeCount.intValue - 1).coerceAtLeast(0)
                                    }
                                }
                            }) {
                                Icon(
                                    if (isLiked.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = strings.like,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isLiked.value) accentColor() else secondaryTextColor()
                                )
                            }
                            Text("${likeCount.intValue}", color = secondaryTextColor(), fontSize = 13.sp)
                            Spacer(Modifier.width(12.dp))
                            IconButton(onClick = {
                                val visible = commentVisibilities[post.id] ?: false
                                commentVisibilities = commentVisibilities + (post.id to !visible)
                                if (!visible) {
                                    scope.launch {
                                        try {
                                            val serverComments = NetworkClient.api.getComments(post.id)
                                            for (c in serverComments) {
                                                db.commentDao().insertComment(c)
                                            }
                                        } catch (e: Exception) { e.printStackTrace() }
                                        loadedComments = loadedComments + (post.id to db.commentDao().getForPost(post.id))
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Comment, contentDescription = strings.comments, modifier = Modifier.size(20.dp), tint = secondaryTextColor())
                            }
                        }
                        val showComments = commentVisibilities[post.id] ?: false
                        if (showComments) {
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 6.dp))
                            val comments = loadedComments[post.id] ?: emptyList()
                            comments.forEach { comment ->
                                var commentAuthorName by remember { mutableStateOf(comment.authorId) }
                                LaunchedEffect(comment.authorId) {
                                    val p = userProfileManager.getProfile(comment.authorId)
                                    commentAuthorName = p?.name ?: withContext(Dispatchers.IO) { fetchUserName(comment.authorId) }
                                }
                                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(24.dp), tint = secondaryTextColor())
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            commentAuthorName,
                                            color = accentColor(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(comment.content, color = Color.LightGray, fontSize = 13.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = commentTexts[post.id] ?: "",
                                    onValueChange = { commentTexts = commentTexts + (post.id to it) },
                                    placeholder = { Text(strings.comments, color = secondaryTextColor()) },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                        focusedBorderColor = accentColor(),
                                        cursorColor = accentColor(),
                                        focusedTextColor = textColor(),
                                        unfocusedTextColor = textColor()
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                IconButton(onClick = {
                                    scope.launch {
                                        val commentText = commentTexts[post.id] ?: ""
                                        if (commentText.isNotBlank()) {
                                            socialRepository.comment(post.id, currentUserId, commentText)
                                            commentTexts = commentTexts + (post.id to "")
                                            loadedComments = loadedComments + (post.id to db.commentDao().getForPost(post.id))
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = accentColor(), modifier = Modifier.size(20.dp))
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = commentTexts[post.id] ?: "",
                                    onValueChange = { commentTexts = commentTexts + (post.id to it) },
                                    placeholder = { Text(strings.comments, color = secondaryTextColor()) },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                        focusedBorderColor = accentColor(),
                                        cursorColor = accentColor(),
                                        focusedTextColor = textColor(),
                                        unfocusedTextColor = textColor()
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                IconButton(onClick = {
                                    scope.launch {
                                        val commentText = commentTexts[post.id] ?: ""
                                        if (commentText.isNotBlank()) {
                                            socialRepository.comment(post.id, currentUserId, commentText)
                                            commentTexts = commentTexts + (post.id to "")
                                            try {
                                                val serverComments = NetworkClient.api.getComments(post.id)
                                                for (c in serverComments) { db.commentDao().insertComment(c) }
                                            } catch (e: Exception) { e.printStackTrace() }
                                            loadedComments = loadedComments + (post.id to db.commentDao().getForPost(post.id))
                                            commentVisibilities = commentVisibilities + (post.id to true)
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = accentColor(), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
