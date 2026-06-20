package com.example.gymlog2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

enum class Feature(val key: String) {
    SOCIAL("social"),
    GAMIFICATION("gamification"),
    LEADERBOARD("leaderboard"),
    ADVANCED_CHARTS("advanced_charts"),
    CSV_IMPORT("csv_import"),
    CUSTOM_TEMPLATES("custom_templates"),
    DARK_MODE("dark_mode"),
    MULTI_LANG("multi_lang"),
    SUBSCRIPTION("subscription")
}

@Composable
fun FeatureGate(
    feature: Feature,
    featureFlagRepository: FeatureFlagRepository,
    modifier: Modifier = Modifier,
    enabledContent: @Composable () -> Unit,
    disabledContent: @Composable (() -> Unit)? = null
) {
    val enabled by kotlinx.coroutines.flow.flow {
        emit(featureFlagRepository.isEnabled(feature.key))
    }.collectAsState(initial = false)

    if (enabled) {
        enabledContent()
    } else {
        disabledContent?.invoke()
    }
}
