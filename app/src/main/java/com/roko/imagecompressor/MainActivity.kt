package com.roko.imagecompressor

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.roko.imagecompressor.ui.theme.ImageCompressorTheme

class MainActivity : ComponentActivity() {

    private lateinit var workManager: WorkManager
    private val viewModel by viewModels<ImageCompressorViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)
        onNewIntent(intent)
        enableEdgeToEdge()
        setContent {
            ImageCompressorTheme {
                val workerResult = viewModel.workId?.let { id ->
                    workManager.getWorkInfoByIdLiveData(id).observeAsState().value
                }
                LaunchedEffect(key1 = workerResult?.outputData) {
                    if (workerResult?.outputData != null) {
                        workerResult.outputData.getString(
                            ImageCompressorWorker.KEY_RESULT_PATH
                        )?.let { filePath ->
                            val bitmap = BitmapFactory.decodeFile(filePath)
                            viewModel.updateCompressedBitmap(bitmap)
                        }
                        workerResult.outputData.getInt(
                            ImageCompressorWorker.KEY_RESULT_QUALITY,
                            100
                        ).also { quality ->
                            viewModel.updateQuality(quality)
                        }

                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                   Column(
                       modifier = Modifier
                           .fillMaxSize()
                           .padding(innerPadding),
                       verticalArrangement = Arrangement.Center,
                       horizontalAlignment = Alignment.CenterHorizontally
                   ) {
                       viewModel.uncompressedUri?.let {
                           Text( text = "Uncompressed Image: ${viewModel.uncompressedSize}")
                           AsyncImage(model = it, contentDescription = null)
                       }
                       Spacer(modifier = Modifier.height(16.dp))
                       viewModel.compressedBitmap?.let {
                           Text( text = "Compressed Image: ${viewModel.quality}")
                           Image(bitmap = it.asImageBitmap(), contentDescription = null)
                       }
                   }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: return
        viewModel.updateUncompressedUri(uri)

        val request = OneTimeWorkRequestBuilder<ImageCompressorWorker>()
            .setInputData(
                workDataOf(
                    ImageCompressorWorker.KEY_CONTENT_URI to uri.toString(),
                    ImageCompressorWorker.KEY_COMPRESSION_THRESHOLD to 1024 * 20L
                )
            )
            .setConstraints(Constraints(
                    requiresStorageNotLow = true
            ))
            .build()
        viewModel.updateWorkId(request.id)
        workManager.enqueue(request)
    }
}