package com.example.gemmabench.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * âÇëÕ¡¤ëÀ¦óíüÀü
 *
 * Hugging FaceK‰Gemma 3nâÇë4.4GB	’ŸLBkÀ¦óíüÉ
 */
class ModelDownloader(private val context: Context) {

    /**
     * âÇëÕ¡¤ënÝXHÑ¹’Ö—
     *
     * @return âÇëÕ¡¤ënFileªÖ¸§¯È
     */
    fun getModelPath(): File {
        return File(context.filesDir, Constants.MODEL_NAME)
    }

    /**
     * âÇëLYgkÀ¦óíüÉKÁ§Ã¯
     *
     * @return À¦óíüÉn4true
     */
    fun isModelDownloaded(): Boolean {
        val modelFile = getModelPath()
        val exists = modelFile.exists()
        val isValid = exists && modelFile.length() > 0

        if (exists) {
            val sizeMB = modelFile.length() / (1024 * 1024)
            Log.d(Constants.LOG_TAG, "âÇëÕ¡¤ëº: size=${sizeMB}MB, valid=$isValid")
        } else {
            Log.d(Constants.LOG_TAG, "âÇëÕ¡¤ë*ú")
        }

        return isValid
    }

    /**
     * âÇë’À¦óíüÉ
     *
     * @param onProgress 2W³üëÐÃ¯0.0-1.0	
     * @return ŸBoâÇëÕ¡¤ënvþÑ¹1WBo¨éü
     */
    suspend fun downloadModel(
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val modelFile = getModelPath()

        try {
            Log.i(Constants.LOG_TAG, "À¦óíüÉ‹Ë: ${Constants.MODEL_URL}")

            // âXÕ¡¤ëLBŒpJdŒhjÀ¦óíüÉnïý'	
            if (modelFile.exists()) {
                Log.w(Constants.LOG_TAG, "âXÕ¡¤ë’Jd")
                modelFile.delete()
            }

            // HTTP¥š-š
            val url = URL(Constants.MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = Constants.DOWNLOAD_TIMEOUT_MS.toInt()
            connection.readTimeout = Constants.DOWNLOAD_TIMEOUT_MS.toInt()
            connection.requestMethod = "GET"

            connection.connect()

            // HTTP¹Æü¿¹º
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorMsg = "${Constants.ERROR_DOWNLOAD_FAILED}: HTTP $responseCode"
                Log.e(Constants.LOG_TAG, errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            // Õ¡¤ëµ¤ºÖ—
            val totalSize = connection.contentLengthLong
            Log.d(Constants.LOG_TAG, "Õ¡¤ëµ¤º: ${totalSize / (1024 * 1024)}MB")

            // À¦óíüÉŸL
            var downloadedSize = 0L
            val buffer = ByteArray(Constants.DOWNLOAD_BUFFER_SIZE)

            connection.inputStream.use { input ->
                modelFile.outputStream().use { output ->
                    var bytesRead: Int
                    var lastProgressUpdate = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedSize += bytesRead

                        // 2Wô°1MBÎ	
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
                                "À¦óíüÉ2W: ${downloadedMB}MB / ${totalMB}MB (${(progress * 100).toInt()}%)"
                            )

                            lastProgressUpdate = currentTime
                        }
                    }
                }
            }

            connection.disconnect()

            // À¦óíüÉŒ†<
            if (!modelFile.exists() || modelFile.length() == 0L) {
                val errorMsg = "${Constants.ERROR_DOWNLOAD_FAILED}: Õ¡¤ënÝXk1W"
                Log.e(Constants.LOG_TAG, errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val finalSizeMB = modelFile.length() / (1024 * 1024)
            Log.i(Constants.LOG_TAG, "À¦óíüÉŒ†: size=${finalSizeMB}MB, path=${modelFile.absolutePath}")

            Result.success(modelFile.absolutePath)

        } catch (e: java.net.SocketTimeoutException) {
            val errorMsg = "${Constants.ERROR_DOWNLOAD_FAILED}: ¿¤à¢¦È"
            Log.e(Constants.LOG_TAG, errorMsg, e)
            // ŒhjÕ¡¤ë’Jd
            modelFile.delete()
            Result.failure(Exception(errorMsg, e))

        } catch (e: java.io.IOException) {
            val errorMsg = "${Constants.ERROR_DOWNLOAD_FAILED}: ÍÃÈïü¯¨éü"
            Log.e(Constants.LOG_TAG, errorMsg, e)
            // ŒhjÕ¡¤ë’Jd
            modelFile.delete()
            Result.failure(Exception(errorMsg, e))

        } catch (e: Exception) {
            val errorMsg = "${Constants.ERROR_DOWNLOAD_FAILED}: ${e.message}"
            Log.e(Constants.LOG_TAG, errorMsg, e)
            // ŒhjÕ¡¤ë’Jd
            modelFile.delete()
            Result.failure(e)
        }
    }

    /**
     * À¦óíüÉâÇë’Jd
     *
     * @return JdŸBtrue
     */
    fun deleteModel(): Boolean {
        val modelFile = getModelPath()
        return if (modelFile.exists()) {
            val deleted = modelFile.delete()
            Log.i(Constants.LOG_TAG, "âÇëÕ¡¤ëJd: $deleted")
            deleted
        } else {
            Log.w(Constants.LOG_TAG, "JdþanâÇëÕ¡¤ëLX(W~[“")
            false
        }
    }
}
