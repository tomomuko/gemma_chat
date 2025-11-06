package com.example.gemmabench.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmabench.inference.GemmaInference
import com.example.gemmabench.inference.GenerationMetrics
import com.example.gemmabench.inference.StreamingResult
import com.example.gemmabench.utils.Constants
import com.example.gemmabench.utils.ModelDownloader
import com.example.gemmabench.utils.TokenManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Gemma Chat ViewModel
 *
 * Manages UI state and inference logic
 */
class GemmaViewModel(application: Application) : AndroidViewModel(application) {

    private val inference = GemmaInference(application)
    private val downloader = ModelDownloader(application)
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow<UiState>(UiState.Initializing)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    init {
        // Initialize model at startup
        initializeModel()
    }

    /**
     * Initialize the model
     *
     * 1. Check if model exists in internal storage
     * 2. Download with token if needed
     * 3. Initialize inference
     */
    private fun initializeModel() {
        viewModelScope.launch {
            try {
                // Check if model exists in internal storage
                if (!downloader.isModelDownloaded()) {
                    // Check for saved token
                    val token = tokenManager.getToken()
                    if (token == null) {
                        Log.i(Constants.LOG_TAG, "No token found, need user input")
                        _uiState.value = UiState.NeedToken
                        return@launch
                    }

                    Log.i(Constants.LOG_TAG, "Model not found, starting download")
                    _uiState.value = UiState.Downloading(0f)

                    downloader.downloadModel(token) { progress ->
                        _uiState.value = UiState.Downloading(progress)
                    }.onFailure { error ->
                        val errorMsg = "Download failed: ${error.message}"
                        Log.e(Constants.LOG_TAG, errorMsg, error)
                        _uiState.value = UiState.Error(errorMsg)
                        return@launch
                    }
                }

                // Initialize inference
                _uiState.value = UiState.Loading("Loading model...\nThis may take 30-60 seconds")

                val modelPath = downloader.getModelPath().absolutePath
                inference.initialize(modelPath)
                    .onSuccess { delegate ->
                        Log.i(Constants.LOG_TAG, "Initialization successful: delegate=$delegate")
                        _uiState.value = UiState.Ready(
                            promptText = "",
                            outputText = "",
                            metrics = GenerationMetrics(delegate = delegate),
                            isGenerating = false,
                            errorMessage = null
                        )
                    }
                    .onFailure { error ->
                        val errorMsg = "Initialization failed: ${error.message}"
                        Log.e(Constants.LOG_TAG, errorMsg, error)
                        _uiState.value = UiState.Error(errorMsg)
                    }

            } catch (e: Exception) {
                val errorMsg = "Unexpected error: ${e.message}"
                Log.e(Constants.LOG_TAG, errorMsg, e)
                _uiState.value = UiState.Error(errorMsg)
            }
        }
    }

    /**
     * Start text generation
     *
     * @param prompt Input prompt text
     */
    fun generate(prompt: String) {
        if (prompt.isBlank()) {
            Log.w(Constants.LOG_TAG, "Empty prompt")
            return
        }

        // Cancel previous generation if any
        generationJob?.cancel()

        val currentState = (_uiState.value as? UiState.Ready) ?: run {
            Log.w(Constants.LOG_TAG, "Cannot generate: not in Ready state")
            return
        }

        // Update state to generating
        _uiState.value = currentState.copy(
            isGenerating = true,
            outputText = "",
            errorMessage = null
        )

        val startTime = System.currentTimeMillis()
        var firstTokenTime: Long? = null
        var tokenCount = 0

        generationJob = viewModelScope.launch {
            inference.generateStreaming(prompt)
                .onStart {
                    Log.d(Constants.LOG_TAG, "Generation started")
                }
                .catch { error ->
                    val errorMsg = "Generation error: ${error.message}"
                    Log.e(Constants.LOG_TAG, errorMsg, error)

                    val state = _uiState.value as? UiState.Ready
                    _uiState.value = state?.copy(
                        isGenerating = false,
                        errorMessage = errorMsg
                    ) ?: UiState.Error(errorMsg)
                }
                .collect { result ->
                    when (result) {
                        is StreamingResult.Started -> {
                            Log.d(Constants.LOG_TAG, "Streaming started")
                        }

                        is StreamingResult.TokenGenerated -> {
                            tokenCount++

                            // Record first token time
                            if (firstTokenTime == null) {
                                firstTokenTime = System.currentTimeMillis() - startTime
                            }

                            // Update UI with new token
                            val state = _uiState.value as? UiState.Ready ?: return@collect
                            _uiState.value = state.copy(
                                outputText = state.outputText + result.text
                            )
                        }

                        is StreamingResult.Completed -> {
                            Log.i(
                                Constants.LOG_TAG,
                                "Generation complete: ${result.metrics.formatForDisplay()}"
                            )

                            val state = _uiState.value as? UiState.Ready ?: return@collect
                            _uiState.value = state.copy(
                                isGenerating = false,
                                metrics = result.metrics
                            )
                        }

                        is StreamingResult.Error -> {
                            Log.e(Constants.LOG_TAG, "Streaming error: ${result.message}")

                            val state = _uiState.value as? UiState.Ready ?: return@collect
                            _uiState.value = state.copy(
                                isGenerating = false,
                                errorMessage = result.message
                            )
                        }
                    }
                }
        }
    }

    /**
     * Stop generation
     */
    fun stopGeneration() {
        generationJob?.cancel()
        Log.i(Constants.LOG_TAG, "Generation stopped")

        val state = _uiState.value as? UiState.Ready ?: return
        _uiState.value = state.copy(isGenerating = false)
    }

    /**
     * Clear output text
     */
    fun clearOutput() {
        val state = _uiState.value as? UiState.Ready ?: return
        _uiState.value = state.copy(
            outputText = "",
            errorMessage = null
        )
        Log.d(Constants.LOG_TAG, "Output cleared")
    }

    /**
     * Save Hugging Face token and start download
     *
     * @param token Hugging Face API token
     */
    fun saveTokenAndDownload(token: String) {
        viewModelScope.launch {
            // Validate and save token
            if (!tokenManager.saveToken(token)) {
                _uiState.value = UiState.Error("Invalid token format. Token must start with 'hf_'")
                return@launch
            }

            // Start download
            initializeModel()
        }
    }

    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
        inference.cleanup()
        Log.i(Constants.LOG_TAG, "ViewModel cleared")
    }
}

/**
 * UI state types
 */
sealed class UiState {
    /** Initializing */
    object Initializing : UiState()

    /** Need Hugging Face token */
    object NeedToken : UiState()

    /** Downloading model */
    data class Downloading(val progress: Float) : UiState()

    /** Loading model */
    data class Loading(val message: String) : UiState()

    /** Ready to generate */
    data class Ready(
        val promptText: String,
        val outputText: String,
        val metrics: GenerationMetrics,
        val isGenerating: Boolean,
        val errorMessage: String?
    ) : UiState() {
        /**
         * Display text (truncated if too long to prevent memory issues)
         */
        val displayText: String
            get() = if (outputText.length > MAX_DISPLAY_LENGTH) {
                "...(truncated)...\n" + outputText.takeLast(MAX_DISPLAY_LENGTH)
            } else {
                outputText
            }

        companion object {
            private const val MAX_DISPLAY_LENGTH = 10_000  // 10,000 characters
        }
    }

    /** Error state */
    data class Error(val message: String) : UiState()
}
