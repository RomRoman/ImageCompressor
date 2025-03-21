package com.roko.imagecompressor

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.util.UUID

class ImageCompressorViewModel(
    private val application: Application,
): AndroidViewModel(application) {

    var uncompressedUri: Uri? by mutableStateOf(null)
        private set

    var uncompressedSize: Float by mutableFloatStateOf(0f)

    var compressedBitmap: Bitmap? by mutableStateOf(null)
        private set

    var workId: UUID? by mutableStateOf(null)
        private set

    var quality: Int by mutableIntStateOf(100)
        private set

    fun updateUncompressedUri(uri: Uri) {
        uncompressedUri = uri
        val bytes =  application.contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        }

        uncompressedSize = (bytes?.size?.toFloat() ?: 0f) / 1024

    }

    fun updateCompressedBitmap(bitmap: Bitmap) {
        compressedBitmap = bitmap
    }

    fun updateWorkId(uuid: UUID) {
        workId = uuid
    }

    fun updateQuality(quality: Int) {
        this.quality = quality
    }


}