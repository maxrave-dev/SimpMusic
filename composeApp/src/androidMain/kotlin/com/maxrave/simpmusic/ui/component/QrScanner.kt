package com.maxrave.simpmusic.ui.component

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.maxrave.logger.Logger
import java.util.concurrent.Executors

/**
 * Camera preview that reports every QR code it reads.
 *
 * CameraX + ZXing rather than ML Kit or Google's code scanner: both of those are proprietary, and
 * F-Droid refuses to build an app that depends on either. QR is found by its finder patterns, so the
 * frame is decoded as the sensor delivers it — no rotation needed.
 */
@Composable
internal fun QrScanner(
    onResult: (String) -> Unit,
    onCameraUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnCameraUnavailable by rememberUpdatedState(onCameraUnavailable)
    // COMPATIBLE = TextureView. The default SurfaceView ignores Compose clipping, so the square
    // viewfinder's rounded corners would render as square ones.
    val previewView =
        remember {
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }

    DisposableEffect(lifecycleOwner) {
        val analyzerThread = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        // The provider can arrive after the scanner has already left; binding then would hold the
        // camera until the activity dies.
        var disposed = false
        val reader = QRCodeReader()
        providerFuture.addListener({
            if (disposed) return@addListener
            try {
                val cameraProvider = providerFuture.get().also { provider = it }
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis =
                    ImageAnalysis
                        .Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                analysis.setAnalyzer(analyzerThread) { image ->
                    image.use { decodeQr(it, reader) }?.let { text -> mainExecutor.execute { currentOnResult(text) } }
                }
                // Tablets and Chromebooks may only have a front camera.
                val selector =
                    if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            } catch (e: Exception) {
                Logger.e("QrScanner", "camera: ${e.message}")
                currentOnCameraUnavailable()
            }
        }, mainExecutor)
        onDispose {
            disposed = true
            provider?.unbindAll()
            analyzerThread.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private val DECODE_HINTS = mapOf(DecodeHintType.TRY_HARDER to true)

/** Reads the luminance (Y) plane only, which is all ZXing needs. */
private fun decodeQr(
    image: ImageProxy,
    reader: QRCodeReader,
): String? {
    val plane = image.planes[0]
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
    // rowStride, not width, is the real distance between rows: many sensors pad each row.
    val source = PlanarYUVLuminanceSource(bytes, plane.rowStride, image.height, 0, 0, image.width, image.height, false)
    return try {
        reader.decode(BinaryBitmap(HybridBinarizer(source)), DECODE_HINTS).text
    } catch (e: ReaderException) {
        null
    } finally {
        reader.reset()
    }
}
