package rs.nikolaivanovic.imagetoasciiconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import rs.nikolaivanovic.imagetoasciiconverter.ui.theme.ImageToAsciiConverterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageToAsciiConverterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    CameraScreen(
                        onImageCaptured = { imagePath ->
                            // TODO: Handle captured image for ASCII conversion
                            println("Image captured at: $imagePath")
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    ImageToAsciiConverterTheme {
        // Preview placeholder since camera preview can't be shown in preview
    }
}