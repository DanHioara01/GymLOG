package com.example.gymlog2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog2.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTrainerScreen(
    isDark: Boolean,
    strings: LanguageManager.Strings,
    aiManager: AiTrainerManager,
    userId: String,
    preferencesManager: PreferencesManager,
    onBack: () -> Unit
) {
    val surfaceBg = if (isDark) bgColor() else LightBackground
    val textPrimary = if (isDark) textColor() else LightTextPrimary
    val textSecondary = if (isDark) secondaryTextColor() else LightTextSecondary
    val cardBg = if (isDark) cardColor() else LightCard
    val accent = if (isDark) accentColor() else LightPrimaryRed
    val userBubbleBg = if (isDark) AccentPurple else AccentPurple.copy(alpha = 0.8f)
    val aiBubbleBg = if (isDark) cardBg else LightCard

    var messages by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceBg)
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            title = { Text(strings.aiTrainer) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = accent)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = surfaceBg,
                titleContentColor = textPrimary
            )
        )

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✦", fontSize = 48.sp, color = accent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        strings.aiTrainerWelcome,
                        color = textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        strings.aiTrainerHint,
                        color = textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    val suggestions = listOf(
                        strings.aiSuggestion1,
                        strings.aiSuggestion2,
                        strings.aiSuggestion3,
                        strings.aiSuggestion4
                    )
                    suggestions.forEach { suggestion ->
                        OutlinedButton(
                            onClick = {
                                inputText = suggestion
                                scope.launch {
                                    messages = messages + ("user" to suggestion)
                                    isLoading = true
                                    val response = aiManager.chat(userId, suggestion, messages, preferencesManager)
                                    messages = messages + ("ai" to response)
                                    isLoading = false
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                        ) {
                            Text(suggestion, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { (role, text) ->
                    val isUser = role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                    )
                                )
                                .background(if (isUser) userBubbleBg else aiBubbleBg)
                                .padding(12.dp)
                        ) {
                            Text(
                                text,
                                color = if (isUser) androidx.compose.ui.graphics.Color.White else textPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                                    .background(aiBubbleBg)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "...",
                                    color = textSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(strings.askAiTrainer, color = textSecondary.copy(alpha = 0.5f)) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = textSecondary.copy(alpha = 0.3f),
                    cursorColor = accent,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                maxLines = 3
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isLoading) {
                        val msg = inputText.trim()
                        messages = messages + ("user" to msg)
                        inputText = ""
                        isLoading = true
                        scope.launch {
                            val response = aiManager.chat(userId, msg, messages, preferencesManager)
                            messages = messages + ("ai" to response)
                            isLoading = false
                        }
                    }
                },
                enabled = inputText.isNotBlank() && !isLoading
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    tint = if (inputText.isNotBlank() && !isLoading) accent else textSecondary
                )
            }
        }
    }
}
