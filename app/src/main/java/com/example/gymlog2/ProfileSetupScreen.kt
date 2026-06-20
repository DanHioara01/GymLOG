package com.example.gymlog2

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ProfileSetupScreen(
    strings: LanguageManager.Strings,
    onSave: (name: String, photoUri: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> photoUri = uri }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(strings.profileSetup, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        if (photoUri != null) {
            AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.size(120.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { launcher.launch("image/*") }) { Text(strings.pickPhoto) }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.enterName) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = { if (name.isNotBlank()) onSave(name, photoUri?.toString() ?: "") }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(strings.saveProfile) }
    }
}
