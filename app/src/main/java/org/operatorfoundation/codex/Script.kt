package org.operatorfoundation.codex

import java.util.*

class Script(val elements: Array<ScriptElement>) {
    fun encode(bits: BitSet): String
    {
        return "TBD"
    }

    fun decode(ciphertext: String): BitSet
    {
        return BitSet(0)
    }
}

interface ScriptElement
{
    fun consume(bits: BitSet): Pair<BitSet, String>
}