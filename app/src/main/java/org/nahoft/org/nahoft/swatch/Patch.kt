package org.nahoft.org.nahoft.swatch

import android.graphics.Bitmap
import android.graphics.Color

class Patch(val patchIndex: Int, val size: Int, var bitmap: MappedBitmap)
{
    var pixels: Array<Pixel>
    var pixelsToModify = emptyList<Pixel>().toMutableList()
    var brightness: Int

    init {
        brightness = 0

        pixels = Array<Pixel>(size) { index ->
            val pixelIndex = (patchIndex * size) + index
            val mappedPixelIndex = bitmap.mapping[pixelIndex]
            val pixel = Pixel(mappedPixelIndex, bitmap)
            brightness += pixel.brightness()
            pixel
        }
        pixelsToModify = pixels.toMutableList()
    }

    // Returns the actual change in brightness achieved
    fun modifyBrightness(direction: EncoderConstraint, targetChangeInBrightness: Int): Int {
        var achievedChangeInBrightness = 0
        if (targetChangeInBrightness == 0) {
            // Success! Achieved target change in brighhtness of 0.
            return achievedChangeInBrightness
        }

        var patchBrightnessDifferencePerPixel = targetChangeInBrightness / pixelsToModify.size
        if (patchBrightnessDifferencePerPixel == 0) {
            // Deal with rounding to 0
            patchBrightnessDifferencePerPixel = 1
        }

        while (achievedChangeInBrightness < targetChangeInBrightness) {
            var unchangeablePixels = emptyList<Pixel>().toMutableList()

            if (pixelsToModify.isEmpty())
            {
                // Failure or partial success. Did not achieve target change in brightness and ran out of pixels to modify.
                return achievedChangeInBrightness
            }

            // Iterate though all of the pixels which we are allowed to change.
            for ((index, pixel) in pixelsToModify.withIndex()) {
                print(index)
                // Picks a random pixel from the patch changes the color of the pixel to be darker
                val changeInBrightness = when (direction) {
                    EncoderConstraint.GREATER -> pixel.brighten(patchBrightnessDifferencePerPixel)
                    EncoderConstraint.LESS -> pixel.darken(patchBrightnessDifferencePerPixel)
                }

                if (changeInBrightness == 0) {
                    unchangeablePixels.add(pixel)
                } else {
                    achievedChangeInBrightness += changeInBrightness
                    when (direction) {
                        EncoderConstraint.GREATER -> brightness += changeInBrightness
                        EncoderConstraint.LESS -> brightness -= changeInBrightness
                    }

                    // Check if we can escape early from iterating through all of the pixels.
                    if (achievedChangeInBrightness >= targetChangeInBrightness) {
                        return achievedChangeInBrightness
                    }
                }
            }

            // Remove all of the pixel that we can't change to save time through the next iteration.
            for (unPixel in unchangeablePixels) {
                pixelsToModify.remove(unPixel)
            }
        }

        // Success! Achieved targeted change in brightness, as indicated by leaving the main loop.
        return achievedChangeInBrightness
    }
}
