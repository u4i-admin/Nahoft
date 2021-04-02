package org.nahoft.org.nahoft.swatch;

import android.graphics.Bitmap
import org.nahoft.swatch.Rule;

public class Solver(val coverImageBitmap: Bitmap, var messageARules: Array<Rule>, var messageBRules: Array<Rule>)
{
    var conflicts: Bitmap = coverImageBitmap.copy(Bitmap.Config.ARGB_8888, true)
    var solution: Bitmap = coverImageBitmap.copy(Bitmap.Config.ARGB_8888, true)

    fun solve(): Bitmap
    {
        recordConflicts()
        constrain()

        return solution
    }

    // Conflicts happen when overlapping patches disagree about whether a pixel should be lightened or darkened (one say lighten the other says darken)
    // When this happens, we leave that pixel alone (neither lighten nor darken)
    // This leaves a subset of pixels in each patch that will be modified.
    // TODO: We may not need to know lighten or darken, possibly just conflict or no conflict
    fun recordConflicts()
    {
        // Every(ish) Pixel is subject to two constraints
        // In a patch from messageA and a patch from messageB

        // Rules (2 patches and a constraint) for message1

        // This first loop will add pixels to the conflicts bitmap
        // Each pixel will be either a 1, or -1
        // 1 = lighten
        // -1 = darken
        for (rule in messageARules)
        {
            // For each pixel in this rule's patch0
            // check for a conflict, and add it to conflicts bitmap if one is found
            // (bitmap is used only because it is a convenient data structure)

            // messageA, patch0 (first)
            for (pixel in rule.patch0.points)
            {
                // Get the pixel's coordinates
                val y = pixel.toY(coverImageBitmap)
                val x = pixel.toX(coverImageBitmap)

                // Debug: this should never happen
                if (x >= conflicts.width)
                {
                    print("Error: Solver received an x coordinate for a pixel that is larger than the width of its bitmap.")
                }

                if (y >= conflicts.height)
                {
                    print("Error: Solver received a y coordinate for a pixel that is larger than the height of its bitmap.")
                }

                // Conflicts bitmap corresponds to the cover bitmap
                // Set the pixel in the conflicts bitmap that corresponds to this pixel from the cover bitmap
                // Also set this pixels constraint (replaces the color value)
                // GREATER(1)
                // EQUAL(0)
                // LESS(-1)
                conflicts.setPixel(x, y, rule.constraint.constraint)
            }

            // Now patch1
            // messageA patch1 (second patch)
            for (pixel in rule.patch1.points)
            {
                val y = pixel.toY(coverImageBitmap)
                val x = pixel.toX(coverImageBitmap)

                // Debug: this should never happen
                if (x >= conflicts.width)
                {
                    print("Error: Solver received an x coordinate for a pixel that is larger than the width of its bitmap.")
                }

                if (y >= conflicts.height)
                {
                    print("Error: Solver received a y coordinate for a pixel that is larger than the height of its bitmap.")
                }

                conflicts.setPixel(x, y, rule.constraint.constraint)
            }

            print("Done with MessageA conflicts.")
        }

        // Rules for MessageB

        // This second loop will update each pixel in the constraints bitmap by adding or subtracting 1 (add 1 for lighten, subtract one for darken)
        // All pixels in the conflict bitmap should now have a value of 2, -2, or 0
        // 2 = lighten
        // -2 = darken
        // 0 = conflict (do not alter the pixel)
        for (rule in messageBRules)
        {
            for (pixel in rule.patch0.points)
            {
                val y = pixel.toY(coverImageBitmap)
                val x = pixel.toX(coverImageBitmap)

                // Add 1 or -1 to the current constraint value.
                val oldValue = conflicts.getPixel(x, y)
                val newValue = oldValue + rule.constraint.constraint
                conflicts.setPixel(x, y, newValue)
            }

            for (pixel in rule.patch1.points)
            {
                val y = pixel.toY(coverImageBitmap)
                val x = pixel.toX(coverImageBitmap)

                // Add 1 or -1 to the current constraint value.
                val oldValue = conflicts.getPixel(x, y)
                val newValue = oldValue + rule.constraint.constraint
                conflicts.setPixel(x, y, newValue)
            }
        }
    }

    fun constrain()
    {
        for (rule in messageARules)
        {
            solution = rule.constrain(solution, conflicts)
        }

        for (rule in messageBRules)
        {
            solution = rule.constrain(solution, conflicts)
        }
    }
}
