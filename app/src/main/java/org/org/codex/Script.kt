package org.org.codex

import java.util.*

interface Script
{
    fun encode(bits: BitSet): String
    fun decode(ciphertext: String): BitSet
}

interface ScriptElement
{
    fun consume(bits: BitSet): Pair<BitSet, String>
}