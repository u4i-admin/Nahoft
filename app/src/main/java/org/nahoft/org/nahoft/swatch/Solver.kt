package org.nahoft.org.nahoft.swatch;

import android.graphics.Bitmap
import org.nahoft.swatch.Rule;
import org.nahoft.swatch.SetPixel
import org.nahoft.swatch.Pixel
import java.util.*
import java.util.List

public class Solver(val bitmap: Bitmap, var rules1: Array<Rule>, var rules2: Array<Rule>)
{
    var conflicts: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    var solution: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    fun solve(): Bitmap
    {
        removeConflicts()
        constrain()

        return solution
    }

    fun removeConflicts()
    {
        for (rule in rules1)
        {
            for (pixel in rule.patch0.points)
            {
                conflicts.setPixel(pixel.x, pixel.y, rule.constraint.constraint)
            }

            for (pixel in rule.patch1.points)
            {
                conflicts.setPixel(pixel.x, pixel.y, rule.constraint.constraint)
            }
        }

        for (rule in rules2)
        {
            for (pixel in rule.patch0.points)
            {
                val oldValue = conflicts.getPixel(pixel.x, pixel.y)
                val newValue = oldValue + rule.constraint.constraint
                conflicts.setPixel(pixel.x, pixel.y, newValue)
            }

            for (pixel in rule.patch1.points)
            {
                val oldValue = conflicts.getPixel(pixel.x, pixel.y)
                val newValue = oldValue + rule.constraint.constraint
                conflicts.setPixel(pixel.x, pixel.y, newValue)
            }
        }
    }

    fun constrain()
    {
        for (rule in rules1)
        {
            solution = rule.constrain(solution, conflicts)
        }

        for (rule in rules2)
        {
            solution = rule.constrain(solution, conflicts)
        }
    }
}
