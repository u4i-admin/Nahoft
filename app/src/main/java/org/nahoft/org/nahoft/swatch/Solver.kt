package org.nahoft.org.nahoft.swatch;

import android.graphics.Bitmap
import android.graphics.Color
import org.nahoft.swatch.*

public class Solver(val coverImageBitmap: Bitmap, var messageARules: Array<Rule>, var messageBRules: Array<Rule>)
{
    var conflicts: Bitmap = coverImageBitmap.copy(Bitmap.Config.ARGB_8888, true)

    fun solve(): Boolean
    {
        recordConflicts()
        return constrain()
    }

    // Conflicts happen when overlapping patches disagree about whether a pixel should be lightened or darkened (one says lighten the other says darken)
    // When this happens, we leave that pixel alone (neither lighten nor darken)
    // This leaves a subset of pixels in each patch that will be modified.
    fun recordConflicts()
    {
        for (rule in messageARules)
        {
            // For each pixel in this rule's patch0
            // check for a conflict, and add it to conflicts bitmap if one is found
            // (bitmap is used only because it is a convenient data structure)
            for (pixel in rule.patch0.pixels)
            {
                conflicts.setPixel(pixel.x, pixel.y, rule.constraint.getPixelColor())
            }

            // Now patch1
            // messageA patch1 (second patch)
            for (pixel in rule.patch1.pixels)
            {
                conflicts.setPixel(pixel.x, pixel.y, rule.constraint.getPixelColor())
            }
        }

        for (rule in messageBRules)
        {
            for (pixel in rule.patch0.pixels)
            {
                val oldColor = conflicts.getPixel(pixel.x, pixel.y)
                val newColor = getPixelColor(oldColor, rule.constraint)
                conflicts.setPixel(pixel.x, pixel.y, newColor)
            }

            for (pixel in rule.patch1.pixels)
            {
                val oldColor = conflicts.getPixel(pixel.x, pixel.y)
                val newColor = getPixelColor(oldColor, rule.constraint)
                conflicts.setPixel(pixel.x, pixel.y, newColor)
            }
        }
    }

    fun getPixelColor(oldPixelColor: Int, constraint: EncoderConstraint): Int {
        if (oldPixelColor == constraint.getPixelColor()) {
            return oldPixelColor
        } else {
            return Color.RED
        }
    }

    fun constrain(): Boolean
    {
        for (rule in messageARules)
        {
            val success = rule.constrain(conflicts)
            if (!success) {
                return false
            }
        }

        for (rule in messageBRules)
        {
            val success = rule.constrain(conflicts)
            if (!success) {
                return false
            }
        }

        return true
    }
}
