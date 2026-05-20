package rs.nikolaivanovic.imagetoasciiconverter

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
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

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
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

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AppNavigation(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen = remember { mutableStateOf<AppScreen>(AppScreen.Camera) }
    val asciiResult = remember { mutableStateOf<CameraViewModel.ConversionResult?>(null) }
    val isColorEnabled = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    when (currentScreen.value) {
        is AppScreen.Camera -> {
            CameraScreen(
                isColorEnabled = isColorEnabled.value,
                onColorToggle = { isColorEnabled.value = it },
                onImageCaptured = { imagePath ->
                    currentScreen.value = AppScreen.Loading
                    scope.launch {
                        val result = viewModel.convertImageToAscii(
                            imagePath,
                            width = 80,
                            isColorEnabled = isColorEnabled.value
                        )
                        asciiResult.value = result
                        currentScreen.value = AppScreen.AsciiResult
                    }
                },
                modifier = modifier
            )
        }
        is AppScreen.AsciiResult -> {
            asciiResult.value?.let { result ->
                AsciiResultScreen(
                    result = result,
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