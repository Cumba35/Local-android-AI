
package com.localllm.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percent: Int =
        if (totalBytes > 0)
            ((bytesDownloaded * 100) / totalBytes).toInt()
        else 0,
    val done: Boolean = false,
    val error: String? = null
)

object ModelDownloader {

    // Modelo local de Nova AI
    const val DEFAULT_MODEL_NAME =
        "LFM2.5-350M-Q4_K_M.gguf"

    const val DEFAULT_MODEL_URL =
        "https://huggingface.co/LiquidAI/LFM2.5-350M-GGUF/resolve/main/LFM2.5-350M-Q4_K_M.gguf"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    fun modelFile(ctx: Context): File {
        val directory = ctx.getExternalFilesDir(null)
            ?: ctx.filesDir
        return File(directory, DEFAULT_MODEL_NAME)
    }

    fun isModelDownloaded(ctx: Context): Boolean {
        val file = modelFile(ctx)
        return file.exists() &&
            file.isFile &&
            file.length() > 100_000_000L
    }

    suspend fun download(
        ctx: Context,
        url: String = DEFAULT_MODEL_URL,
        onProgress: (DownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {

        val dest = modelFile(ctx)
        val tmpFile = File(
            dest.parentFile,
            dest.name + ".tmp"
        )

        try {
            dest.parentFile?.mkdirs()

            val existingBytes =
                if (tmpFile.exists()) tmpFile.length()
                else 0L

            val requestBuilder = Request.Builder()
                .url(url)

            if (existingBytes > 0L) {
                requestBuilder.header(
                    "Range",
                    "bytes=$existingBytes-"
                )
            }

            client.newCall(requestBuilder.build())
                .execute().use { response ->

                    if (!response.isSuccessful) {
                        onProgress(
                            DownloadProgress(
                                0, 0,
                                error = "HTTP ${response.code}"
                            )
                        )
                        return@withContext
                    }

                    val body = response.body
                    if (body == null) {
                        onProgress(
                            DownloadProgress(
                                0, 0,
                                error = "Respuesta vacía"
                            )
                        )
                        return@withContext
                    }

                    // Solo añadir al archivo si el servidor
                    // confirma que envía el resto del archivo.
                    val append =
                        existingBytes > 0L &&
                        response.code == 206

                    val initialBytes =
                        if (append) existingBytes else 0L

                    val contentLength = body.contentLength()

                    val totalBytes =
                        if (contentLength > 0L) {
                            initialBytes + contentLength
                        } else {
                            0L
                        }

                    body.byteStream().use { input ->
                        tmpFile.outputStream(
                            append = append
                        ).use { output ->

                            val buffer = ByteArray(8192)
                            var downloaded = initialBytes

                            while (true) {
                                val count = input.read(buffer)
                                if (count == -1) break

                                output.write(buffer, 0, count)
                                downloaded += count

                                onProgress(
                                    DownloadProgress(
                                        downloaded,
                                        totalBytes
                                    )
                                )
                            }

                            output.flush()
                        }
                    }

                    val actualSize = tmpFile.length()

                    if (actualSize < 100_000_000L) {
                        onProgress(
                            DownloadProgress(
                                actualSize,
                                totalBytes,
                                error = "Archivo incompleto. " +
                                    "Comprueba la URL e inténtalo de nuevo."
                            )
                        )
                        return@withContext
                    }

                    // Reemplazar el archivo anterior.
                    if (dest.exists() && !dest.delete()) {
                        onProgress(
                            DownloadProgress(
                                actualSize,
                                totalBytes,
                                error = "No se pudo reemplazar el modelo"
                            )
                        )
                        return@withContext
                    }

                    if (!tmpFile.renameTo(dest)) {
                        onProgress(
                            DownloadProgress(
                                actualSize,
                                totalBytes,
                                error = "No se pudo guardar el modelo"
                            )
                        )
                        return@withContext
                    }

                    onProgress(
                        DownloadProgress(
                            dest.length(),
                            dest.length(),
                            100,
                            done = true
                        )
                    )
                }

        } catch (e: Exception) {
            onProgress(
                DownloadProgress(
                    0,
                    0,
                    error = e.message ?: "Error de descarga"
                )
            )
        }
    }
}
