package rs.nikolaivanovic.imagetoasciiconverter.utils

import android.graphics.Bitmap

class AsciiConverter {

    companion object {
        // ASCII characters from darkest to lightest
        private const val ASCII_CHARS = "@%#*+=-:. "
    }

    fun convertToAscii(
        bitmap: Bitmap,
        width: Int = 100,
        height: Int = 50
    ): String {
        // Resize bitmap for ASCII art
        val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)

        val asciiArt = StringBuilder()

        for (y in 0 until resized.height) {
            for (x in 0 until resized.width) {
                val pixel = resized.getPixel(x, y)

                // Extract RGB components
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF

                // Calculate brightness (luminance)
                val brightness = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0

                // Map brightness to ASCII character
                val charIndex = (brightness * (ASCII_CHARS.length - 1)).toInt()
                asciiArt.append(ASCII_CHARS[charIndex])
            }
            asciiArt.append("\n")
        }

        return asciiArt.toString()
    }

    fun convertToAsciiFromPath(
        imagePath: String,
        width: Int = 100,
        height: Int = 50
    ): String {
        val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
            ?: return "Error: Could not load image"

        return convertToAscii(bitmap, width, height)
    }
}