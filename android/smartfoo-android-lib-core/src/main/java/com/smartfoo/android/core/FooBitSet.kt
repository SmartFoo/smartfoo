package com.smartfoo.android.core

import java.util.BitSet

/**
 * A bit-level view over one or more bytes, with least-significant-bit at index 0.
 *
 * Construct from a single [Byte] or a [ByteArray]. Each byte contributes 8 bits;
 * bit index 0 is the LSB of the first byte. Use [get] to read individual bits by index.
 */
class FooBitSet {
    companion object {
        /** Number of bits in a single byte. */
        const val BITS_PER_BYTE: Byte = 8
    }

    /** Total number of bits represented by this instance. */
    val length: Int
    private val bitset: BitSet

    /**
     * Constructs a [FooBitSet] from a single byte.
     *
     * @param value the byte whose bits are stored; bit 0 is the LSB
     */
    constructor(value: Byte) {
        length = BITS_PER_BYTE.toInt()
        bitset = BitSet(length)

        // Walk through bytes and set the bits
        for (j in 0..<BITS_PER_BYTE) {
            if ((value.toInt() and (1 shl j)) != 0) {
                bitset.set(j)
            }
        }
    }

    /**
     * Constructs a [FooBitSet] from an array of bytes.
     *
     * The resulting [length] is `bytes.size * 8`. Bit index 0 is the LSB of `bytes[0]`.
     *
     * @param bytes the byte array whose bits are stored
     */
    constructor(bytes: ByteArray) {
        length = bytes.size * BITS_PER_BYTE
        bitset = BitSet(length)

        // Walk through bytes and set the bits
        for (i in bytes.indices) {
            for (j in 0..<BITS_PER_BYTE) {
                if ((bytes[i].toInt() and (1 shl j)) != 0) {
                    bitset.set(i * BITS_PER_BYTE + j)
                }
            }
        }
    }

    /**
     * Returns the bit value at the given zero-based [index].
     *
     * @param index zero-based bit position; 0 is the LSB of the first byte
     * @return true if the bit is set, false otherwise
     */
    fun get(index: Int): Boolean = bitset.get(index)
}
