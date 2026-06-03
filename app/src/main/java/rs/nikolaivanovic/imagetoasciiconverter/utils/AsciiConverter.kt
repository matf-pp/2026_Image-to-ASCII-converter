package rs.nikolaivanovic.imagetoasciiconverter.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.get
import androidx.core.graphics.scale
import kotlin.math.max
import kotlin.math.pow

/*
Utility class for converting Bitmaps into ASCII art representations.
Refactored to use a unified processing engine for both plain and colored output.
*/
class AsciiConverter {

    // Holds a character and its corresponding color from the original image
    data class ColoredChar(val char: Char, val color: Int)

    // Represents the output of an ASCII conversion, either plain text or colored characters.
    sealed class Result {
        data class Plain(val text: String) : Result()
        data class Colored(val text: String, val coloredChars: List<ColoredChar>) : Result()
    }

    companion object {
        private const val ASCII_CHARS = "\u00A0.:-+=*#%@"
        private const val EXTENDED_ASCII_CHARS = "\u00A0\u00A0\u00A0...:::---===+++***###%%@@@"
        private const val ULTRA_ASCII_CHARS = "\u00A0\u00A0_.,-=:;cba!?0123456789\$W#@"
    }

    // Converts a bitmap into ASCII art, returning either plain text or a list of colored characters based on the flag.
    fun convert(
        bitmap: Bitmap,
        width: Int = 64,
        quality: Quality = Quality.ULTRA,
        isColorEnabled: Boolean = false
    ): Result {
        val plainText = StringBuilder()
        val coloredChars = if (isColorEnabled) mutableListOf<ColoredChar>() else null

        processImage(
            bitmap, width, quality,
            onPixel = { char, color ->
                plainText.append(char)
                coloredChars?.add(ColoredChar(char, color))
            },
            onRowEnd = {
                plainText.append("\n")
                coloredChars?.add(ColoredChar('\n', 0))
            }
        )

        return if (coloredChars != null) {
            Result.Colored(plainText.toString(), coloredChars)
        } else {
            Result.Plain(plainText.toString())
        }
    }

    // Decodes an image from a path and converts it to ASCII art using the unified conversion function.
    fun convertFromPath(
        imagePath: String,
        width: Int = 64,
        quality: Quality = Quality.ULTRA,
        isColorEnabled: Boolean = false
    ): Result {
        return loadBitmap(imagePath)?.let {
            convert(it, width, quality, isColorEnabled)
        } ?: Result.Plain("Error: Could not load image")
    }

    // Resizes the image and orchestrates the conversion by calling callbacks for each mapped pixel and row end.
    private inline fun processImage(
        bitmap: Bitmap,
        width: Int,
        quality: Quality,
        onPixel: (Char, Int) -> Unit,
        onRowEnd: () -> Unit
    ) {
        val aspectRatio = bitmap.height.toDouble() / bitmap.width.toDouble()
        val height = (width * aspectRatio * 0.5).toInt().coerceAtLeast(1)

        val resized = bitmap.scale(width, height)
        val chars = getCharSet(quality)

        val (minBrightness, maxBrightness) = calculateBrightnessRange(resized)
        val brightnessRange = maxBrightness - minBrightness

        for (y in 0 until resized.height) {
            for (x in 0 until resized.width) {
                val pixel = resized[x, y]
                val char = mapPixelToChar(pixel, chars, minBrightness, brightnessRange)
                onPixel(char, pixel)
            }
            onRowEnd()
        }
    }

    // Calculates a pixel's brightness and maps it to a character from the selected set using gamma correction.
    private fun mapPixelToChar(pixel: Int, chars: String, minBrightness: Double, range: Double): Char {
        val brightness = calculateBrightness(pixel)

        val normalizedBrightness = if (range > 0.1) {
            (brightness - minBrightness) / range
        } else {
            brightness / 255.0
        }

        val clampedBrightness = normalizedBrightness.coerceIn(0.0, 1.0)
        val gammaCorrection = clampedBrightness.pow(0.9)

        val charIndex = (gammaCorrection * (chars.length - 1)).toInt()
        return chars[charIndex]
    }

    // Calculates perceptual brightness of a pixel using weighted red, green, and blue components.
    private fun calculateBrightness(pixel: Int): Double {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        // Weights adjusted for better perceptual brightness
        return (0.2126 * red + 0.7152 * green + 0.0722 * blue)
    }

    // Scans sampled pixels of the image to find the minimum and maximum brightness values for normalization.
    private fun calculateBrightnessRange(bitmap: Bitmap): Pair<Double, Double> {
        var minBrightness = Double.MAX_VALUE
        var maxBrightness = 0.0

        // Sample pixels for faster calculation on large images
        val sampleRate = max(1, bitmap.width / 50)

        for (y in 0 until bitmap.height step sampleRate) {
            for (x in 0 until bitmap.width step sampleRate) {
                val brightness = calculateBrightness(bitmap[x, y])
                if (brightness < minBrightness) minBrightness = brightness
                if (brightness > maxBrightness) maxBrightness = brightness
            }
        }

        return Pair(minBrightness, maxBrightness)
    }

    // Loads a bitmap object from the specified file system path.
    private fun loadBitmap(path: String): Bitmap? = BitmapFactory.decodeFile(path)

    // Returns the appropriate string of ASCII characters based on the requested quality level.
    private fun getCharSet(quality: Quality) = when (quality) {
        Quality.LOW -> ASCII_CHARS
        Quality.MEDIUM -> EXTENDED_ASCII_CHARS
        Quality.ULTRA -> ULTRA_ASCII_CHARS
    }

    enum class Quality {
        LOW,
        MEDIUM,
        ULTRA
    }
}
