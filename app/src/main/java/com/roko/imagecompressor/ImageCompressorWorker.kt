package com.roko.imagecompressor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

class ImageCompressorWorker(
    private val appContext: Context,
    private val workerParams: WorkerParameters
): CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val stringUri = workerParams.inputData.getString(KEY_CONTENT_URI)
        val compressionThresholdInBytes = workerParams.inputData.getLong(
            KEY_COMPRESSION_THRESHOLD,
            0L
        )
        val uri = stringUri?.toUri() ?: return@withContext Result.failure()
        val bytes = appContext.contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        } ?: return@withContext Result.failure()

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        var outputBytes: ByteArray
        var quality = 100
        do {
            val outputStream = ByteArrayOutputStream()
            outputStream.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
                outputBytes = it.toByteArray()
                quality -= (quality * 0.1).roundToInt()
            }
        } while (outputBytes.size > compressionThresholdInBytes && quality > 5)

        val file = File(appContext.cacheDir, "${workerParams.id}.jpg")
        file.writeBytes(outputBytes)

        Result.success(
            workDataOf(
                KEY_RESULT_PATH to file.absolutePath
            )
        )

    }

    companion object {
        const val KEY_CONTENT_URI = "KEY_CONTENT_URI"
        const val KEY_COMPRESSION_THRESHOLD = "KEY_COMPRESSION_THRESHOLD"
        const val KEY_RESULT_PATH = "KEY_RESULT_PATH"
    }
}