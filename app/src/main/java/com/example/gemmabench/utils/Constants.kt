package com.example.gemmabench.utils

/**
 * Application-wide constants
 *
 * Model configs, UI strings, and error messages
 */
object Constants {
    // Logging
    const val LOG_TAG = "GemmaBench"

    // Model configuration
    const val MODEL_NAME = "gemma-3n-E4B-it-int4.litertlm"
    const val MODEL_URL = "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm/resolve/main/$MODEL_NAME"
    const val MODEL_SIZE_MB = 4400L  // 4.4GB
    const val MODEL_CHECKSUM = ""  // TODO: Add SHA-256 checksum for validation

    // Hugging Face API
    const val HUGGING_FACE_TOKEN_URL = "https://huggingface.co/settings/tokens"

    // Generation default parameters
    const val MAX_TOKENS = 1024  // Max output length (Gemma 3n supports up to 32K)
    const val TOP_K = 40  // Top-k sampling parameter
    const val TEMPERATURE = 0.8f  // Sampling temperature (0.0-1.0, higher = more creative)
    const val RANDOM_SEED = 101  // Random seed for reproducible results

    // UI strings
    const val PROMPT_HINT = "Enter your message"
    const val BUTTON_GENERATE = "Generate"
    const val BUTTON_STOP = "Stop"
    const val BUTTON_CLEAR = "Clear"

    // Performance metrics format
    const val METRICS_FORMAT = "Delegate: %s | First token: %dms | Speed: %.2f tok/s"

    // Error messages
    const val ERROR_MODEL_NOT_FOUND = "Model file not found or invalid"
    const val ERROR_INITIALIZATION_FAILED = "Model initialization failed"
    const val ERROR_GENERATION_FAILED = "Text generation failed"
    const val ERROR_DOWNLOAD_FAILED = "Model download failed"
    const val ERROR_INVALID_TOKEN = "Invalid Hugging Face token"

    // Download configuration
    const val DOWNLOAD_BUFFER_SIZE = 8192  // 8KB buffer
    const val DOWNLOAD_TIMEOUT_MS = 300_000L  // 5 minutes timeout
}
