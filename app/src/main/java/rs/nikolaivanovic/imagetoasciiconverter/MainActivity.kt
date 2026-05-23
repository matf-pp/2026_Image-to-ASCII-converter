package rs.nikolaivanovic.imagetoasciiconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import rs.nikolaivanovic.imagetoasciiconverter.ui.theme.ImageToAsciiConverterTheme
import rs.nikolaivanovic.imagetoasciiconverter.viewmodels.CameraViewModel

enum class AsciiMode(val label: String, val isColorEnabled: Boolean) {
    Plain("Plain", false),
    Colored("Colored", true)
}

enum class AsciiSizePreset(val label: String, val width: Int) {
    Compact("Compact", 60),
    Balanced("Balanced", 80),
    Detailed("Detailed", 100)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageToAsciiConverterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    val viewModel = ViewModelProvider(this).get(CameraViewModel::class.java)
                    AppNavigation(
                        viewModel = viewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen = remember { mutableStateOf<AppScreen>(AppScreen.Camera) }
    val currentImagePath = remember { mutableStateOf<String?>(null) }
    val asciiResult = remember { mutableStateOf<CameraViewModel.ConversionResult?>(null) }
    val currentMode = remember { mutableStateOf(AsciiMode.Plain) }
    val currentSizePreset = remember { mutableStateOf(AsciiSizePreset.Balanced) }
    val isUpdatingResult = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun renderAscii(
        imagePath: String,
        mode: AsciiMode,
        sizePreset: AsciiSizePreset,
        showLoadingScreen: Boolean
    ) {
        if (showLoadingScreen) {
            currentScreen.value = AppScreen.Loading
        } else {
            isUpdatingResult.value = true
        }

        scope.launch {
            try {
                val result = viewModel.convertImageToAscii(
                    imagePath = imagePath,
                    width = sizePreset.width,
                    isColorEnabled = mode.isColorEnabled
                )
                asciiResult.value = result
                currentScreen.value = AppScreen.AsciiResult
            } finally {
                isUpdatingResult.value = false
            }
        }
    }

    when (currentScreen.value) {
        is AppScreen.Camera -> {
            CameraScreen(
                onImageCaptured = { imagePath ->
                    currentImagePath.value = imagePath
                    renderAscii(
                        imagePath = imagePath,
                        mode = currentMode.value,
                        sizePreset = currentSizePreset.value,
                        showLoadingScreen = true
                    )
                },
                modifier = modifier
            )
        }

        is AppScreen.AsciiResult -> {
            asciiResult.value?.let { result ->
                AsciiResultScreen(
                    result = result,
                    currentMode = currentMode.value,
                    currentSizePreset = currentSizePreset.value,
                    isUpdating = isUpdatingResult.value,
                    onModeSelected = { newMode ->
                        if (newMode != currentMode.value) {
                            currentMode.value = newMode
                            currentImagePath.value?.let { imagePath ->
                                renderAscii(
                                    imagePath = imagePath,
                                    mode = newMode,
                                    sizePreset = currentSizePreset.value,
                                    showLoadingScreen = false
                                )
                            }
                        }
                    },
                    onSizePresetSelected = { newSizePreset ->
                        if (newSizePreset != currentSizePreset.value) {
                            currentSizePreset.value = newSizePreset
                            currentImagePath.value?.let { imagePath ->
                                renderAscii(
                                    imagePath = imagePath,
                                    mode = currentMode.value,
                                    sizePreset = newSizePreset,
                                    showLoadingScreen = false
                                )
                            }
                        }
                    },
                    onBackToCamera = {
                        currentScreen.value = AppScreen.Camera
                    },
                    modifier = modifier
                )
            }
        }

        is AppScreen.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.Green)
            }
        }
    }
}

sealed class AppScreen {
    object Camera : AppScreen()
    object AsciiResult : AppScreen()
    object Loading : AppScreen()
}

@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    ImageToAsciiConverterTheme {
        // Preview placeholder
    }
}