package com.example.gemmabench.inference

import com.example.gemmabench.utils.Constants

/**
 * Text generation configuration
 *
 * @param topK Top-k sampling parameter (1-100 range)
 * @param temperature Sampling temperature (0.0 = deterministic, 1.0 = creative)
 * @param randomSeed Random seed for reproducible results
 */
data class GenerationConfig(
    val topK: Int = Constants.TOP_K,
    val temperature: Float = Constants.TEMPERATURE,
    val randomSeed: Int = Constants.RANDOM_SEED
)

/**
 * Performance metrics
 *
 * @param firstTokenMs Time to first token in milliseconds
 * @param totalTokens Total number of tokens generated
 * @param tokensPerSec Token generation speed (tokens/second)
 * @param delegate Hardware acceleration delegate (GPU/NNAPI/XNNPACK/CPU)
 */
data class GenerationMetrics(
    val firstTokenMs: Long = 0L,
    val totalTokens: Int = 0,
    val tokensPerSec: Float = 0f,
    val delegate: String = "Unknown"
) {
    /**
     * Format metrics for display
     */
    fun formatForDisplay(): String {
        return String.format(
            Constants.METRICS_FORMAT,
            delegate,
            firstTokenMs,
            tokensPerSec
        )
    }
}
