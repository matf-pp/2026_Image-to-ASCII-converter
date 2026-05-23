package rs.nikolaivanovic.imagetoasciiconverter.utils

import android.graphics.Bitmap
import kotlin.math.pow
import androidx.core.graphics.get
import androidx.core.graphics.scale

/*
Utility class for converting Bitmaps into ASCII art representations
*/
class AsciiConverter {


    // Holds a character and its corresponding color from the original image
    data class ColoredChar(val char: Char, val color: Int)

    companion object {
        // Standard ASCII characters from darkest to lightest
        private const val ASCII_CHARS = " .:-=+*#%@"

        // Extended ASCII with more granularity
        private const val EXTENDED_ASCII_CHARS = "   ...:::---===+++***###%%@@@"

        // Ultra-extended with maximum detail - includes many shades
        private const val ULTRA_ASCII_CHARS = "  _.,-=+:;cba!?0123456789\$W#@"
    }

    /*
    Converts a bitmap into a string of ASCII characters based on pixel luminance.
    */
    fun convertToAscii(
        bitmap: Bitmap,
        width: Int = 64,
        quality: Quality = Quality.ULTRA
    ): String {
        val aspectRatio = bitmap.height.toDouble() / bitmap.width.toDouble()
        val height = (width * aspectRatio * 0.5).toInt().coerceAtLeast(1)

        val resized = bitmap.scale(width, height)

        val chars = when (quality) {
            Quality.LOW -> ASCII_CHARS
            Quality.MEDIUM -> EXTENDED_ASCII_CHARS
            Quality.ULTRA -> ULTRA_ASCII_CHARS
        }

        val asciiArt = StringBuilder()

        val (minBrightness, maxBrightness) = calculateBrightnessRange(resized)
        val brightnessRange = maxBrightness - minBrightness

        for (y in 0 until resized.height) {
            for (x in 0 until resized.width) {
                val pixel = resized[x, y]

                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF

                val brightness = calculateEnhancedBrightness(red, green, blue)

                val normalizedBrightness = if (brightnessRange > 0.1) {
                    (brightness - minBrightness) / brightnessRange
                } else {
                    brightness / 255.0
                }

                val clampedBrightness = normalizedBrightness.coerceIn(0.0, 1.0)
                val gammaCorrection = clampedBrightness.pow(0.9)

                val charIndex = (gammaCorrection * (chars.length - 1)).toInt()
                asciiArt.append(chars[charIndex])
            }
            asciiArt.append("\n")
        }

        return asciiArt.toString()
    }
    
    //Helper to load an image from a file path and convert it to ASCII
    fun convertToAsciiFromPath(
        imagePath: String,
        width: Int = 64,
        quality: Quality = Quality.ULTRA
    ): String {
        val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
            ?: return "Error: Could not load image"

        return convertToAscii(bitmap, width, quality)
    }

    /*
    Algorithm to convert a Bitmap to a list of ColoredChar's
    Preserves original pixel colors for each ASCII character
    */
    fun convertToColoredAscii(
        bitmap: Bitmap,
        width: Int = 64,
        quality: Quality = Quality.ULTRA
    ): List<ColoredChar> {
        val aspectRatio = bitmap.height.toDouble() / bitmap.width.toDouble()
        val height = (width * aspectRatio * 0.5).toInt().coerceAtLeast(1)
        val resized = bitmap.scale(width, height)

        val chars = when (quality) {
            Quality.LOW -> ASCII_CHARS
            Quality.MEDIUM -> EXTENDED_ASCII_CHARS
            Quality.ULTRA -> ULTRA_ASCII_CHARS
        }

        val result = mutableListOf<ColoredChar>()
        val (minBrightness, maxBrightness) = calculateBrightnessRange(resized)
        val brightnessRange = maxBrightness - minBrightness

        for (y in 0 until resized.height) {
            for (x in 0 until resized.width) {
                val pixel = resized[x, y]
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                val brightness = calculateEnhancedBrightness(red, green, blue)

                val normalizedBrightness = if (brightnessRange > 0.1) {
                    (brightness - minBrightness) / brightnessRange
                } else {
                    brightness / 255.0
                }
                val clampedBrightness = normalizedBrightness.coerceIn(0.0, 1.0)
                val gammaCorrection = clampedBrightness.pow(0.9)
                val charIndex = (gammaCorrection * (chars.length - 1)).toInt()

                result.add(ColoredChar(chars[charIndex], pixel))
            }
            result.add(ColoredChar('\n', 0))
        }
        return result
    }

    /*
    Loads an image from path and converts it to colored ASCII data
    */
    fun convertToColoredAsciiFromPath(
        imagePath: String,
        width: Int = 64,
        quality: Quality = Quality.ULTRA
    ): List<ColoredChar> {
        val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
            ?: return listOf(ColoredChar('E', 0))

        return convertToColoredAscii(bitmap, width, quality)
    }

    private fun calculateEnhancedBrightness(red: Int, green: Int, blue: Int): Double {
        // Weights adjusted for better perceptual brightness
        return (0.2126 * red + 0.7152 * green + 0.0722 * blue)
    }


    //Analyzes the image to find the darkest and brightest pixels for contrast normalization
    private fun calculateBrightnessRange(bitmap: Bitmap): Pair<Double, Double> {
        var minBrightness = Double.MAX_VALUE
        var maxBrightness = 0.0

        // Sample pixels for faster calculation on large images
        val sampleRate = kotlin.math.max(1, bitmap.width / 50)

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap[x, y]

                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF

                val brightness = calculateEnhancedBrightness(red, green, blue)

                if (brightness < minBrightness) minBrightness = brightness
                if (brightness > maxBrightness) maxBrightness = brightness

                x += sampleRate
            }
            y += sampleRate
        }

        return Pair(minBrightness, maxBrightness)
    }

    enum class Quality {
        LOW,
        MEDIUM,
        ULTRA
    }
}