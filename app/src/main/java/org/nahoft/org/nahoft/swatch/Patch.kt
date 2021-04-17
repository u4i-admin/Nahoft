package org.nahoft.org.nahoft.swatch

class Patch(val patchIndex: Int, val size: Int, var bitmap: MappedBitmap)
{
    var pixels = IntArray(size).mapIndexed { index, value -> Pixel(patchIndex * size + index, bitmap)}
    var pointsToModify = emptyList<Pixel>().toMutableList()
    var brightness: Int

    init {
        brightness = 0
        for (pixel in pixels) {
            brightness += pixel.brightness()
        }
    }
}
