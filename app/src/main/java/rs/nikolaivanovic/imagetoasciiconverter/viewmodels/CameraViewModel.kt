package rs.nikolaivanovic.imagetoasciiconverter.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.ViewModel
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
                override fun onCaptureSuccess(image: ImageProxy) {
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