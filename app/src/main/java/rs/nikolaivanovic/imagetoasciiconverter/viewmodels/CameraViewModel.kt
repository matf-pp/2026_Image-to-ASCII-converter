package rs.nikolaivanovic.imagetoasciiconverter.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rs.nikolaivanovic.imagetoasciiconverter.utils.AsciiConverter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CameraViewModel : ViewModel() {

    @RequiresApi(Build.VERSION_CODES.P)
    fun captureImage(
        controller: LifecycleCameraController,
        context: Context,
        onImageCaptured: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val mainExecutor = context.mainExecutor

        controller.takePicture(
            mainExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                    super.onCaptureSuccess(image)

                    // Convert ImageProxy to Bitmap
                    val bitmap = image.toBitmap()

                    // Save to file
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
        width: Int = 80,
        height: Int = 40
    ): String = withContext(Dispatchers.Default) {
        return@withContext try {
            val converter = AsciiConverter()
            converter.convertToAsciiFromPath(imagePath, width, height)
        } catch (e: Exception) {
            "Error converting image: ${e.message}"
        }
    }

    private fun saveBitmapToFile(context: Context, bitmap: Bitmap): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val fileName = "IMG_$timeStamp.jpg"

        val file = File(context.cacheDir, fileName)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        }

        return file
    }
}