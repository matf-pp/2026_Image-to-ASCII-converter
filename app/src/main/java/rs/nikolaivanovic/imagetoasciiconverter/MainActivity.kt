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
import rs.nikolaivanovic.imagetoasciiconverter.utils.AsciiConverter
import rs.nikolaivanovic.imagetoasciiconverter.viewmodels.CameraViewModel

// Configuration for ASCII rendering styles
enum class AsciiMode(val label: String, val isColorEnabled: Boolean) {
    Plain("Plain", false),
    Colored("Colored", true)
}

// Maps UI selection labels to technical resolution (width) and character-set quality
enum class AsciiSizePreset(val label: String, val width: Int, val quality: AsciiConverter.Quality) {
    Compact("Compact", 60, AsciiConverter.Quality.LOW),
    Balanced("Balanced", 80,  AsciiConverter.Quality.MEDIUM),
    Detailed("Detailed", 100,  AsciiConverter.Quality.ULTRA)
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

/*
 This composable acts as the "Single Source of Truth" and Router for the application
 It tracks the navigation state, currently selected image, and conversion results
 */
@Composable
fun AppNavigation(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    // UI state variables
    val currentScreen = remember { mutableStateOf<AppScreen>(AppScreen.Camera) }
    val currentImagePath = remember { mutableStateOf<String?>(null) }
    val asciiResult = remember { mutableStateOf<CameraViewModel.ConversionResult?>(null) }
    
    // Configuration states
    val currentMode = remember { mutableStateOf(AsciiMode.Plain) }
    val currentSizePreset = remember { mutableStateOf(AsciiSizePreset.Balanced) }
    
    // Tracking if an asynchronous update is happening to show progress indicators
    val isUpdatingResult = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    /*
     This function manages the workflow of converting an image file into an ASCII result.
     It handles two types of loading:
     1. A full-screen loader (when first entering the result screen).
     2. A transparent overlay (when the user toggles settings while viewing the art).
     */
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
                    isColorEnabled = mode.isColorEnabled,
                    quality = sizePreset.quality
                )
                asciiResult.value = result
                currentScreen.value = AppScreen.AsciiResult
            } finally {
                isUpdatingResult.value = false
            }
        }
    }

    // Navigation logic
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
                        // Resetting state for a fresh capture.
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

// Simple state definitions for navigation.
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