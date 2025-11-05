package com.example.gemmabench.inference

import com.example.gemmabench.utils.Constants

/**
 * Æ­¹ÈBn-š
 *
 * @param topK Top-kµó×êó°nK$1-100¨h	
 * @param temperature )¦Ñéáü¿0.0gzš„1.0géóÀà	
 * @param randomSeed qp·üÉş'n_	
 */
data class GenerationConfig(
    val topK: Int = Constants.TOP_K,
    val temperature: Float = Constants.TEMPERATURE,
    val randomSeed: Int = Constants.RANDOM_SEED
)

/**
 * PœnáÈê¯¹
 *
 * @param firstTokenMs ŞÈü¯ó~gnì¤Æó·ßêÒ	
 * @param totalTokens UŒ_ÏÈü¯óp
 * @param tokensPerSec Èü¯ó¦Èü¯ó/Ò	
 * @param delegate (UŒ_Çê²üÈGPU/NNAPI/XNNPACK/CPU	
 */
data class GenerationMetrics(
    val firstTokenMs: Long = 0L,
    val totalTokens: Int = 0,
    val tokensPerSec: Float = 0f,
    val delegate: String = "Unknown"
) {
    /**
     * º“L­„YDbgÕ©üŞÃÈ
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
