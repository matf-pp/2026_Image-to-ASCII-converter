package rs.nikolaivanovic.imagetoasciiconverter

import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import rs.nikolaivanovic.imagetoasciiconverter.viewmodels.CameraViewModel

@Composable
fun AsciiResultScreen(
    result: CameraViewModel.ConversionResult,
    currentMode: AsciiMode,
    currentSizePreset: AsciiSizePreset,
    isUpdating: Boolean,
    onModeSelected: (AsciiMode) -> Unit,
    onSizePresetSelected: (AsciiSizePreset) -> Unit,
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    var modeMenuExpanded by remember { mutableStateOf(false) }
    var sizeMenuExpanded by remember { mutableStateOf(false) }

    val plainAsciiText = result.plainText.trimEnd('\n')

    // uses the Storage Access Framework for safely writing files
    val saveAsciiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            try {
                // openOutputStream is the standard way to write data to a URI provided by SAF
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.writer().use { writer ->
                        writer.write(plainAsciiText)
                    }
                }
                Toast.makeText(context, "ASCII saved as .txt", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to save .txt file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /*
    buildAnnotatedString is crucial for colored ASCII because it allows us to
    attach different SpanStyles (colors) to individual characters in a single string
    */
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

    val lines = result.plainText.trimEnd('\n').lines().ifEmpty { listOf("") }
    val rowCount = max(lines.size, 1)
    val columnCount = max(lines.maxOfOrNull { it.length } ?: 1, 1)
    /*
    We track the container size to dynamically calculate the font size so
    the ASCII art fits perfectly within the viewer
    */
    var viewerSize by remember { mutableStateOf(Size(0, 0)) }

    val fontSize = remember(viewerSize, rowCount, columnCount, density) {
        calculateAsciiFontSize(
            widthPx = viewerSize.width,
            heightPx = viewerSize.height,
            columnCount = columnCount,
            rowCount = rowCount,
            density = density
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D10))
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackToCamera,
                modifier = Modifier
                    .background(Color(0xFF171B21), CircleShape)
                    .border(1.dp, Color(0xFF2A3038), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Camera",
                    tint = Color.White
                )
            }

            Text(
                text = "ASCII Result",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )

            Box(
                modifier = Modifier
                    .background(Color(0xFF171B21), CircleShape)
                    .border(1.dp, Color(0xFF2A3038), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$columnCount x $rowCount",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                InteractiveInfoChip(
                    label = "Mode",
                    value = currentMode.label,
                    onClick = { modeMenuExpanded = true }
                )

                DropdownMenu(
                    expanded = modeMenuExpanded,
                    onDismissRequest = { modeMenuExpanded = false }
                ) {
                    AsciiMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            onClick = {
                                modeMenuExpanded = false
                                onModeSelected(mode)
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                InteractiveInfoChip(
                    label = "Size",
                    value = currentSizePreset.label,
                    onClick = { sizeMenuExpanded = true }
                )

                DropdownMenu(
                    expanded = sizeMenuExpanded,
                    onDismissRequest = { sizeMenuExpanded = false }
                ) {
                    AsciiSizePreset.entries.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text("${preset.label} (${preset.width})") },
                            onClick = {
                                sizeMenuExpanded = false
                                onSizePresetSelected(preset)
                            }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    color = Color(0xFF11151A),
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF252C35),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0A0D11))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF1C232C),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(10.dp)
                    .onSizeChanged { size ->
                        viewerSize = Size(size.width, size.height)
                    }
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = displayContent,
                    fontSize = fontSize,
                    lineHeight = fontSize * 1.02f,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    softWrap = false
                )

                if (isUpdating) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.40f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Text(
                                text = "Updating...",
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val fileName = "ascii_art_${currentMode.label.lowercase()}_${currentSizePreset.label.lowercase()}.txt"
                    saveAsciiLauncher.launch(fileName)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E88E5),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Save as TXT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onBackToCamera,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2A313B)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF13171C),
                    contentColor = Color(0xFFD7DEE8)
                )
            ) {
                Text(
                    text = "New Capture",
                    fontSize = 15.sp
                )
            }
        }
    }
}

private fun calculateAsciiFontSize(
    widthPx: Int,
    heightPx: Int,
    columnCount: Int,
    rowCount: Int,
    density: androidx.compose.ui.unit.Density
): TextUnit {
    if (widthPx <= 0 || heightPx <= 0) return 4.sp

    // Using a 94% safety margin to prevent edge clipping
    val safeWidthPx = widthPx * 0.94f
    val safeHeightPx = heightPx * 0.94f

    val widthBasedSp = with(density) {
        val cellWidthPx = safeWidthPx / columnCount
        (cellWidthPx / 0.68f).toSp()
    }

    // 1.08f is a constant that accounts for standard monospace line height
    val heightBasedSp = with(density) {
        val cellHeightPx = safeHeightPx / rowCount
        (cellHeightPx / 1.08f).toSp()
    }
    /*
    We take the smaller of the two sizes to ensure it fits in both dimensions.
    coerceIn prevents the font from becoming microscopic or too large.
    */
    return min(widthBasedSp.value, heightBasedSp.value).coerceIn(2.5f, 18f).sp
}

@Composable
fun InteractiveInfoChip(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141920))
            .border(1.dp, Color(0xFF242B34), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF8C97A6),
            fontSize = 12.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "▼",
                color = Color(0xFF8C97A6),
                fontSize = 12.sp
            )
        }
    }
}