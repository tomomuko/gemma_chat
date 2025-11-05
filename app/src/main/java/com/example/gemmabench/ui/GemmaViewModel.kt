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
 * UI¶K¡h¨Ö¨ó¸ón6¡’ÅS
 */
class GemmaViewModel(application: Application) : AndroidViewModel(application) {

    private val inference = GemmaInference(application)
    private val downloader = ModelDownloader(application)

    private val _uiState = MutableStateFlow<UiState>(UiState.Initializing)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    init {
        // ¢×êwÕBkâÇë’‹Ë
        initializeModel()
    }

    /**
     * âÇë×í»¹
     *
     * 1. À¦óíüÉº
     * 2. Åj‰À¦óíüÉŸL
     * 3. âÇë
     */
    private fun initializeModel() {
        viewModelScope.launch {
            try {
                // À¦óíüÉº
                if (!downloader.isModelDownloaded()) {
                    Log.i(Constants.LOG_TAG, "âÇë*úÀ¦óíüÉ‹Ë")
                    _uiState.value = UiState.Downloading(0f)

                    downloader.downloadModel { progress ->
                        _uiState.value = UiState.Downloading(progress)
                    }.onFailure { error ->
                        val errorMsg = "À¦óíüÉ¨éü: ${error.message}"
                        Log.e(Constants.LOG_TAG, errorMsg, error)
                        _uiState.value = UiState.Error(errorMsg)
                        return@launch
                    }
                }

                // âÇë
                _uiState.value = UiState.Loading("âÇë-...\nÞo30-60ÒKKŠ~Y")

                val modelPath = downloader.getModelPath().absolutePath
                inference.initialize(modelPath)
                    .onSuccess { delegate ->
                        Log.i(Constants.LOG_TAG, "Ÿ: delegate=$delegate")
                        _uiState.value = UiState.Ready(
                            promptText = "",
                            outputText = "",
                            metrics = GenerationMetrics(delegate = delegate),
                            isGenerating = false,
                            errorMessage = null
                        )
                    }
                    .onFailure { error ->
                        val errorMsg = "¨éü: ${error.message}"
                        Log.e(Constants.LOG_TAG, errorMsg, error)
                        _uiState.value = UiState.Error(errorMsg)
                    }

            } catch (e: Exception) {
                val errorMsg = "ˆWjD¨éü: ${e.message}"
                Log.e(Constants.LOG_TAG, errorMsg, e)
                _uiState.value = UiState.Error(errorMsg)
            }
        }
    }

    /**
     * Æ­¹È’‹Ë
     *
     * @param prompt e›×íó×È
     */
    fun generate(prompt: String) {
        if (prompt.isBlank()) {
            Log.w(Constants.LOG_TAG, "zn×íó×È")
            return
        }

        // âXn’­ãó»ë
        generationJob?.cancel()

        val currentState = (_uiState.value as? UiState.Ready) ?: run {
            Log.w(Constants.LOG_TAG, "Ready¶KgojD_’¹­Ã×")
            return
        }

        // ‹Ë¶Kkô°
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
                    Log.d(Constants.LOG_TAG, "Õíü‹Ë")
                }
                .catch { error ->
                    val errorMsg = "¨éü: ${error.message}"
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
                            Log.d(Constants.LOG_TAG, "‹Ëå")
                        }

                        is StreamingResult.TokenGenerated -> {
                            tokenCount++

                            // ÞÈü¯óì¤Æó·2
                            if (firstTokenTime == null) {
                                firstTokenTime = System.currentTimeMillis() - startTime
                            }

                            // UIk!ý 
                            val state = _uiState.value as? UiState.Ready ?: return@collect
                            _uiState.value = state.copy(
                                outputText = state.outputText + result.text
                            )
                        }

                        is StreamingResult.Completed -> {
                            Log.i(
                                Constants.LOG_TAG,
                                "Œ†: ${result.metrics.formatForDisplay()}"
                            )

                            val state = _uiState.value as? UiState.Ready ?: return@collect
                            _uiState.value = state.copy(
                                isGenerating = false,
                                metrics = result.metrics
                            )
                        }

                        is StreamingResult.Error -> {
                            Log.e(Constants.LOG_TAG, "¨éü: ${result.message}")

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
     * ’\b
     */
    fun stopGeneration() {
        generationJob?.cancel()
        Log.i(Constants.LOG_TAG, "\b")

        val state = _uiState.value as? UiState.Ready ?: return
        _uiState.value = state.copy(isGenerating = false)
    }

    /**
     * ú›Æ­¹È’¯ê¢
     */
    fun clearOutput() {
        val state = _uiState.value as? UiState.Ready ?: return
        _uiState.value = state.copy(
            outputText = "",
            errorMessage = null
        )
        Log.d(Constants.LOG_TAG, "ú›¯ê¢")
    }

    /**
     * ê½ü¹ã>
     */
    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
        inference.cleanup()
        Log.i(Constants.LOG_TAG, "ViewModel cleared")
    }
}

/**
 * UI¶K
 */
sealed class UiState {
    /** - */
    object Initializing : UiState()

    /** À¦óíüÉ- */
    data class Downloading(val progress: Float) : UiState()

    /** íüÉ- */
    data class Loading(val message: String) : UiState()

    /** –™Œ†ûŸL- */
    data class Ready(
        val promptText: String,
        val outputText: String,
        val metrics: GenerationMetrics,
        val isGenerating: Boolean,
        val errorMessage: String?
    ) : UiState() {
        /**
         * h:(Æ­¹Èáâêêü¯2bn_
P-š	
         */
        val displayText: String
            get() = if (outputText.length > MAX_DISPLAY_LENGTH) {
                "...(e)...\n" + outputText.takeLast(MAX_DISPLAY_LENGTH)
            } else {
                outputText
            }

        companion object {
            private const val MAX_DISPLAY_LENGTH = 10_000  // 10,000‡W~g
        }
    }

    /** ¨éü */
    data class Error(val message: String) : UiState()
}
