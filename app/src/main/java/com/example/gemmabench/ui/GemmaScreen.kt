package com.example.gemmabench.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gemmabench.utils.Constants

/**
 * Gemma Benchmark·§Û;b
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GemmaScreen(
    viewModel: GemmaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gemma 3n ŸÛ¡ﬁ¸Ø") },
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
                is UiState.Initializing -> LoadingScreen("w’-...")
                is UiState.Downloading -> DownloadingScreen(state.progress)
                is UiState.Loading -> LoadingScreen(state.message)
                is UiState.Ready -> ChatScreen(state, viewModel)
                is UiState.Error -> ErrorScreen(state.message)
            }
        }
    }
}

/**
 * Ì¸«£Û∞;b
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
 * ¿¶ÛÌ¸…2W;b
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
            text = "‚«Î¿¶ÛÌ¸…-",
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
            text = "ﬁn4.4GBn¿¶ÛÌ¸…L≈ÅgY",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * ¡„√»;b
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
        // ◊ÌÛ◊»eõ®Í¢
        OutlinedTextField(
            value = promptText,
            onValueChange = { promptText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            label = { Text(Constants.PROMPT_HINT) },
            placeholder = { Text("ã: Sìkao Ân)kdDfYHf") },
            enabled = !state.isGenerating,
            maxLines = 5
        )

        // üL˚\b˚ØÍ¢‹øÛ
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

        // ®È¸·√ª¸∏h:
        if (state.errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(
                    text = "† ${state.errorMessage}",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ˙õ®Í¢
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val scrollState = rememberScrollState()

            LaunchedEffect(state.displayText) {
                // ∞WD∆≠π»L˝†Uå_âÍ’πØÌ¸Î
                if (state.displayText.isNotEmpty()) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }

            if (state.displayText.isEmpty()) {
                // ◊Ï¸π€Î¿¸
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SSkPúLh:Uå~Y\n\n
Ën∆≠π»’£¸Î…k◊ÌÛ◊»íeõWf‹øÛíºWfO`UD",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // ∆≠π»h:
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

            // -§Û∏±¸ø¸
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

        // ·»ÍØπh:
        MetricsCard(state.metrics)
    }
}

/**
 * ·»ÍØπh:´¸…
 */
@Composable
fun MetricsCard(metrics: com.example.gemmabench.inference.GenerationMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "=  —’©¸ﬁÛπ·»ÍØπ",
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
    }
}

/**
 * ®È¸;b
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
            text = "† ®È¸LzW~W_",
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
