package com.example.gemmabench.inference

import android.content.Context
import android.os.Build
import android.util.Log
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
 * Performance metrics (basic version for display)
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

/**
 * Detailed performance metrics with Prefill/Decode separation
 *
 * @param firstTokenMs Time to first token in milliseconds
 * @param totalTokens Total number of tokens generated
 * @param tokensPerSec Token generation speed (tokens/second)
 * @param delegate Hardware acceleration delegate
 * @param prefillTimeMs Time spent processing prompt (ms)
 * @param decodeTimeMs Time spent generating tokens (ms)
 * @param prefillTokensPerSec Prefill speed (tokens/second)
 * @param decodeTokensPerSec Decode speed (tokens/second)
 * @param minTokenTimeMs Minimum time for one token generation (ms)
 * @param maxTokenTimeMs Maximum time for one token generation (ms)
 * @param avgTokenTimeMs Average time per token generation (ms)
 * @param memoryUsedMB Estimated memory usage (MB)
 * @param deviceInfo Device information
 */
data class DetailedGenerationMetrics(
    val firstTokenMs: Long = 0L,
    val totalTokens: Int = 0,
    val tokensPerSec: Float = 0f,
    val delegate: String = "Unknown",
    val prefillTimeMs: Long = 0L,
    val decodeTimeMs: Long = 0L,
    val prefillTokensPerSec: Float = 0f,
    val decodeTokensPerSec: Float = 0f,
    val minTokenTimeMs: Long = 0L,
    val maxTokenTimeMs: Long = 0L,
    val avgTokenTimeMs: Float = 0f,
    val memoryUsedMB: Long = 0L,
    val deviceInfo: DeviceInfo = DeviceInfo()
) {
    /**
     * Format detailed metrics for display
     */
    fun formatDetailedDisplay(): String {
        return """
            |=== Performance Metrics ===
            |Delegate: $delegate
            |Device: ${deviceInfo.model} (${deviceInfo.soc})
            |
            |Timing:
            |  First token: ${firstTokenMs}ms
            |  Prefill: ${prefillTimeMs}ms (${String.format("%.2f", prefillTokensPerSec)} tok/s)
            |  Decode: ${decodeTimeMs}ms (${String.format("%.2f", decodeTokensPerSec)} tok/s)
            |  Per-token: min=${minTokenTimeMs}ms, avg=${String.format("%.1f", avgTokenTimeMs)}ms, max=${maxTokenTimeMs}ms
            |
            |Tokens:
            |  Total: $totalTokens tokens
            |  Overall speed: ${String.format("%.2f", tokensPerSec)} tok/s
            |
            |Memory:
            |  Model: ~4400 MB
            |  Runtime: ${memoryUsedMB} MB (estimated)
        """.trimMargin()
    }

    /**
     * Estimate delegate from speed characteristics
     */
    fun estimatedDelegate(): String {
        return when {
            tokensPerSec > 100 -> "GPU (estimated)"
            tokensPerSec > 50 -> "NNAPI (estimated)"
            tokensPerSec > 20 -> "XNNPACK (estimated)"
            else -> "CPU (estimated)"
        }
    }
}

/**
 * Device information for performance context
 *
 * @param manufacturer Device manufacturer (e.g., Samsung)
 * @param model Device model (e.g., SM-S928B)
 * @param soc System-on-Chip info (e.g., Snapdragon 8 Gen 3)
 * @param androidVersion Android version (e.g., 15)
 */
data class DeviceInfo(
    val manufacturer: String = "Unknown",
    val model: String = "Unknown",
    val soc: String = "Unknown",
    val androidVersion: String = "Unknown"
) {
    companion object {
        /**
         * Detect device info from Build class
         */
        fun detect(context: Context): DeviceInfo {
            return DeviceInfo(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                soc = detectSoC(),
                androidVersion = Build.VERSION.RELEASE
            )
        }

        /**
         * Detect SoC from device model (best effort detection)
         *
         * This is a simplified detection. For comprehensive SoC detection,
         * would need to parse /proc/cpuinfo or use device-specific databases
         */
        private fun detectSoC(): String {
            // Try to extract SoC from Build properties if available
            // This is limited, but covers common cases
            return try {
                val cpuInfo = readCpuInfo()
                when {
                    cpuInfo.contains("Snapdragon 8 Gen 3") -> "Snapdragon 8 Gen 3"
                    cpuInfo.contains("Snapdragon 8 Gen 2") -> "Snapdragon 8 Gen 2"
                    cpuInfo.contains("A18") -> "A18"
                    cpuInfo.contains("A17") -> "A17"
                    cpuInfo.contains("MediaTek") -> "MediaTek"
                    cpuInfo.contains("Exynos") -> "Exynos"
                    else -> "Unknown"
                }
            } catch (e: Exception) {
                Log.d(Constants.LOG_TAG, "Could not detect SoC: ${e.message}")
                "Unknown"
            }
        }

        /**
         * Read CPU info from /proc/cpuinfo (Linux-style)
         */
        private fun readCpuInfo(): String {
            return try {
                java.io.File("/proc/cpuinfo").readText()
            } catch (e: Exception) {
                ""
            }
        }
    }
}
