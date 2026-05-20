package rs.nikolaivanovic.imagetoasciiconverter.utils

import android.graphics.Bitmap
import kotlin.math.pow
import androidx.core.graphics.get
import androidx.core.graphics.scale

class AsciiConverter {

    companion object {
        // Standard ASCII characters from darkest to lightest
        private const val ASCII_CHARS = "@%#*+=-:. "

        // Extended ASCII with more granularity
        private const val EXTENDED_ASCII_CHARS = "@@@%%###***+++===---:::...   "

        // Ultra-extended with maximum detail - includes many shades
        private const val ULTRA_ASCII_CHARS =
            "@@@%%%###***+++===---:::;;;'''\"\"\"....,,,,,    "
    }

    fun convertToAscii(
        bitmap: Bitmap,
        width: Int = 100,
        height: Int = 50,
        quality: Quality = Quality.ULTRA
    ): String {
        // Resize bitmap for ASCII art
        val resized = bitmap.scale(width, height)

        val chars = when (quality) {
            Quality.LOW -> ASCII_CHARS
            Quality.MEDIUM -> EXTENDED_ASCII_CHARS
            Quality.ULTRA -> ULTRA_ASCII_CHARS
        }

        val asciiArt = StringBuilder()

        // Calculate brightness range for better contrast
        val (minBrightness, maxBrightness) = calculateBrightnessRange(resized)
        val brightnessRange = maxBrightness - minBrightness

        for (y in 0 until resized.height) {
            for (x in 0 until resized.width) {
                val pixel = resized[x, y]

                // Extract RGB components
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF

                // Calculate brightness with enhanced luminance formula
                val brightness = calculateEnhancedBrightness(red, green, blue)

                // Normalize brightness to full range for better contrast
                val normalizedBrightness = if (brightnessRange > 0.1) {
                    (brightness - minBrightness) / brightnessRange
                } else {
                    brightness / 255.0
                }

                // Clamp to 0-1 range
                val clampedBrightness = normalizedBrightness.coerceIn(0.0, 1.0)

                // Apply gamma correction for better light sensitivity
                val gammaCorrection = clampedBrightness.pow(0.9)

                // Map brightness to ASCII character
                val charIndex = (gammaCorrection * (chars.length - 1)).toInt()
                asciiArt.append(chars[charIndex])
            }
            asciiArt.append("\n")
        }

        return asciiArt.toString()
    }

    fun convertToAsciiFromPath(
        imagePath: String,
        width: Int = 100,
        height: Int = 50,
        quality: Quality = Quality.ULTRA
    ): String {
        val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
            ?: return "Error: Could not load image"

        return convertToAscii(bitmap, width, height, quality)
    }

    private fun calculateEnhancedBrightness(red: Int, green: Int, blue: Int): Double {
        // Enhanced luminance calculation with better sensitivity
        // Weights adjusted for better perceptual brightness
        return (0.2126 * red + 0.7152 * green + 0.0722 * blue)
    }

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
        LOW,      // 10 characters - basic
        MEDIUM,   // 30 characters - good
        ULTRA     // 47 characters - maximum detail
    }
}