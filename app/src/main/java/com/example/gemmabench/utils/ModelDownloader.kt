package com.example.gemmabench.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Model file downloader
 *
 * Downloads Gemma 3n model (4.4GB) from Hugging Face
 */
class ModelDownloader(private val context: Context) {

    /**
     * Get model file path
     *
     * @return File reference to the model
     */
    fun getModelPath(): File {
        return File(context.filesDir, Constants.MODEL_NAME)
    }

    /**
     * Check if model is already downloaded and valid
     *
     * @return true if model exists and valid
     */
    fun isModelDownloaded(): Boolean {
        val modelFile = getModelPath()
        val exists = modelFile.exists()
        val isValid = exists && modelFile.length() > 0

        if (exists) {
            val sizeMB = modelFile.length() / (1024 * 1024)
            Log.d(Constants.LOG_TAG, "Model found: size=${sizeMB}MB, valid=$isValid")
        } else {
            Log.d(Constants.LOG_TAG, "Model not found")
        }

        return isValid
    }

    /**
     * Download model from Hugging Face
     *
     * @param onProgress Progress callback (0.0-1.0)
     * @return Success with file path, or failure with error
     */
    suspend fun downloadModel(
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val modelFile = getModelPath()

        try {
            Log.i(Constants.LOG_TAG, "Starting download: ${Constants.MODEL_URL}")

            // Delete existing partial file if needed
            if (modelFile.exists()) {
                Log.w(Constants.LOG_TAG, "Deleting existing partial file")
                modelFile.delete()
            }

            // Configure HTTP connection
            val url = URL(Constants.MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = Constants.DOWNLOAD_TIMEOUT_MS.toInt()
            connection.readTimeout = Constants.DOWNLOAD_TIMEOUT_MS.toInt()
            connection.requestMethod = "GET"

            connection.connect()

            // Check HTTP response code
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorMsg = "${Constants.ERROR_DOWNLOAD_FAILED}: HTTP $responseCode"
                Log.e(Constants.LOG_TAG, errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            // Get file size
            val totalSize = connection.contentLengthLong
            Log.d(Constants.LOG_TAG, "File size: ${totalSize / (1024 * 1024)}MB")

            // Download with progress tracking
            var downloadedSize = 0L
            val buffer = ByteArray(Constants.DOWNLOAD_BUFFER_SIZE)

            connection.inputStream.use { input ->
                modelFile.outputStream().use { output ->
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

            connection.disconnect()

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
            // Delete partial file
            modelFile.delete()
            Result.failure(Exception(errorMsg, e))

        } catch (e: java.io.IOException) {
            val errorMsg = "${Constants.ERROR_DOWNLOAD_FAILED}: Network error"
            Log.e(Constants.LOG_TAG, errorMsg, e)
            // Delete partial file
            modelFile.delete()
            Result.failure(Exception(errorMsg, e))

        } catch (e: Exception) {
            val errorMsg = "${Constants.ERROR_DOWNLOAD_FAILED}: ${e.message}"
            Log.e(Constants.LOG_TAG, errorMsg, e)
            // Delete partial file
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
