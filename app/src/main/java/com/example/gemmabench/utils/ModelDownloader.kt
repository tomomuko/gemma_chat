package com.example.gemmabench.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Model downloader with Hugging Face authentication
 *
 * Downloads Gemma 3n model (4.4GB) with resumable downloads
 */
class ModelDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(Constants.DOWNLOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(Constants.DOWNLOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    /**
     * Get model file path in internal storage
     *
     * @return File reference to the model
     */
    fun getModelPath(): File {
        return File(context.filesDir, Constants.MODEL_NAME)
    }

    /**
     * Check if model is already downloaded and valid
     *
     * Verifies file exists and has exact expected size
     *
     * @return true if model exists with correct size
     */
    fun isModelDownloaded(): Boolean {
        val modelFile = getModelPath()
        val exists = modelFile.exists()
        val expectedSize = Constants.MODEL_SIZE_MB * 1024 * 1024
        val isValid = exists && modelFile.length() == expectedSize

        if (exists) {
            val sizeMB = modelFile.length() / (1024 * 1024)
            Log.d(Constants.LOG_TAG, "Model found: size=${sizeMB}MB, valid=$isValid (expected=${Constants.MODEL_SIZE_MB}MB)")
        } else {
            Log.d(Constants.LOG_TAG, "Model not found in internal storage")
        }

        return isValid
    }

    /**
     * Verify model file integrity
     *
     * Checks if file is corrupted or incomplete
     *
     * @return true if file is valid and complete
     */
    fun verifyModelIntegrity(): Boolean {
        val modelFile = getModelPath()
        if (!modelFile.exists()) {
            Log.w(Constants.LOG_TAG, "Model file does not exist for integrity check")
            return false
        }

        val expectedSize = Constants.MODEL_SIZE_MB * 1024 * 1024
        val actualSize = modelFile.length()

        if (actualSize < expectedSize) {
            Log.w(Constants.LOG_TAG, "Model file is incomplete: $actualSize / $expectedSize bytes")
            return false
        }

        if (actualSize > expectedSize) {
            Log.w(Constants.LOG_TAG, "Model file is corrupted (oversized): $actualSize / $expectedSize bytes")
            return false
        }

        Log.d(Constants.LOG_TAG, "Model file integrity verified")
        return true
    }

    /**
     * Download model from Hugging Face with authentication
     *
     * @param token Hugging Face API token
     * @param onProgress Progress callback (0.0-1.0)
     * @return Success with file path, or failure with error
     */
    suspend fun downloadModel(
        token: String,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val modelFile = getModelPath()

        try {
            // Check if model already exists and is complete
            if (modelFile.exists() && modelFile.length() == Constants.MODEL_SIZE_MB * 1024 * 1024) {
                Log.i(Constants.LOG_TAG, "Model already downloaded")
                return@withContext Result.success(modelFile.absolutePath)
            }

            Log.i(Constants.LOG_TAG, "Starting download: ${Constants.MODEL_URL}")

            // Check for partial download
            val startByte = if (modelFile.exists()) modelFile.length() else 0L
            if (startByte > 0) {
                Log.i(Constants.LOG_TAG, "Resuming download from ${startByte / (1024 * 1024)}MB")
            }

            // Build request with authentication and resume support
            val request = Request.Builder()
                .url(Constants.MODEL_URL)
                .header("Authorization", "Bearer $token")
                .apply {
                    if (startByte > 0) {
                        addHeader("Range", "bytes=$startByte-")
                    }
                }
                .build()

            // Execute download
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = when (response.code) {
                        401 -> "${Constants.ERROR_INVALID_TOKEN}: HTTP ${response.code}"
                        403 -> "${Constants.ERROR_DOWNLOAD_FAILED}: Access denied - please accept license on Hugging Face"
                        404 -> "${Constants.ERROR_DOWNLOAD_FAILED}: Model not found"
                        else -> "${Constants.ERROR_DOWNLOAD_FAILED}: HTTP ${response.code}"
                    }
                    Log.e(Constants.LOG_TAG, errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                // Get total size (handle both full and range responses)
                val totalSize = if (response.code == 206) {
                    // Partial content response
                    val contentRange = response.header("Content-Range")
                    contentRange?.substringAfter("/")?.toLongOrNull() ?: 0L
                } else {
                    response.body?.contentLength() ?: 0L
                }

                Log.d(Constants.LOG_TAG, "Total file size: ${totalSize / (1024 * 1024)}MB")

                // Download with progress tracking
                var downloadedSize = startByte
                val buffer = ByteArray(Constants.DOWNLOAD_BUFFER_SIZE)

                response.body?.byteStream()?.use { input ->
                    // Use FileOutputStream with append mode for proper resume support
                    java.io.FileOutputStream(modelFile, startByte > 0).use { output ->
                        var bytesRead: Int
                        var lastProgressUpdate = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedSize += bytesRead

                            // Update progress every second
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastProgressUpdate > 1000 || downloadedSize == totalSize) {
                                val progress = if (totalSize > 0) {
                                    downloadedSize.toFloat() / totalSize
                                } else {
                                    0f
                                }

                                withContext(Dispatchers.Main) {
                                    onProgress(progress)
                                }

                                val downloadedMB = downloadedSize / (1024 * 1024)
                                val totalMB = totalSize / (1024 * 1024)
                                Log.d(
                                    Constants.LOG_TAG,
                                    "Download progress: ${downloadedMB}MB / ${totalMB}MB (${(progress * 100).toInt()}%)"
                                )

                                lastProgressUpdate = currentTime
                            }
                        }
                    }
                }
            }

            // Verify download
            if (!modelFile.exists() || modelFile.length() == 0L) {
                val errorMsg = "${Constants.ERROR_DOWNLOAD_FAILED}: File verification failed"
                Log.e(Constants.LOG_TAG, errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val finalSizeMB = modelFile.length() / (1024 * 1024)
            Log.i(Constants.LOG_TAG, "Download complete: size=${finalSizeMB}MB, path=${modelFile.absolutePath}")

            Result.success(modelFile.absolutePath)

        } catch (e: java.net.SocketTimeoutException) {
            val errorMsg = "${Constants.ERROR_DOWNLOAD_FAILED}: Connection timeout"
            Log.e(Constants.LOG_TAG, errorMsg, e)
            // Partial file is kept for resume
            Result.failure(Exception(errorMsg, e))

        } catch (e: java.io.IOException) {
            val errorMsg = "${Constants.ERROR_DOWNLOAD_FAILED}: Network error - ${e.message}"
            Log.e(Constants.LOG_TAG, errorMsg, e)
            // Partial file is kept for resume
            Result.failure(Exception(errorMsg, e))

        } catch (e: Exception) {
            val errorMsg = "${Constants.ERROR_DOWNLOAD_FAILED}: ${e.message}"
            Log.e(Constants.LOG_TAG, errorMsg, e)
            // Delete partial file on unexpected errors
            modelFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Delete downloaded model file
     *
     * @return true if successfully deleted
     */
    fun deleteModel(): Boolean {
        val modelFile = getModelPath()
        return if (modelFile.exists()) {
            val deleted = modelFile.delete()
            Log.i(Constants.LOG_TAG, "Model deleted: $deleted")
            deleted
        } else {
            Log.w(Constants.LOG_TAG, "Cannot delete: model file does not exist")
            false
        }
    }
}
