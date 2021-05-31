package org.nahoft.org.nahoft.swatch;

import android.graphics.Bitmap
import android.graphics.Color
import org.nahoft.swatch.*

public class Solver(val coverImageBitmap: Bitmap, var messageARules: Array<Rule>)
{
    fun solve(): Boolean
    {
        return constrain()
    }

    fun constrain(): Boolean
    {
        for ((index, rule) in messageARules.withIndex())
        {
            println("A: $index / $messageARules.size")
            val success = rule.constrain()
            if (!success) {
                return false
            }
        }

        return true
    }
}
