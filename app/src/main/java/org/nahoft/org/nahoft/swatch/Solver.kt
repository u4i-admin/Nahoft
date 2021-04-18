package org.nahoft.org.nahoft.swatch;

import android.graphics.Bitmap
import android.graphics.Color
import org.nahoft.swatch.*

public class Solver(val coverImageBitmap: Bitmap, var messageARules: Array<Rule>, var messageBRules: Array<Rule>)
{
    var checkedPixelsA: MutableSet<Int> = mutableSetOf()
    var checkedPixelsB: MutableSet<Int> = mutableSetOf()
    var canBrighten: MutableSet<Int> = mutableSetOf()
    var canDarken: MutableSet<Int> = mutableSetOf()
    var conflicted: MutableSet<Int> = mutableSetOf()

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
            for (pixel in rule.patch0.pixels) {
                when (rule.constraint) {
                    EncoderConstraint.GREATER -> canBrighten.add(pixel.index)
                    EncoderConstraint.LESS -> canDarken.add(pixel.index)
                }

                checkedPixelsA.add(pixel.index)
            }

            // Now patch1
            // messageA patch1 (second patch)
            for (pixel in rule.patch1.pixels)
            {
                if (checkedPixelsA.contains(pixel.index)) {
                    val duplicatedIndex = pixel.index
                    print("Error, duplicate pixel $duplicatedIndex")
                    val duplicatedIndexIndex = checkedPixelsA.toList().indexOf(duplicatedIndex)
                    print("Error, duplicate pixel $duplicatedIndexIndex")
                }

                // Note that the constraint is inverted for patch1.
                when (rule.constraint) {
                    EncoderConstraint.LESS -> canBrighten.add(pixel.index)
                    EncoderConstraint.GREATER -> canDarken.add(pixel.index)
                }
            }
        }

        for (rule in messageBRules)
        {
            for (pixel in rule.patch0.pixels)
            {
                checkedPixelsA.add(pixel.index)

                if (rule.constraint == EncoderConstraint.GREATER) {
                    if (canBrighten.contains(pixel.index)) {
                        // Compatible constraints
                        continue
                    } else if(canDarken.contains(pixel.index)) {
                        // Conflict
                        conflicted.add(pixel.index)
                    } else {
                        print("Weird stray pixel. Suspicious.")
                    }
                } else {
                    if (canDarken.contains(pixel.index)) {
                        // Compatible constraints
                        continue
                    } else if (canBrighten.contains(pixel.index)) {
                        // Conflict
                        conflicted.add(pixel.index)
                    } else {
                        print("Weird stray pixel. Suspicious.")
                    }
                }
            }

            for (pixel in rule.patch1.pixels)
            {
                if (checkedPixelsB.contains(pixel.index)) {
                    print("Error, duplicate pixel")
                }

                // Note that the constraint is inverted for patch1.
                if (rule.constraint == EncoderConstraint.LESS) {
                    if (canBrighten.contains(pixel.index)) {
                        // Compatible constraints
                        continue
                    } else if(canDarken.contains(pixel.index)) {
                        // Conflict
                        conflicted.add(pixel.index)
                    } else {
                        print("Weird stray pixel. Suspicious.")
                    }
                } else {
                    if (canDarken.contains(pixel.index)) {
                        // Compatible constraints
                        continue
                    } else if (canBrighten.contains(pixel.index)) {
                        // Conflict
                        conflicted.add(pixel.index)
                    } else {
                        print("Weird stray pixel. Suspicious.")
                    }
                }
            }
        }

        canBrighten.removeAll(conflicted)
        canDarken.removeAll(conflicted)
    }

    fun constrain(): Boolean
    {
        val knownBadPixel = messageARules[0].patch0.pixels[5]
        val knownBadPixelColor0 = knownBadPixel.color
        var forbiddenSet: MutableSet<Pixel> = mutableSetOf()
        var forbiddenList: MutableList<Pixel> = mutableListOf()
        for ((index, rule) in messageARules.withIndex())
        {
            println("A: $index / $messageARules.size")
            val success = rule.constrain(canBrighten, canDarken)
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
            forbiddenSet.add(pixel)
            forbiddenList.add(pixel)
        }

        for (rule in messageBRules)
        {
            val success = rule.constrain(canBrighten, canDarken)
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

//        if (forbiddenBrightnessBefore != forbiddenBrightnessAfter) {
//            print("Failure")
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

//            for (index in 0..forbiddenColorListBefore.size) {
//                val before = forbiddenColorListBefore[index]
//                val after = forbiddenColorListAfter[index]
//                if (before != after) {
//                    print("Mismatch at $index")
//                }
//            }
//        }

        return true
    }
}
