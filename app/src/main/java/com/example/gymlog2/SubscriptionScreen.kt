package com.example.gymlog2

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Composable
fun SubscriptionScreen(
    userId: String,
    monetizationRepository: MonetizationRepository,
    strings: LanguageManager.Strings,
    onBack: () -> Unit
) {
    var isSubscribed by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        isSubscribed = monetizationRepository.isSubscribed(userId)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(strings.subscription, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        if (isSubscribed) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(8.dp))
            Text(strings.youAreSubscribed, color = MaterialTheme.colorScheme.primary)
        } else {
            Text(strings.choosePlan, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            listOf("monthly" to "$4.99/month", "yearly" to "$39.99/year").forEach { (plan, label) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { selectedPlan = plan },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPlan == plan) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(label)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (selectedPlan.isNotEmpty()) {
                        val periodEnd = System.currentTimeMillis() + if (selectedPlan == "monthly") 30L * 86400000 else 365L * 86400000
                        kotlinx.coroutines.MainScope().launch {
                            monetizationRepository.seedActiveSubscription(userId, selectedPlan, periodEnd)
                            isSubscribed = true
                        }
                    }
                },
                enabled = selectedPlan.isNotEmpty()
            ) { Text(strings.subscribe) }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text(strings.back) }
    }
}
