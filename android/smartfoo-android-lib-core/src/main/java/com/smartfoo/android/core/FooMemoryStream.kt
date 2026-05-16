package com.smartfoo.android.core

import kotlin.math.min

/**
 * A resizable in-memory byte stream that supports sequential read and write operations on
 * primitive integer types and null-terminated strings.
 *
 * The internal buffer grows automatically in [BLOCK_SIZE]-aligned increments as data is written.
 * A separate [getPosition]/[setPosition] cursor tracks the current read/write offset within the
 * logical [getLength] of the stream. Multi-byte integers are written in big-endian (network) byte
 * order. Factory methods on the companion object ([newBytes]) produce raw little-endian byte arrays
 * from primitive values, which the string formatting helpers in [FooString] then interpret.
 *
 * This class is thread-safe; all public methods are `@Synchronized`.
 *
 * @param capacity initial buffer capacity in bytes; the buffer will grow as needed
 */
@Suppress("unused")
open class FooMemoryStream
    @JvmOverloads
    constructor(
        capacity: Int = BLOCK_SIZE,
    ) {
        companion object {
            val EMPTY_BUFFER = ByteArray(0)
            const val BLOCK_SIZE = 256

            /**
             * Validates that reading [size] bytes starting at [offset] within a logical [length]
             * does not exceed the buffer bounds.
             *
             * @param size number of bytes to be read
             * @param buffer the backing buffer (used to validate [length])
             * @param offset current read position
             * @param length logical length of valid data in [buffer]
             * @param checkParameters if true, validates [length] and [offset] preconditions
             * @param throwException if true, throws [IndexOutOfBoundsException] on overflow;
             *   otherwise returns false
             * @return true if the read is within bounds, false if not (when [throwException] is false)
             * @throws IllegalArgumentException if [checkParameters] is true and arguments are invalid
             * @throws IndexOutOfBoundsException if the read would exceed [length] and [throwException] is true
             */
            protected fun checkOffset(
                size: Int,
                buffer: ByteArray,
                offset: Int,
                length: Int, //
                checkParameters: Boolean = true,
                throwException: Boolean = true,
            ): Boolean {
                if (checkParameters) {
                    require(length <= buffer.size) { "length($length) must be <= buffer.length(${buffer.size})" }
                    require(offset in 0..<length) {
                        "offset($offset) must be >= 0 and < (length($length) or buffer.length(${buffer.size}))"
                    }
                }

                if (offset + size > length) {
                    if (throwException) {
                        throw IndexOutOfBoundsException(
                            "attempted to read $size bytes past offset($offset) would exceed length($length)",
                        )
                    }
                    return false
                }
                return true
            }

            private fun unsignedByteToInt(value: Byte): Int = value.toInt() and 0xff

            private fun unsignedByteToInt(
                value: Byte,
                leftShift: Int,
            ): Int = unsignedByteToInt(value) shl leftShift

            /**
             * Returns a 1-byte little-endian representation of [value].
             *
             * @param value the byte value
             * @return a single-element [ByteArray]
             */
            fun newBytes(value: Byte): ByteArray =
                byteArrayOf(
                    value,
                )

            /**
             * Returns a 2-byte little-endian representation of [value].
             *
             * @param value the short value
             * @return a 2-element [ByteArray] in little-endian order
             */
            fun newBytes(value: Short): ByteArray =
                byteArrayOf(
                    (value.toInt() and 0xFF).toByte(),
                    ((value.toInt() shr 8) and 0xFF).toByte(),
                )

            /**
             * Returns a 4-byte little-endian representation of [value].
             *
             * @param value the int value
             * @return a 4-element [ByteArray] in little-endian order
             */
            fun newBytes(value: Int): ByteArray =
                byteArrayOf(
                    (value and 0xFF).toByte(),
                    ((value shr 8) and 0xFF).toByte(),
                    ((value shr 16) and 0xFF).toByte(),
                    ((value shr 24) and 0xFF).toByte(),
                )

            /**
             * Returns an 8-byte little-endian representation of [value].
             *
             * @param value the long value
             * @return an 8-element [ByteArray] in little-endian order
             */
            fun newBytes(value: Long): ByteArray =
                byteArrayOf(
                    (value and 0xFFL).toByte(),
                    ((value shr 8) and 0xFFL).toByte(),
                    ((value shr 16) and 0xFFL).toByte(),
                    ((value shr 24) and 0xFFL).toByte(),
                    ((value shr 32) and 0xFFL).toByte(),
                    ((value shr 40) and 0xFFL).toByte(),
                    ((value shr 48) and 0xFFL).toByte(),
                    ((value shr 56) and 0xFFL).toByte(),
                )
        }

        @get:Synchronized
        var buffer = EMPTY_BUFFER // never null
            protected set
        private var position = 0
        private var length = 0

        init {
            makeSpaceFor(capacity)
        }

        /**
         * Resets the logical [getLength] to zero without releasing the backing buffer, effectively
         * discarding all previously written content while retaining allocated capacity.
         */
        @Synchronized
        fun reset() {
            setLength(0)
            //setPosition(0);
        }

        /**
         * Releases the backing buffer and resets both length and position to zero.
         *
         * Unlike [reset], this also frees the allocated memory by replacing the buffer with
         * [EMPTY_BUFFER].
         */
        @Synchronized
        fun clear() {
            buffer = EMPTY_BUFFER
            reset()
        }

        @get:Synchronized
        val capacity: Int
            get() = buffer.size

        @Synchronized
        fun getPosition(): Int = this.position

        /**
         * Sets the read/write cursor to [position], expanding the buffer if necessary.
         *
         * @param position the new cursor offset; must be >= 0
         */
        @Synchronized
        fun setPosition(position: Int) {
            makeSpaceFor(position)
            this.position = position
        }

        @Synchronized
        fun incPosition(amount: Int): Int {
            setPosition(getPosition() + amount)
            return getPosition()
        }

        @Synchronized
        fun getLength(): Int = this.length

        /**
         * Sets the logical length of the stream.
         *
         * When growing, the newly exposed bytes are zeroed. When shrinking, the position is clamped
         * to [length] if it would otherwise exceed it.
         *
         * @param length the new logical length; must be >= 0
         * @throws IllegalArgumentException if [length] is negative
         */
        @Synchronized
        fun setLength(length: Int) {
            require(length >= 0) { "length must be >= 0" }

            makeSpaceFor(length)

            // Re-zero bytes added when growing; no-op when shrinking.
            if (length > this.length) {
                FooArrays.fill(this.buffer, 0.toByte(), this.length, length)
            }

            this.length = length

            if (this.position > this.length) {
                this.position = this.length
            }
        }

        @Synchronized
        fun incLength(amount: Int): Int {
            setLength(getLength() + amount)
            return getPosition()
        }

        /**
         * Ensures the backing buffer is at least [size] bytes long, growing in
         * [BLOCK_SIZE]-aligned increments if necessary.
         *
         * The logical [getLength] and [getPosition] are unchanged by this call.
         *
         * @param size the minimum required buffer capacity in bytes
         * @return true if the buffer was reallocated (i.e. it was too small), false if no
         *         change was needed
         */
        @Synchronized
        protected fun makeSpaceFor(size: Int): Boolean {
            var size = size
            if (size <= buffer.size) {
                // already big enough, do nothing
                // this also handles the size <= 0 case
                return false
            }

            val remainder = size % BLOCK_SIZE
            size = size / BLOCK_SIZE * BLOCK_SIZE
            if (remainder > 0) {
                size += BLOCK_SIZE
            }
            if (size == 0) {
                return false
            }

            // only need to copy the bytes in the array that are actually used
            // TODO: Seems a little heavy to do in a method that is called by every write method
            buffer = FooArrays.copy(buffer, 0, ByteArray(size), 0, length)

            // position and length remain unchanged
            return true
        }

        private fun extendLengthToPosition() {
            if (position > length) {
                length = position
            }
        }

        /**
         * Writes [length] bytes from [buffer] starting at [offset] at the current position,
         * then advances the position by [length].
         *
         * @param buffer source byte array
         * @param offset starting index in [buffer]
         * @param length number of bytes to write
         */
        @Synchronized
        fun write(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ) {
            makeSpaceFor(position + length)
            FooArrays.copy(buffer, offset, this.buffer, position, length)
            position += length
            extendLengthToPosition()
        }

        @Synchronized
        fun writeInt8(value: Byte) {
            makeSpaceFor(position + 1)
            buffer[position++] = value
            extendLengthToPosition()
        }

        /**
         * Writes an unsigned 8-bit value (supplied as a [Short] to accommodate values 0–255).
         *
         * @param value value in the range 0–255; throws if the high byte is non-zero
         * @throws IllegalArgumentException if [value] exceeds 0xFF
         */
        @Synchronized
        fun writeUInt8(value: Short) {
            require((value.toInt() shr 8) == 0) {
                "value is not a uint8: 0x${FooString.toHexString(value, 4)}"
            }
            makeSpaceFor(position + 1)
            buffer[position++] = value.toByte()
            extendLengthToPosition()
        }

        @Synchronized
        fun writeInt16(value: Short) {
            makeSpaceFor(position + 2)
            buffer[position++] = (value.toInt() shr 8).toByte()
            buffer[position++] = value.toByte()
            extendLengthToPosition()
        }

        @Synchronized
        fun writeUInt16(value: Int) {
            require((value shr 16) == 0) {
                "value is not a uint16: 0x${FooString.toHexString(value, 8)}"
            }
            makeSpaceFor(position + 2)
            buffer[position++] = (value shr 8).toByte()
            buffer[position++] = value.toByte()
            extendLengthToPosition()
        }

        @Synchronized
        fun writeInt32(value: Long) {
            makeSpaceFor(position + 4)
            buffer[position++] = (value shr 24).toByte()
            buffer[position++] = (value shr 16).toByte()
            buffer[position++] = (value shr 8).toByte()
            buffer[position++] = value.toByte()
            extendLengthToPosition()
        }

        @Synchronized
        fun writeUInt32(value: Long) {
            require((value shr 32) == 0L) {
                "value is not a uint32: 0x${FooString.toHexString(value, 16)}"
            }
            makeSpaceFor(position + 4)
            buffer[position++] = (value shr 24).toByte()
            buffer[position++] = (value shr 16).toByte()
            buffer[position++] = (value shr 8).toByte()
            buffer[position++] = value.toByte()
            extendLengthToPosition()
        }

        /**
         * Writes a null-terminated UTF-8 string at the current position.
         *
         * If [value] is null or empty, only the null terminator byte is written.
         *
         * @param value the string to write; null writes only the null terminator
         */
        @Synchronized
        fun writeString(value: String?) {
            if (!value.isNullOrEmpty()) {
                val b = FooString.getBytes(value)
                makeSpaceFor(position + b.size + 1) // null terminated
                write(b, 0, b.size)
            }
            writeUInt8(0.toShort())
            extendLengthToPosition()
        }

        /**
         * Reads up to [count] bytes into [dest] starting at [offset], advancing the position by
         * the number of bytes actually read.
         *
         * The actual count is clamped to the number of bytes remaining in the stream.
         *
         * @param dest destination array
         * @param offset starting index in [dest]
         * @param count maximum number of bytes to read
         * @return the number of bytes actually read
         */
        @Synchronized
        fun read(
            dest: ByteArray,
            offset: Int,
            count: Int,
        ): Int {
            var count = count
            count = min(count, length - position)
            FooArrays.copy(buffer, position, dest, offset, count)
            position += count
            return count
        }

        @Synchronized
        fun readInt8(): Byte {
            checkOffset(1, buffer, position, length)
            return buffer[position++]
        }

        @Synchronized
        fun readUInt8(): Short {
            checkOffset(1, buffer, position, length)
            return unsignedByteToInt(buffer[position++]).toShort()
        }

        @Synchronized
        fun readInt16(): Short {
            checkOffset(2, buffer, position, length)
            var value = unsignedByteToInt(buffer[position++], 8)
            value += unsignedByteToInt(buffer[position++])
            return value.toShort()
        }

        @Synchronized
        fun readUInt16(): Int {
            checkOffset(2, buffer, position, length)
            var value = unsignedByteToInt(buffer[position++], 8)
            value += unsignedByteToInt(buffer[position++])
            return value
        }

        @Synchronized
        fun readInt32(): Int {
            checkOffset(4, buffer, position, length)
            var value = unsignedByteToInt(buffer[position++], 24)
            value += unsignedByteToInt(buffer[position++], 16)
            value += unsignedByteToInt(buffer[position++], 8)
            value += unsignedByteToInt(buffer[position++])
            return value
        }

        @Synchronized
        fun readUInt32(): Long {
            checkOffset(4, buffer, position, length)
            var value = unsignedByteToInt(buffer[position++]).toLong() shl 24
            value += unsignedByteToInt(buffer[position++]).toLong() shl 16
            value += unsignedByteToInt(buffer[position++]).toLong() shl 8
            value += unsignedByteToInt(buffer[position++]).toLong()
            return value
        }

        /**
         * Reads a null-terminated UTF-8 string from the current position and advances past the
         * null terminator.
         *
         * @return the decoded string, not including the null terminator
         * @throws IndexOutOfBoundsException if the end of the stream is reached before a null terminator
         */
        @Synchronized
        fun readString(): String {
            val index = position
            while (checkOffset(
                    1,
                    buffer,
                    position,
                    length,
                ) &&
                buffer[position].toInt() != 0
            ) {
                position++
            }
            position++ // null terminated
            return FooString.getString(buffer, index, position - index - 1)
        }

        /**
         * Returns a debug representation showing the logical length and the hex-encoded content.
         *
         * @return a string of the form "(length):HEX"
         */
        @Synchronized
        fun toDebugString(): String = "($length):${FooString.toHexString(buffer, 0, length)}"
    }
