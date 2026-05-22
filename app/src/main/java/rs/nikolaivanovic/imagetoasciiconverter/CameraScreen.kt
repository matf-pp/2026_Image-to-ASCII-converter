package rs.nikolaivanovic.imagetoasciiconverter

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import rs.nikolaivanovic.imagetoasciiconverter.viewmodels.CameraViewModel

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onImageCaptured: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current

    // Request permission in a side effect
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        val viewModel = ViewModelProvider(context as ComponentActivity).get(CameraViewModel::class.java)
        CameraPreview(
            onImageCaptured = onImageCaptured,
            viewModel = viewModel,
            modifier = modifier
        )
    } else {
        // Empty box while waiting for permission dialog
        Box(modifier = modifier.fillMaxSize())
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun CameraPreview(
    onImageCaptured: (String) -> Unit,
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraController = LifecycleCameraController(context).apply {
        setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        bindToLifecycle(lifecycleOwner)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(selectedUri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap?.let {
                    // Note: You must remove 'private' from saveBitmapToFile in CameraViewModel
                    val file = viewModel.saveBitmapToFile(context, it)
                    onImageCaptured(file.absolutePath)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load gallery image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // UI Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.6f), CircleShape)
                    .clickable {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("🖼️", fontSize = 24.sp)
            }

            // Capture Button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.8f), CircleShape)
                    .clickable {
                        viewModel.captureImage(
                            controller = cameraController,
                            context = context,
                            onImageCaptured = { file ->
                                Toast.makeText(
                                    context,
                                    "Image saved: ${file.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onImageCaptured(file.absolutePath)
                            },
                            onError = { exception ->
                                Toast.makeText(
                                    context,
                                    "Capture failed: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "●",
                    fontSize = 32.sp,
                    color = Color.Black
                )
            }

            // Empty space to keep capture button centered
            Box(modifier = Modifier.size(56.dp))
        }
    }
}