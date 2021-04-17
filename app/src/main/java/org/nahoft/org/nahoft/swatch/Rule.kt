package org.nahoft.org.nahoft.swatch

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.absoluteValue

class Rule(val ruleIndex: Int, patchSize: Int, val constraint: EncoderConstraint, var bitmap: MappedBitmap) {
    var patch0: Patch
    var patch1: Patch

    var valid: Boolean
    var brightnessGap: Int

    init {
        patch0 = Patch(ruleIndex * 2, patchSize, bitmap)
        patch1 = Patch((ruleIndex * 2) + 1, patchSize, bitmap)

        valid = when (constraint) {
            EncoderConstraint.GREATER -> patch0.brightness > patch1.brightness
            EncoderConstraint.LESS -> patch0.brightness < patch1.brightness
        }

        if (valid) {
            brightnessGap = 0
        } else if (patch0.brightness == patch1.brightness) {
            brightnessGap = 1
        } else {
            // Neither valid nor equal, the validity condition must be inverted.
            brightnessGap = (patch0.brightness - patch1.brightness).absoluteValue + 1
        }
    }

    // Does this pair of patches meet the constraint
    fun validate(): Boolean {
        valid = when (constraint) {
            EncoderConstraint.GREATER -> patch0.brightness > patch1.brightness
            EncoderConstraint.LESS -> patch0.brightness < patch1.brightness
        }

        if (valid) {
            brightnessGap = 0
        } else if (patch0.brightness == patch1.brightness) {
            brightnessGap = 1
        } else {
            // Neither valid nor equal, the validity condition must be inverted.
            brightnessGap = (patch0.brightness - patch1.brightness).absoluteValue + 1
        }

        return valid
    }

    fun removeConflictedPixels(directions: Bitmap): Boolean {
        val patch0GoodPixels = removeConflictedPixelsForPatch(patch0, directions)
        if (patch0GoodPixels.isEmpty()) {
            print("Error removing conflicts from a patch, all points were in conflict.")
            return false
        }
        patch0.pointsToModify = patch0GoodPixels

        val patch1GoodPixels = removeConflictedPixelsForPatch(patch1, directions)
        if (patch1GoodPixels.isEmpty()) {
            print("Error removing conflicts from a patch, all points were in conflict.")
            return false
        }
        patch1.pointsToModify = patch1GoodPixels

        return true
    }

    private fun removeConflictedPixelsForPatch(patch: Patch, directions: Bitmap): MutableList<Pixel> {
        var goodPixels = patch.pixels.toMutableList()

        for (point in patch.pixels) {
            val pixelColor = directions.getPixel(point.x, point.y)

            val goodColor = constraint.getPixelColor()

            if (pixelColor != goodColor) {
                goodPixels.remove(point)
            }
        }

        return goodPixels
    }

    fun constrain(directions: Bitmap): Boolean {
        // If the working bitmap is already valid, return it
        if (valid) {
            return true
        }

        // If there is an error removing conflicted pixels, return null
        val success = removeConflictedPixels(directions)
        if (!success) {
            return false
        }

        return modifyBrightness()
    }

    fun modifyBrightness(): Boolean {
        while (!validate()) {
            // Not valid, but no brightness gap? Weird, give up.
            if (brightnessGap == 0) {
                return false
            }

            // Divide the necessary work between the two patches
            var patchBrightnessGap = brightnessGap / 2
            if (patchBrightnessGap == 0) {
                // Deal with rounding to 0
                patchBrightnessGap = 1
            }

            val patch0Direction = when (constraint) {
                EncoderConstraint.GREATER -> EncoderConstraint.GREATER
                EncoderConstraint.LESS    -> EncoderConstraint.LESS
            }

            val patch1Direction = when (constraint) {
                EncoderConstraint.GREATER -> EncoderConstraint.LESS
                EncoderConstraint.LESS    -> EncoderConstraint.GREATER
            }

            modifyPatchBrightness(patch0, patch0Direction, patchBrightnessGap)
            modifyPatchBrightness(patch1, patch1Direction, patchBrightnessGap)
        }

        return true
    }

    fun modifyPatchBrightness(patch: Patch, direction: EncoderConstraint, patchBrightnessGap: Int): Boolean {
        if (patchBrightnessGap == 0) {
            // Success!
            return true
        }

        var patchBrightnessDifferencePerPixel = patchBrightnessGap / patch.pointsToModify.size
        if (patchBrightnessDifferencePerPixel == 0) {
            // Deal with rounding to 0
            patchBrightnessDifferencePerPixel = 1
        }

        var unchangeablePixels = emptyList<Pixel>().toMutableList()

        for (point in patch.pointsToModify) {
            // Picks a random pixel from the patch changes the color of the pixel to be darker
            val changeInBrightness = when (direction) {
                EncoderConstraint.GREATER -> point.brighten(patchBrightnessDifferencePerPixel)
                EncoderConstraint.LESS -> point.darken(patchBrightnessDifferencePerPixel)
            }

            if (changeInBrightness == 0) {
                unchangeablePixels.add(point)
            } else if (validate()) {
                return true
            }
        }

        for (unPixel in unchangeablePixels) {
            patch.pointsToModify.remove(unPixel)
        }

        return false
    }
}

enum class EncoderConstraint(val constraint: Int)
{
    GREATER(1),
    LESS(-1)
}

fun EncoderConstraint.getPixelColor(): Int {
    return when (this) {
        EncoderConstraint.GREATER -> Color.GREEN
        EncoderConstraint.LESS -> Color.BLUE
    }
}
