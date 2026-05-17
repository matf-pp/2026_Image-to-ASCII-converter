package rs.nikolaivanovic.imagetoasciiconverter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AsciiResultScreen(
    asciiArt: String,
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "ASCII Art Result",
            fontSize = 24.sp,
            color = Color.Green,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ASCII Art Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.DarkGray)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = asciiArt,
                fontSize = 6.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.Green,
                lineHeight = 8.sp
            )
        }

        // Back to Camera Button
        Button(
            onClick = onBackToCamera,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(text = "Back to Camera", fontSize = 16.sp)
        }
    }
}