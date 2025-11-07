package com.example.gemmabench.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gemmabench.utils.SettingsManager
import kotlin.math.roundToInt

/**
 * Settings Screen
 *
 * Allows users to configure generation parameters and select presets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onNavigateBack: () -> Unit
) {
    var currentSettings by remember { mutableStateOf(settingsManager.getSettings()) }
    var currentPreset by remember { mutableStateOf(settingsManager.getCurrentPreset()) }
    var showSavedMessage by remember { mutableStateOf(false) }

    // Update settings when they change
    LaunchedEffect(currentSettings) {
        if (currentPreset == SettingsManager.Preset.CUSTOM) {
            settingsManager.saveSettings(currentSettings)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preset selection
            PresetsSection(
                currentPreset = currentPreset,
                onPresetSelected = { preset ->
                    currentPreset = preset
                    currentSettings = preset.settings
                    settingsManager.applyPreset(preset)
                    showSavedMessage = true
                }
            )

            HorizontalDivider()

            // Custom settings
            Text(
                text = "詳細設定",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Max tokens slider
            SettingSlider(
                label = "最大トークン数",
                value = currentSettings.maxTokens.toFloat(),
                valueRange = 256f..8192f,
                steps = 30,
                onValueChange = { value ->
                    currentPreset = SettingsManager.Preset.CUSTOM
                    currentSettings = currentSettings.copy(maxTokens = value.roundToInt())
                },
                valueDisplay = "${currentSettings.maxTokens} tokens"
            )

            // Top-K slider
            SettingSlider(
                label = "Top-K (サンプリング幅)",
                value = currentSettings.topK.toFloat(),
                valueRange = 1f..100f,
                steps = 98,
                onValueChange = { value ->
                    currentPreset = SettingsManager.Preset.CUSTOM
                    currentSettings = currentSettings.copy(topK = value.roundToInt())
                },
                valueDisplay = currentSettings.topK.toString()
            )

            // Temperature slider
            SettingSlider(
                label = "Temperature (創造性)",
                value = currentSettings.temperature,
                valueRange = 0.1f..2.0f,
                steps = 18,
                onValueChange = { value ->
                    currentPreset = SettingsManager.Preset.CUSTOM
                    currentSettings = currentSettings.copy(temperature = value)
                },
                valueDisplay = String.format("%.1f", currentSettings.temperature)
            )

            // Max display length slider
            SettingSlider(
                label = "最大表示文字数",
                value = currentSettings.maxDisplayLength.toFloat(),
                valueRange = 10_000f..200_000f,
                steps = 18,
                onValueChange = { value ->
                    currentPreset = SettingsManager.Preset.CUSTOM
                    currentSettings = currentSettings.copy(maxDisplayLength = value.roundToInt())
                },
                valueDisplay = "${currentSettings.maxDisplayLength / 1000}K chars"
            )

            // Save confirmation message
            if (showSavedMessage) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showSavedMessage = false
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "✓ 設定を保存しました",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Reset button
            OutlinedButton(
                onClick = {
                    currentPreset = SettingsManager.Preset.RECOMMENDED
                    currentSettings = SettingsManager.Preset.RECOMMENDED.settings
                    settingsManager.resetToRecommended()
                    showSavedMessage = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("推奨設定にリセット")
            }
        }
    }
}

/**
 * Presets selection section
 */
@Composable
fun PresetsSection(
    currentPreset: SettingsManager.Preset,
    onPresetSelected: (SettingsManager.Preset) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "プリセット",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "用途に応じた設定を選択できます",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Preset buttons (exclude CUSTOM from user selection)
        SettingsManager.Preset.values()
            .filter { it != SettingsManager.Preset.CUSTOM }
            .forEach { preset ->
                PresetCard(
                    preset = preset,
                    isSelected = currentPreset == preset,
                    onClick = { onPresetSelected(preset) }
                )
            }

        // Show current preset
        if (currentPreset == SettingsManager.Preset.CUSTOM) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${SettingsManager.Preset.CUSTOM.emoji} ${SettingsManager.Preset.CUSTOM.displayName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * Preset card component
 */
@Composable
fun PresetCard(
    preset: SettingsManager.Preset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${preset.emoji} ${preset.displayName}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "選択中",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Setting slider component
 */
@Composable
fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueDisplay: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = valueDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
