package com.example.gemmabench.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Settings Manager
 *
 * Manages user-configurable generation parameters and presets
 */
class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "gemma_settings"

        // Keys
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_TOP_K = "top_k"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_MAX_DISPLAY_LENGTH = "max_display_length"
        private const val KEY_CURRENT_PRESET = "current_preset"

        // Default values
        private const val DEFAULT_MAX_TOKENS = 2048
        private const val DEFAULT_TOP_K = 40
        private const val DEFAULT_TEMPERATURE = 0.8f
        private const val DEFAULT_MAX_DISPLAY_LENGTH = 50_000
    }

    /**
     * Generation settings data class
     */
    data class GenerationSettings(
        val maxTokens: Int = DEFAULT_MAX_TOKENS,
        val topK: Int = DEFAULT_TOP_K,
        val temperature: Float = DEFAULT_TEMPERATURE,
        val maxDisplayLength: Int = DEFAULT_MAX_DISPLAY_LENGTH
    )

    /**
     * Preset configurations
     */
    enum class Preset(
        val displayName: String,
        val emoji: String,
        val description: String,
        val settings: GenerationSettings
    ) {
        RECOMMENDED(
            "推奨設定",
            "🎯",
            "バランスの取れた設定",
            GenerationSettings(
                maxTokens = 2048,
                topK = 40,
                temperature = 0.8f,
                maxDisplayLength = 50_000
            )
        ),
        FAST(
            "短い応答",
            "⚡",
            "速度重視・短文生成",
            GenerationSettings(
                maxTokens = 512,
                topK = 20,
                temperature = 0.7f,
                maxDisplayLength = 20_000
            )
        ),
        LONG(
            "長い応答",
            "📝",
            "長文生成に最適",
            GenerationSettings(
                maxTokens = 4096,
                topK = 40,
                temperature = 0.8f,
                maxDisplayLength = 100_000
            )
        ),
        CREATIVE(
            "創造的",
            "🎨",
            "多様性重視・創造的な応答",
            GenerationSettings(
                maxTokens = 2048,
                topK = 60,
                temperature = 1.0f,
                maxDisplayLength = 50_000
            )
        ),
        PRECISE(
            "正確",
            "🎓",
            "正確性重視・安定した応答",
            GenerationSettings(
                maxTokens = 2048,
                topK = 20,
                temperature = 0.5f,
                maxDisplayLength = 50_000
            )
        ),
        CUSTOM(
            "カスタム",
            "⚙️",
            "ユーザー設定",
            GenerationSettings()
        )
    }

    /**
     * Get current settings
     */
    fun getSettings(): GenerationSettings {
        return GenerationSettings(
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS),
            topK = prefs.getInt(KEY_TOP_K, DEFAULT_TOP_K),
            temperature = prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE),
            maxDisplayLength = prefs.getInt(KEY_MAX_DISPLAY_LENGTH, DEFAULT_MAX_DISPLAY_LENGTH)
        )
    }

    /**
     * Save settings
     */
    fun saveSettings(settings: GenerationSettings) {
        prefs.edit().apply {
            putInt(KEY_MAX_TOKENS, settings.maxTokens)
            putInt(KEY_TOP_K, settings.topK)
            putFloat(KEY_TEMPERATURE, settings.temperature)
            putInt(KEY_MAX_DISPLAY_LENGTH, settings.maxDisplayLength)
            apply()
        }
    }

    /**
     * Apply preset
     */
    fun applyPreset(preset: Preset) {
        saveSettings(preset.settings)
        prefs.edit().putString(KEY_CURRENT_PRESET, preset.name).apply()
    }

    /**
     * Get current preset
     */
    fun getCurrentPreset(): Preset {
        val presetName = prefs.getString(KEY_CURRENT_PRESET, Preset.RECOMMENDED.name)
        return try {
            Preset.valueOf(presetName ?: Preset.RECOMMENDED.name)
        } catch (e: IllegalArgumentException) {
            Preset.RECOMMENDED
        }
    }

    /**
     * Reset to recommended settings
     */
    fun resetToRecommended() {
        applyPreset(Preset.RECOMMENDED)
    }
}
