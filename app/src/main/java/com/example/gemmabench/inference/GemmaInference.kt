package com.example.gemmabench.inference

import android.content.Context
import android.util.Log
import com.example.gemmabench.utils.Constants
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * Gemma 3n LiteRT LLM Inference
 *
 * Uses MediaPipe GenAI API for text generation
 * Efficiently manages KV cache
 */
class GemmaInference(private val context: Context) {
    private var llmInference: LlmInference? = null
    private var detectedDelegate: String = "Unknown"

    /**
     * Initialize the model
     *
     * @param modelPath Path to the .task or .litert_lm model file
     * @param maxTokens Maximum number of tokens to generate (default from Constants)
     * @return Success with delegate info, or failure with error
     */
    suspend fun initialize(modelPath: String, maxTokens: Int = Constants.MAX_TOKENS): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.i(Constants.LOG_TAG, "Initializing model: $modelPath (maxTokens=$maxTokens)")

            // Configure LlmInference options
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(maxTokens)
                .build()

            // Create LlmInference instance
            llmInference = LlmInference.createFromOptions(context, options)

            // Detect delegate (MediaPipe auto-selects the best available)
            detectedDelegate = detectDelegate()

            Log.i(Constants.LOG_TAG, "Model initialized: delegate=$detectedDelegate")
            Result.success(detectedDelegate)

        } catch (e: IllegalArgumentException) {
            val errorMsg = "${Constants.ERROR_INITIALIZATION_FAILED}: Invalid model path"
            Log.e(Constants.LOG_TAG, errorMsg, e)
            Result.failure(IllegalArgumentException(errorMsg, e))

        } catch (e: IllegalStateException) {
            val errorMsg = "${Constants.ERROR_INITIALIZATION_FAILED}: Invalid state"
            Log.e(Constants.LOG_TAG, errorMsg, e)
            Result.failure(IllegalStateException(errorMsg, e))

        } catch (e: Exception) {
            val errorMsg = "${Constants.ERROR_INITIALIZATION_FAILED}: ${e.message}"
            Log.e(Constants.LOG_TAG, errorMsg, e)
            Result.failure(e)
        }
    }

    /**
     * Generate text with streaming (real-time token output)
     *
     * @param prompt Input prompt text
     * @param config Generation config (Top-k, Temperature, etc.)
     * @return Flow of streaming results
     */
    fun generateStreaming(
        prompt: String,
        config: GenerationConfig = GenerationConfig()
    ): Flow<StreamingResult> = callbackFlow {
        val inference = llmInference
            ?: run {
                send(StreamingResult.Error("${Constants.ERROR_GENERATION_FAILED}: Model not initialized"))
                close()
                return@callbackFlow
            }

        try {
            Log.d(Constants.LOG_TAG, "Starting generation: prompt length=${prompt.length}")

            // Configure session options
            val sessionOptions = com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
                .LlmInferenceSessionOptions.builder()
                .setTopK(config.topK)
                .setTemperature(config.temperature)
                .setRandomSeed(config.randomSeed)
                .build()

            // Create session (handles KV cache internally)
            val session = com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
                .createFromOptions(inference, sessionOptions)

            val startTime = System.currentTimeMillis()
            var firstTokenTime: Long? = null
            var tokenCount = 0
            val fullText = StringBuilder()

            // Add input prompt
            session.addQueryChunk(prompt)

            // Start generation
            send(StreamingResult.Started)

            // Generate response asynchronously
            session.generateResponseAsync { partialResult, done ->
                tokenCount++

                // Record first token time
                if (firstTokenTime == null && partialResult.isNotEmpty()) {
                    firstTokenTime = System.currentTimeMillis() - startTime
                    Log.d(Constants.LOG_TAG, "First token time: ${firstTokenTime}ms")
                }

                // Append generated text
                fullText.append(partialResult)
                trySend(StreamingResult.TokenGenerated(partialResult))

                if (done) {
                    // Calculate performance metrics
                    val totalTime = System.currentTimeMillis() - startTime
                    val tokensPerSec = if (totalTime > 0) {
                        (tokenCount * 1000f) / totalTime
                    } else {
                        0f
                    }

                    val metrics = GenerationMetrics(
                        firstTokenMs = firstTokenTime ?: 0L,
                        totalTokens = tokenCount,
                        tokensPerSec = tokensPerSec,
                        delegate = detectedDelegate
                    )

                    Log.i(
                        Constants.LOG_TAG,
                        "Generation complete: tokens=$tokenCount, time=${totalTime}ms, speed=${tokensPerSec}tok/s"
                    )

                    // Send completion
                    trySend(StreamingResult.Completed(metrics, fullText.toString()))

                    // Clean up session
                    session.close()
                    close()
                }
            }

        } catch (e: Exception) {
            val errorMsg = "${Constants.ERROR_GENERATION_FAILED}: ${e.message}"
            Log.e(Constants.LOG_TAG, errorMsg, e)
            send(StreamingResult.Error(errorMsg))
            close(e)
        }

        // Flow cleanup
        awaitClose {
            Log.d(Constants.LOG_TAG, "Generation flow closed")
        }
    }

    /**
     * Generate text synchronously (non-streaming)
     *
     * @param prompt Input prompt text
     * @param config Generation config
     * @return Result with generated text or error
     */
    suspend fun generate(
        prompt: String,
        config: GenerationConfig = GenerationConfig()
    ): Result<String> = withContext(Dispatchers.IO) {
        val inference = llmInference
            ?: return@withContext Result.failure(IllegalStateException("Model not initialized"))

        try {
            val sessionOptions = com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
                .LlmInferenceSessionOptions.builder()
                .setTopK(config.topK)
                .setTemperature(config.temperature)
                .setRandomSeed(config.randomSeed)
                .build()

            val session = com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
                .createFromOptions(inference, sessionOptions)
            session.addQueryChunk(prompt)

            val response = session.generateResponse()
            session.close()

            Log.i(Constants.LOG_TAG, "Generation complete: length=${response.length}")
            Result.success(response)

        } catch (e: Exception) {
            val errorMsg = "${Constants.ERROR_GENERATION_FAILED}: ${e.message}"
            Log.e(Constants.LOG_TAG, errorMsg, e)
            Result.failure(e)
        }
    }

    /**
     * Detect hardware acceleration delegate
     *
     * MediaPipe automatically selects the best delegate
     * This is a simplified detection method
     */
    private fun detectDelegate(): String {
        // MediaPipe GenAI API auto-selects GPU/NNAPI/XNNPACK
        // No explicit API to detect which is used, so return generic info
        return "Auto (GPU/NNAPI/XNNPACK)"
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        llmInference?.close()
        llmInference = null
        Log.i(Constants.LOG_TAG, "Inference cleaned up")
    }
}

/**
 * Streaming result types
 */
sealed class StreamingResult {
    /** Generation started */
    object Started : StreamingResult()

    /** Token generated */
    data class TokenGenerated(val text: String) : StreamingResult()

    /** Generation completed */
    data class Completed(val metrics: GenerationMetrics, val fullText: String) : StreamingResult()

    /** Error occurred */
    data class Error(val message: String) : StreamingResult()
}
