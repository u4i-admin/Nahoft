package org.nahoft.org.nahoft.swatch

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.absoluteValue

class Rule(val ruleIndex: Int, val patchSize: Int, val constraint: EncoderConstraint, var bitmap: MappedBitmap) {
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

    fun check(): Boolean {
        if (!patch0.brightnessCheck()) {return false}
        if (!patch1.brightnessCheck()) {return false}

        val checked = when (constraint) {
            EncoderConstraint.GREATER -> patch0.brightness > patch1.brightness
            EncoderConstraint.LESS -> patch0.brightness < patch1.brightness
        }

        return checked
    }

    fun removeConflictedPixels(directions: Bitmap): Boolean {
        if (!patch0.removeConflictedPixels(constraint, directions)) {
            print("Error removing conflicts from a patch, all points were in conflict.")
            return false
        }

        // Note that the constraint is inverted for patch1.
        if (!patch1.removeConflictedPixels(constraint.invert(), directions)) {
            print("Error removing conflicts from a patch, all points were in conflict.")
            return false
        }

        return true
    }

    fun constrain(directions: Bitmap, forbiddenSet: Set<MappedPixel> = emptySet()): Boolean {
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

    fun modifyBrightness(forbiddenSet: Set<MappedPixel> = emptySet()): Boolean {
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

            val patch0Change = patch0.modifyBrightness(patch0Direction, patchBrightnessGap, forbiddenSet)
            val patch1Change = patch1.modifyBrightness(patch1Direction, patchBrightnessGap, forbiddenSet)

            if (patch0Change == 0 && patch1Change == 0) {
                // Failure. Did not achieve target brightness gap between patches and modifying patch brightness failed.
                return false
            }
        }

        // Success. Achieved target brightness gap between patches, as indicated by exiting the main loop.
        return true
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

fun EncoderConstraint.invert(): EncoderConstraint {
    when (this) {
        EncoderConstraint.GREATER -> return EncoderConstraint.LESS
        EncoderConstraint.LESS -> return EncoderConstraint.GREATER
    }
}