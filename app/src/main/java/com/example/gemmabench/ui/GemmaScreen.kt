package com.example.gemmabench.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gemmabench.utils.Constants

/**
 * Gemma Benchmark main screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GemmaScreen(
    viewModel: GemmaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            settingsManager = viewModel.settingsManager,
            onNavigateBack = { showSettings = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Gemma 3n Chat") },
                    actions = {
                        // Settings button (only show when ready)
                        if (uiState is UiState.Ready) {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, "設定")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is UiState.Initializing -> LoadingScreen("Initializing...")
                is UiState.NeedToken -> TokenInputScreen(viewModel)
                is UiState.Downloading -> DownloadingScreen(state.progress)
                is UiState.Loading -> LoadingScreen(state.message)
                is UiState.Ready -> ChatScreen(state, viewModel)
                is UiState.Error -> ErrorScreen(state.message)
            }
        }
        }
    }
}

/**
 * Token input screen
 */
@Composable
fun TokenInputScreen(viewModel: GemmaViewModel) {
    var tokenText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Hugging Face Token Required",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "This app needs a Hugging Face token to download the model.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = tokenText,
            onValueChange = { tokenText = it },
            label = { Text("Hugging Face Token") },
            placeholder = { Text("hf_...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.saveTokenAndDownload(tokenText) },
            modifier = Modifier.fillMaxWidth(),
            enabled = tokenText.startsWith("hf_")
        ) {
            Text("Save Token & Download Model")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = { /* TODO: Open Hugging Face token URL */ }
        ) {
            Text("Get Token from Hugging Face")
        }
    }
}

/**
 * Loading screen
 */
@Composable
fun LoadingScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Download progress screen
 */
@Composable
fun DownloadingScreen(progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Downloading Model",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(24.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${(progress * 100).toInt()}% (${(Constants.MODEL_SIZE_MB * progress).toInt()} / ${Constants.MODEL_SIZE_MB} MB)",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This is a 4.4GB model and may take a while",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Chat screen
 */
@Composable
fun ChatScreen(state: UiState.Ready, viewModel: GemmaViewModel) {
    var promptText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Prompt input field
        OutlinedTextField(
            value = promptText,
            onValueChange = { promptText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            label = { Text(Constants.PROMPT_HINT) },
            placeholder = { Text("Example: Explain how neural networks work") },
            enabled = !state.isGenerating,
            maxLines = 5
        )

        // Long prompt warning (Bug#3 fix: warn user about token limits)
        if (promptText.length > 500) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Text(
                    text = "⚠️ Long prompt detected (${promptText.length} chars). Generation may be limited by token ceiling.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (promptText.isNotBlank()) {
                        viewModel.generate(promptText)
                        promptText = ""
                    }
                },
                enabled = !state.isGenerating && promptText.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(Constants.BUTTON_GENERATE)
            }

            Button(
                onClick = { viewModel.stopGeneration() },
                enabled = state.isGenerating,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(Constants.BUTTON_STOP)
            }

            OutlinedButton(
                onClick = { viewModel.clearOutput() },
                enabled = !state.isGenerating && state.outputText.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text(Constants.BUTTON_CLEAR)
            }
        }

        // Error message display
        if (state.errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(
                    text = "Error: ${state.errorMessage}",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Output display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val scrollState = rememberScrollState()

            LaunchedEffect(state.displayText) {
                // Auto-scroll to bottom when new text is generated
                if (state.displayText.isNotEmpty()) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }

            if (state.displayText.isEmpty()) {
                // Empty state message
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Generated output will appear here\n\nThis model can answer questions, explain concepts, and help with various tasks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Generated text display
                Text(
                    text = state.displayText,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            // Generation progress indicator
            if (state.isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Performance metrics display
        MetricsCard(state.metrics)
    }
}

/**
 * Performance metrics card with expansion capability
 */
@Composable
fun MetricsCard(metrics: com.example.gemmabench.inference.GenerationMetrics) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Performance Metrics",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = metrics.formatForDisplay(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                // Expand/Collapse button
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (expanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Hide Details" else "Show Details",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Detailed metrics (shown when expanded)
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Detailed Breakdown:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Detailed metrics items
                    DetailMetricRow("Total Tokens", "${metrics.totalTokens}")
                    DetailMetricRow("First Token", "${metrics.firstTokenMs}ms")
                    DetailMetricRow("Token Speed", "${String.format("%.2f", metrics.tokensPerSec)} tok/s")
                    DetailMetricRow("Delegate", metrics.delegate)

                    Text(
                        text = "Note: Complete timing breakdown available in debug logs",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual metric row for detailed view
 */
@Composable
fun DetailMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

/**
 * Error screen
 */
@Composable
fun ErrorScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error: Initialization Failed",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
