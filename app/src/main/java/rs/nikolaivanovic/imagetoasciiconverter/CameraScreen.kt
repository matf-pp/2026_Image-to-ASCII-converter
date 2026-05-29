package rs.nikolaivanovic.imagetoasciiconverter

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rs.nikolaivanovic.imagetoasciiconverter.viewmodels.CameraViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onImageCaptured: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val permissionStatus = cameraPermissionState.status
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    /*
    Determine if we should show a "Settings" button instead of a request button.
    This happens when the user has permanently denied the permission.
    */
    val shouldShowSettingsButton = when (permissionStatus) {
        PermissionStatus.Granted -> false
        is PermissionStatus.Denied -> !permissionStatus.shouldShowRationale
    }

    // Launcher for selecting an existing photo from the gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    // Image decoding and file saving is performed on IO thread to prevent UI freezing
                    val file = withContext(Dispatchers.IO) {
                        val bitmap = context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        } ?: throw IllegalStateException("Could not decode selected image")

                        viewModel.saveBitmapToFile(context, bitmap)
                    }

                    onImageCaptured(file.absolutePath)
                } catch (_: Exception) {
                    Toast.makeText(context, "Failed to load gallery image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Automatically request permission when the screen starts if it's not granted
    LaunchedEffect(permissionStatus.isGranted) {
        if (!permissionStatus.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (permissionStatus.isGranted) {
        CameraPreview(
            onImageCaptured = onImageCaptured,
            viewModel = viewModel,
            onPickFromGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = modifier
        )
    } else {
        CameraPermissionScreen(
            shouldShowSettingsButton = shouldShowSettingsButton,
            onRequestPermission = {
                cameraPermissionState.launchPermissionRequest()
            },
            onPickFromGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onOpenSettings = {
                // Navigates the user directly to the app's settings page in Android
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            },
            modifier = modifier
        )
    }
}

@Composable
fun CameraPreview(
    onImageCaptured: (String) -> Unit,
    viewModel: CameraViewModel,
    onPickFromGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var useFrontCamera by remember { mutableStateOf(false) }

    // LifecycleCameraController handles the boilerplate of connecting to the camera hardware
    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    // We bind the controller to the lifecycle so it starts/stops automatically with the screen
    DisposableEffect(cameraController, lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)

        onDispose {
            // Unbinding ensures camera resources are released correctly
            cameraController.unbind()
        }
    }

    // Reactive update to switch between front and back camera lenses
    LaunchedEffect(useFrontCamera) {
        cameraController.cameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // AndroidView is used to host the non-Compose PreviewView required by CameraX
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    label = "Gallery",
                    symbol = "▣",
                    onClick = onPickFromGallery
                )

                ShutterButton(
                    onClick = {
                        // We pass the current camera controller to the ViewModel to execute the capture
                        viewModel.captureImage(
                            controller = cameraController,
                            context = context,
                            // Mirroring is applied to selfies so the text doesn't appear backwards
                            mirrorHorizontally = useFrontCamera,
                            onImageCaptured = { file ->
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
                    }
                )

                ControlButton(
                    label = if (useFrontCamera) "Back" else "Front",
                    symbol = "↺",
                    onClick = {
                        useFrontCamera = !useFrontCamera
                    }
                )
            }
        }
    }
}

@Composable
fun ControlButton(
    label: String,
    symbol: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun ShutterButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(82.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White.copy(alpha = 0.92f), CircleShape)
            .padding(8.dp)
            .background(Color.Transparent, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun CameraPermissionScreen(
    shouldShowSettingsButton: Boolean,
    onRequestPermission: () -> Unit,
    onPickFromGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101010)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(Color(0xFF1B1B1B), RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Camera Permission Needed",
                color = Color.White,
                fontSize = 24.sp
            )

            Text(
                text = "You can use the camera to capture a new image, or continue with an image from your gallery.",
                color = Color(0xFFD0D0D0),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
            )

            Button(
                onClick = if (shouldShowSettingsButton) onOpenSettings else onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Text(if (shouldShowSettingsButton) "Open Settings" else "Allow Camera")
            }

            Button(
                onClick = onPickFromGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text("Choose From Gallery")
            }
        }
    }
}