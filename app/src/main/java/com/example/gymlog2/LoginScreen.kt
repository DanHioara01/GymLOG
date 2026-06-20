package com.example.gymlog2

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog2.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    strings: LanguageManager.Strings,
    isDark: Boolean,
    onEmailLogin: (String, String) -> Unit,
    onGoogleLogin: () -> Unit,
    onFacebookLogin: () -> Unit,
    onGuestLogin: () -> Unit,
    error: String? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    val accent = AccentRed
    val glassBg = Color.White.copy(alpha = 0.08f)
    val glassBorder = Color.White.copy(alpha = 0.15f)

    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )
    val logoAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.login_bg2),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(2.dp),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(initialScale = 0.3f, animationSpec = tween(800)) + fadeIn(animationSpec = tween(800))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "Kinetic Logo",
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = logoScale
                                scaleY = logoScale
                                alpha = logoAlpha
                            }
                            .shadow(20.dp, CircleShape, ambientColor = accent.copy(alpha = 0.4f))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "KINETIC",
                        fontSize = 32.sp,
                        letterSpacing = 10.sp,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        strings.appTagline,
                        fontSize = 11.sp,
                        letterSpacing = 4.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(600, delayMillis = 200)) + fadeIn(animationSpec = tween(600, delayMillis = 200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(glassBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(strings.email) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = accent) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = glassBorder,
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = accent,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = accent
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(strings.password) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = accent) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = glassBorder,
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = accent,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = accent
                        )
                    )
                }
            }

            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(10.dp))

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(600, delayMillis = 350)) + fadeIn(animationSpec = tween(600, delayMillis = 350))
            ) {
                Button(
                    onClick = {
                        if (!isLoading) {
                            isLoading = true
                            onEmailLogin(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = accent.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    enabled = !isLoading,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(accent, accent.copy(alpha = 0.8f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            strings.login,
                            fontSize = 15.sp,
                            letterSpacing = 3.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 450))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, glassBorder)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(glassBg)
                            .border(1.dp, glassBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            strings.or.uppercase(),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            letterSpacing = 3.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(glassBorder, Color.Transparent)
                                )
                            )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(600, delayMillis = 500)) + fadeIn(animationSpec = tween(600, delayMillis = 500))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SocialLoginButton(
                        text = strings.loginWithGoogle,
                        icon = {
                            Text("G", color = GoogleBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        },
                        bgColor = glassBg,
                        borderColor = glassBorder,
                        enabled = !isLoading,
                        onClick = {
                            if (!isLoading) {
                                isLoading = true
                                onGoogleLogin()
                            }
                        }
                    )
                    SocialLoginButton(
                        text = strings.loginWithFacebook,
                        icon = {
                            Icon(Icons.Default.Facebook, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(22.dp))
                        },
                        bgColor = glassBg,
                        borderColor = glassBorder,
                        enabled = !isLoading,
                        onClick = {
                            if (!isLoading) {
                                isLoading = true
                                onFacebookLogin()
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 600))
            ) {
                Text(
                    strings.loginAsGuest,
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isLoading) {
                            isLoading = true
                            onGuestLogin()
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(52.dp),
                    color = accent,
                    strokeWidth = 4.dp,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
private fun SocialLoginButton(
    text: String,
    icon: @Composable () -> Unit,
    bgColor: Color,
    borderColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) bgColor else bgColor.copy(alpha = 0.3f))
            .border(1.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            color = if (enabled) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.3f),
            fontSize = 13.sp,
            letterSpacing = 2.sp
        )
    }
}
