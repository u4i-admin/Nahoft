package org.operatorfoundation.stencil

import android.graphics.Bitmap

class Stencil {
    fun encode(plaintext: String, cover: Bitmap): Bitmap
    {
        return cover
    }

    fun decode(ciphertext: Bitmap): String
    {
        return "TBD"
    }
}
