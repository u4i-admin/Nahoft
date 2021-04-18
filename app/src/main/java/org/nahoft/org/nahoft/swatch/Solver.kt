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
                // FIXME - remove
                if (pixel.mappedX == 242 && pixel.mappedY == 150) {
                    print("Known bad pixel")
                }
                conflicts.setPixel(pixel.mappedX, pixel.mappedY, rule.constraint.getPixelColor())
            }

            // Now patch1
            // messageA patch1 (second patch)
            for (pixel in rule.patch1.pixels)
            {
                // FIXME - remove
                if (pixel.mappedX == 242 && pixel.mappedY == 150) {
                    print("Known bad pixel")
                }
                // Note that the constraint is inverted for patch1.
                conflicts.setPixel(pixel.mappedX, pixel.mappedY, rule.constraint.invert().getPixelColor())
            }
        }

        for (rule in messageBRules)
        {
            for (pixel in rule.patch0.pixels)
            {
                // FIXME - remove
                if (pixel.mappedX == 242 && pixel.mappedY == 150) {
                    print("Known bad pixel")
                }

                val oldColor = conflicts.getPixel(pixel.mappedX, pixel.mappedY)

                val newColor = getPixelColor(oldColor, rule.constraint)
                conflicts.setPixel(pixel.mappedX, pixel.mappedY, newColor)
            }

            for (pixel in rule.patch1.pixels)
            {
                // FIXME - remove
                if (pixel.mappedX == 242 && pixel.mappedY == 150) {
                    print("Known bad pixel")
                }

                val oldColor = conflicts.getPixel(pixel.mappedX, pixel.mappedY)

                // Note that the constraint is inverted for patch1.
                val newColor = getPixelColor(oldColor, rule.constraint.invert())
                conflicts.setPixel(pixel.mappedX, pixel.mappedY, newColor)
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
        val knownBadPixel = messageARules[0].patch0.pixels[5]
        val knownBadPixelColor0 = knownBadPixel.color
        var forbiddenSet: MutableSet<MappedPixel> = mutableSetOf()
        for ((index, rule) in messageARules.withIndex())
        {
            println("A: $index / $messageARules.size")
            val success = rule.constrain(conflicts)
            if (!success) {
                return false
            }

            // FIXME - expensive, for debugging purposes, remove
            if (!rule.check()) {
                print("Failure, rule check did not pass")
                return false
            }

            val checkRule = Rule(rule.ruleIndex, rule.patchSize, rule.constraint, rule.bitmap)
            if (checkRule.patch0.brightness != rule.patch0.brightness) {
                print("Brightness mismatch 0")
                return false
            }

            if (checkRule.patch1.brightness != rule.patch1.brightness) {
                print("Brightness mismatch 1")
                return false
            }
        }

        val knownBadPixelColor1 = knownBadPixel.color

        var forbiddenBrightnessBefore = 0
        var forbiddenBrightnessListBefore = emptyList<Int>().toMutableList()
        var forbiddenColorListBefore = emptyList<Int>().toMutableList()
        for (pixel in messageARules[0].patch0.pixels) {
            val pixelBrightness = pixel.brightness()
            forbiddenBrightnessBefore += pixelBrightness
            forbiddenBrightnessListBefore.add(pixelBrightness)
            forbiddenColorListBefore.add(pixel.color)
            forbiddenSet.add(pixel.mapped)
        }

        for (rule in messageBRules)
        {
            val success = rule.constrain(conflicts, forbiddenSet)
            if (!success) {
                return false
            }
        }

        val knownBadPixelColor2 = knownBadPixel.color

        var forbiddenBrightnessAfter = 0
        var forbiddenBrightnessListAfter = emptyList<Int>().toMutableList()
        var forbiddenColorListAfter = emptyList<Int>().toMutableList()
        for (pixel in messageARules[0].patch0.pixels) {
            val pixelBrightness = pixel.brightness()
            forbiddenBrightnessAfter += pixelBrightness
            forbiddenBrightnessListAfter.add(pixelBrightness)
            forbiddenColorListAfter.add(pixel.color)
        }

        if (forbiddenBrightnessBefore != forbiddenBrightnessAfter) {
            print("Failure")
//            for (index in 0..forbiddenBrightnessListBefore.size) {
//                val before = forbiddenBrightnessListBefore[index]
//                val after = forbiddenBrightnessListAfter[index]
//                if (before != after) {
//                    print("Mismatch at $index")
//                    val badPixel = messageARules[0].patch0.pixels[index]
//                    println(badPixel)
//                    val badPixelColor = badPixel.color
//                    println(badPixelColor)
//                }
//            }

            for (index in 0..forbiddenColorListBefore.size) {
                val before = forbiddenColorListBefore[index]
                val after = forbiddenColorListAfter[index]
                if (before != after) {
                    print("Mismatch at $index")
                }
            }
        }

        // FIXME - remove
        val rule = messageARules[0]
        val checkRule = Rule(rule.ruleIndex, rule.patchSize, rule.constraint, rule.bitmap)
        if (checkRule.patch0.brightness != rule.patch0.brightness) {
            print("Brightness mismatch 0")
            return false
        }

        // FIXME - remove
        if (checkRule.patch1.brightness != rule.patch1.brightness) {
            print("Brightness mismatch 1")
            return false
        }

        return true
    }
}
