package org.nahoft.org.nahoft.swatch;

import android.graphics.Bitmap
import org.nahoft.swatch.Rule;
import org.nahoft.swatch.SetPixel
import org.nahoft.swatch.Pixel
import java.util.*

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
                val y = pixel.toY(bitmap)
                val x = pixel.toX(bitmap)

                if (x >= conflicts.width)
                {
                    print("Error")
                }

                if (y >= conflicts.height)
                {
                    print("Error")
                }

                conflicts.setPixel(x, y, rule.constraint.constraint)
            }

            for (pixel in rule.patch1.points)
            {
                val y = pixel.toY(bitmap)
                val x = pixel.toX(bitmap)

                if (x >= conflicts.width)
                {
                    print("Error")
                }

                if (y >= conflicts.height)
                {
                    print("Error")
                }

                conflicts.setPixel(x, y, rule.constraint.constraint)
            }

            print("Done")
        }

        for (rule in rules2)
        {
            for (pixel in rule.patch0.points)
            {
                val y = pixel.toY(bitmap)
                val x = pixel.toX(bitmap)
                val oldValue = conflicts.getPixel(x, y)
                val newValue = oldValue + rule.constraint.constraint
                conflicts.setPixel(x, y, newValue)
            }

            for (pixel in rule.patch1.points)
            {
                val y = pixel.toY(bitmap)
                val x = pixel.toX(bitmap)
                val oldValue = conflicts.getPixel(x, y)
                val newValue = oldValue + rule.constraint.constraint
                conflicts.setPixel(x, y, newValue)
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
