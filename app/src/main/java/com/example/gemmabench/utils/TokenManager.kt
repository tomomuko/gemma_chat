package com.example.gemmabench.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure Hugging Face token manager
 *
 * Stores tokens using EncryptedSharedPreferences for security
 */
class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "huggingface_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_HF_TOKEN = "hf_token"
        private const val TOKEN_PREFIX = "hf_"
    }

    /**
     * Save Hugging Face token
     *
     * @param token Hugging Face API token
     * @return true if saved successfully
     */
    fun saveToken(token: String): Boolean {
        return try {
            if (!isValidTokenFormat(token)) {
                Log.w(Constants.LOG_TAG, "Invalid token format")
                return false
            }

            sharedPreferences.edit()
                .putString(KEY_HF_TOKEN, token)
                .apply()

            Log.i(Constants.LOG_TAG, "Token saved successfully")
            true
        } catch (e: Exception) {
            Log.e(Constants.LOG_TAG, "Failed to save token", e)
            false
        }
    }

    /**
     * Get saved Hugging Face token
     *
     * @return Token if exists, null otherwise
     */
    fun getToken(): String? {
        return try {
            sharedPreferences.getString(KEY_HF_TOKEN, null)
        } catch (e: Exception) {
            Log.e(Constants.LOG_TAG, "Failed to retrieve token", e)
            null
        }
    }

    /**
     * Check if token exists
     *
     * @return true if token is saved
     */
    fun hasToken(): Boolean {
        return getToken() != null
    }

    /**
     * Delete saved token
     *
     * @return true if deleted successfully
     */
    fun deleteToken(): Boolean {
        return try {
            sharedPreferences.edit()
                .remove(KEY_HF_TOKEN)
                .apply()

            Log.i(Constants.LOG_TAG, "Token deleted successfully")
            true
        } catch (e: Exception) {
            Log.e(Constants.LOG_TAG, "Failed to delete token", e)
            false
        }
    }

    /**
     * Validate token format
     *
     * Hugging Face tokens start with "hf_"
     *
     * @param token Token to validate
     * @return true if format is valid
     */
    private fun isValidTokenFormat(token: String): Boolean {
        return token.startsWith(TOKEN_PREFIX) && token.length > 10
    }

    /**
     * Validate token with Hugging Face API
     *
     * @param token Token to validate
     * @return Result with validation status
     */
    suspend fun validateToken(token: String): Result<Boolean> {
        // For MVP, just check format
        // TODO: Add actual API validation when needed
        return if (isValidTokenFormat(token)) {
            Result.success(true)
        } else {
            Result.failure(Exception(Constants.ERROR_INVALID_TOKEN))
        }
    }
}
