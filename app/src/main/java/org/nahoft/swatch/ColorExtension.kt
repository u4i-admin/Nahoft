package org.nahoft.org.nahoft.swatch

import android.graphics.Color

fun Color.alphaInt(): Int
{
    return this.toArgb().shr(24) and 0xff
}

fun Color.redInt(): Int
{
    return this.toArgb().shr(16) and 0xff
}

fun Color.greenInt(): Int
{
    return this.toArgb().shr( 8 ) and 0xff
}

fun Color.blueInt(): Int
{
    return this.toArgb() and 0xff
}

fun Color.brightness(): Int
{
    return (redInt() + greenInt() + blueInt()) / 3
}