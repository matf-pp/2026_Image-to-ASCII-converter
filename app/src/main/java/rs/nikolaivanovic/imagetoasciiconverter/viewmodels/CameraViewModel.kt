package rs.nikolaivanovic.imagetoasciiconverter.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rs.nikolaivanovic.imagetoasciiconverter.utils.AsciiConverter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CameraViewModel : ViewModel() {

    sealed class ConversionResult(
        open val width: Int,
        open val plainText: String
    ) {
        data class PlainText(
            val text: String,
            override val width: Int
        ) : ConversionResult(width = width, plainText = text)

        data class ColoredText(
            val coloredChars: List<AsciiConverter.ColoredChar>,
            override val width: Int,
            override val plainText: String
        ) : ConversionResult(width = width, plainText = plainText)
    }

    fun captureImage(
        controller: LifecycleCameraController,
        context: Context,
        mirrorHorizontally: Boolean = false,
        onImageCaptured: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val mainExecutor = ContextCompat.getMainExecutor(context)

        controller.takePicture(
            mainExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                    super.onCaptureSuccess(image)

                    var bitmap = image.toBitmap()
                    bitmap = rotateBitmapIfNeeded(bitmap, image.imageInfo.rotationDegrees)

                    if (mirrorHorizontally) {
                        bitmap = mirrorBitmapHorizontally(bitmap)
                    }

                    val file = saveBitmapToFile(context, bitmap)
                    onImageCaptured(file)
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    onError(exception)
                }
            }
        )
    }

    suspend fun convertImageToAscii(
        imagePath: String,
        width: Int = 100,
        isColorEnabled: Boolean = false
    ): ConversionResult = withContext(Dispatchers.Default) {
        val converter = AsciiConverter()

        return@withContext if (isColorEnabled) {
            val coloredChars = converter.convertToColoredAsciiFromPath(
                imagePath,
                width,
                AsciiConverter.Quality.ULTRA
            )

            val plainText = buildString {
                coloredChars.forEach { append(it.char) }
            }

            ConversionResult.ColoredText(
                coloredChars = coloredChars,
                width = width,
                plainText = plainText
            )
        } else {
            val plainText = converter.convertToAsciiFromPath(
                imagePath,
                width,
                AsciiConverter.Quality.ULTRA
            )

            ConversionResult.PlainText(
                text = plainText,
                width = width
            )
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    private fun mirrorBitmapHorizontally(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            preScale(-1f, 1f)
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val fileName = "IMG_$timeStamp.jpg"

        val file = File(context.cacheDir, fileName)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        }

        return file
    }
}