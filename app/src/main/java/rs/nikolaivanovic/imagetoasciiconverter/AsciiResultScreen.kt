package rs.nikolaivanovic.imagetoasciiconverter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rs.nikolaivanovic.imagetoasciiconverter.viewmodels.CameraViewModel

@Composable
fun AsciiResultScreen(
    result: CameraViewModel.ConversionResult,
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val displayContent = when (result) {
        is CameraViewModel.ConversionResult.PlainText -> AnnotatedString(result.text)
        is CameraViewModel.ConversionResult.ColoredText -> {
            buildAnnotatedString {
                result.coloredChars.forEach { coloredChar ->
                    if (coloredChar.char == '\n') {
                        append("\n")
                    } else {
                        withStyle(style = SpanStyle(color = Color(coloredChar.color))) {
                            append(coloredChar.char.toString())
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(
                onClick = onBackToCamera,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Camera",
                    tint = Color.White
                )
            }

            Text(
                text = "ASCII Art",
                fontSize = 24.sp,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ASCII Art Display Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    Color(0xFF1A1A1A),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = displayContent,
                fontSize = 4.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                lineHeight = 5.sp,
                softWrap = false,
                modifier = Modifier
            )
        }

        // Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onBackToCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text(
                    text = "📷 Capture Another",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Button(
                onClick = onBackToCamera,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF424242)
                )
            ) {
                Text(
                    text = "← Back to Camera",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}