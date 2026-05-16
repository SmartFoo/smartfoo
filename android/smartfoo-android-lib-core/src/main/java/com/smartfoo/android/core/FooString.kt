package com.smartfoo.android.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import com.smartfoo.android.core.FooString.quote
import com.smartfoo.android.core.FooString.repr
import com.smartfoo.android.core.platform.FooPlatformUtils
import java.io.UnsupportedEncodingException
import java.util.Locale
import java.util.Vector
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * String manipulation, formatting, and conversion utilities.
 *
 * Covers null-safe checks, hex/bit string formatting, null-terminated byte array encoding,
 * time-duration formatting, camel-case splitting, and [SpannableString] creation for Android
 * text views. UTF-8 is the assumed character set for all byte–string conversions.
 */
@Suppress("unused")
object FooString {
    @JvmField
    val LINEFEED: String? = System.lineSeparator()

    const val CHARSET_NAME_UTF8 = "UTF-8"

    @JvmField
    val CHARSET_UTF8 = charset(CHARSET_NAME_UTF8)

    @JvmField
    val EMPTY_BYTES = byteArrayOf(0)

    /**
     * Encodes [value] as a UTF-8 [ByteArray].
     *
     * @param value the string to encode
     * @return the UTF-8 encoded bytes
     */
    @JvmStatic
    fun getBytes(value: String): ByteArray {
        try {
            return value.toByteArray(CHARSET_UTF8)
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException("UnsupportedEncodingException: Should never happen as long as CHARSET_UTF8 is valid")
        }
    }

    /**
     * Decodes [length] bytes from [bytes] starting at [offset] as a UTF-8 string.
     *
     * @param bytes the source byte array
     * @param offset starting index in [bytes]
     * @param length number of bytes to decode
     * @return the decoded string
     */
    @JvmStatic
    fun getString(
        bytes: ByteArray?,
        offset: Int,
        length: Int,
    ): String =
        try {
            String(bytes!!, offset, length, CHARSET_UTF8)
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException("UnsupportedEncodingException: Should never happen as long as CHARSET_UTF8 is valid")
        }

    /**
     * Tests if a String value is null or empty.
     *
     * @param value the String value to test
     * @return true if the String is null, zero length, or ""
     */
    @JvmStatic
    fun isNullOrEmpty(value: String?): Boolean = value.isNullOrEmpty()

    /**
     * Returns true if [value] is null or has zero length.
     *
     * @param value the CharSequence to test
     * @return true if null or empty
     */
    @JvmStatic
    fun isNullOrEmpty(value: CharSequence?): Boolean = value.isNullOrEmpty()

    /**
     * Returns [value]`.toString()`, or null if [value] is null.
     *
     * @param value the object to convert
     * @return string representation, or null
     */
    @JvmStatic
    fun toString(value: Any?): String? = value?.toString()

    /**
     * Creates a String from a null-terminated array of default String encoded bytes.
     *
     * @param bytes  the array of bytes that contains the string
     * @param offset the offset in the bytes to start testing for null
     * @return the resulting String value of the bytes from offset to null or end (whichever comes first)
     */
    @JvmStatic
    fun fromNullTerminatedBytes(
        bytes: ByteArray?,
        offset: Int,
    ): String? {
        if (bytes == null) {
            return null
        }

        require(offset >= 0) { "offset must be >= 0" }

        var length = 0
        while (offset + length < bytes.size && bytes[offset + length] != '\u0000'.code.toByte()) {
            length++
        }

        if (length == 0) {
            return null
        }

        return String(bytes, offset, length)
    }

    /**
     * Creates a default encoded array of bytes of the given String value. This is not an efficient implementation, so
     * call sparingly.
     *
     * @param s the String value to convert to bytes
     * @return the bytes of the String followed by the null terminator '\0'
     */
    @JvmStatic
    fun toNullTerminatedBytes(s: String): ByteArray {
        val temp = s.toByteArray()
        val bytes = ByteArray(temp.size + 1) // +1 for null terminator
        System.arraycopy(temp, 0, bytes, 0, bytes.size - 1)
        return bytes
    }

    /**
     * Returns an uppercase hex string representation of [bytes] in byte-array format (e.g. `"AA-BB-CC"`).
     *
     * @param bytes the bytes to format, or null
     * @return hex string, or `"null"` if [bytes] is null
     */
    @JvmStatic
    fun toHexString(bytes: ByteArray?): String = toHexString(bytes, true)

    @JvmStatic
    fun toHexString(
        bytes: ByteArray?, //
        asByteArray: Boolean,
    ): String {
        if (bytes == null) return "null"
        return toHexString(bytes, 0, bytes.size, asByteArray)
    }

    @JvmStatic
    fun toHexString(
        bytes: ByteArray?,
        offset: Int,
        count: Int,
    ): String = toHexString(bytes, offset, count, true)

    private val HEX_CHARS =
        charArrayOf(
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9',
            'A',
            'B',
            'C',
            'D',
            'E',
            'F',
        )

    @JvmStatic
    fun toHexString(
        bytes: ByteArray?,
        offset: Int,
        count: Int, //
        asByteArray: Boolean,
    ): String {
        if (bytes == null) return "null"

        val sb = StringBuilder()
        if (asByteArray) {
            var i = offset
            while (i < count) {
                if (i != offset) {
                    sb.append('-')
                }
                sb.append(HEX_CHARS[((bytes[i]).toInt() and 0x000000f0) shr 4])
                sb.append(HEX_CHARS[((bytes[i]).toInt() and 0x0000000f)])
                i++
            }
            if (i < bytes.size) {
                sb.append('…')
            }
        } else {
            for (i in count - 1 downTo 0) {
                sb.append(HEX_CHARS[((bytes[i]).toInt() and 0x000000f0) shr 4])
                sb.append(HEX_CHARS[((bytes[i]).toInt() and 0x0000000f)])
            }
        }

        return sb.toString()
    }

    @JvmStatic
    fun toHexString(
        value: Short,
        maxBytes: Int,
    ) = toHexString(FooMemoryStream.newBytes(value), 0, maxBytes, false)

    @JvmStatic
    fun toHexString(
        value: Int,
        maxBytes: Int,
    ) = toHexString(FooMemoryStream.newBytes(value), 0, maxBytes, false)

    @JvmStatic
    fun toHexString(
        value: Long,
        maxBytes: Int,
    ) = toHexString(FooMemoryStream.newBytes(value), 0, maxBytes, false)

    @JvmStatic
    fun toHexString(value: String) = toHexString(value.toByteArray())

    @JvmOverloads
    @JvmStatic
    fun toBitString(
        value: Byte,
        maxBits: Int,
        spaceEvery: Int = 0,
    ): String {
        var maxBits = maxBits
        val bits = FooBitSet(value)
        maxBits = max(0, min(maxBits, bits.length))
        val sb = StringBuilder()
        for (i in maxBits - 1 downTo 0) {
            sb.append(if (bits.get(i)) '1' else '0')
            if ((spaceEvery != 0) && (i > 0) && (i % spaceEvery == 0)) {
                sb.append(' ')
            }
        }
        return sb.toString()
    }

    @JvmStatic
    fun toBitString(
        bytes: ByteArray,
        maxBits: Int,
        spaceEvery: Int,
    ): String {
        var maxBits = maxBits
        val bits = FooBitSet(bytes)
        maxBits = max(0, min(maxBits, bits.length))
        val sb = StringBuilder()
        for (i in maxBits - 1 downTo 0) {
            sb.append(if (bits.get(i)) '1' else '0')
            if ((spaceEvery != 0) && (i > 0) && (i % spaceEvery == 0)) {
                sb.append(' ')
            }
        }
        return sb.toString()
    }

    @JvmOverloads
    @JvmStatic
    fun toBitString(
        value: Short,
        maxBits: Int,
        spaceEvery: Int = 8,
    ) = toBitString(FooMemoryStream.newBytes(value), maxBits, spaceEvery)

    @JvmOverloads
    @JvmStatic
    fun toBitString(
        value: Int,
        maxBits: Int,
        spaceEvery: Int = 8,
    ) = toBitString(FooMemoryStream.newBytes(value), maxBits, spaceEvery)

    @JvmOverloads
    @JvmStatic
    fun toBitString(
        value: Long,
        maxBits: Int,
        spaceEvery: Int = 8,
    ) = toBitString(FooMemoryStream.newBytes(value), maxBits, spaceEvery)

    /**
     * Returns `'1'` if [value] is true, `'0'` otherwise.
     *
     * @param value the boolean to convert
     * @return `'1'` or `'0'`
     */
    @JvmStatic
    fun toChar(value: Boolean) = if (value) '1' else '0'

    /**
     * Pads [number] on the left with [ch] until the result is at least [minimumLength] characters.
     *
     * @param number the number to format
     * @param ch the padding character
     * @param minimumLength the minimum width of the resulting string
     * @return the padded string
     */
    @JvmStatic
    fun padNumber(
        number: Long,
        ch: Char,
        minimumLength: Int,
    ): String {
        var s = number.toString()
        while (s.length < minimumLength) {
            s = ch.toString() + s
        }
        return s
    }

    @JvmStatic
    fun formatNumber(
        number: Long,
        minimumLength: Int,
    ) = padNumber(number, '0', minimumLength)

    /**
     * Formats [number] with [leading] digits before the decimal and [trailing] digits after,
     * zero-padding as needed and truncating trailing decimal places to [trailing].
     *
     * Returns [number].toString() directly for NaN and infinite values.
     *
     * @param number the value to format
     * @param leading minimum number of digits before the decimal point
     * @param trailing exact number of digits after the decimal point
     * @return formatted string
     */
    @JvmStatic
    fun formatNumber(
        number: Double,
        leading: Int,
        trailing: Int,
    ): String {
        if (number.isNaN() || number == Double.NEGATIVE_INFINITY || number == Double.POSITIVE_INFINITY) {
            return number.toString()
        }

        // String.valueOf(1) is guaranteed to at least be of the form "1.0"
        val parts = split(number.toString(), ".", 0)!!
        while (parts[0].length < leading) {
            parts[0] = '0'.toString() + parts[0]
        }
        while (parts[1].length < trailing) {
            parts[1] = parts[1] + '0'
        }
        parts[1] = parts[1].substring(0, trailing)
        return parts[0] + '.' + parts[1]
    }

    /**
     * Joins [parts] with [delimiter] using [TextUtils.join].
     *
     * @param delimiter the string to insert between parts
     * @param parts the strings to join; individual elements may be null
     * @return the joined string, or null if [parts] is empty
     */
    @JvmStatic
    fun join(
        delimiter: String,
        vararg parts: String?,
    ): String? = TextUtils.join(delimiter, parts)

    /**
     * Returns a string array that contains the substrings in a source string that are delimited by a specified string.
     *
     * @param source    String to split with the given delimiter.
     * @param separator String that delimits the substrings in the source string.
     * @param limit     Determines the maximum number of entries in the resulting array, and the treatment of trailing
     * empty strings.
     *
     *  * For n &gt; 0, the resulting array contains at most n entries. If this is fewer than the
     * number of matches, the final entry will contain all remaining input.
     *  * For n &lt; 0, the length of the resulting array is exactly the number of occurrences of the
     * Pattern plus one for the text after the final separator. All entries are included.
     *  * For n == 0, the result is as for n &lt; 0, except trailing empty strings will not be
     * returned. (Note that the case where the input is itself an empty string is special, as
     * described above, and the limit parameter does not apply there.)
     *
     * @return An array whose elements contain the substrings in a source string that are delimited by a separator
     * string.
     */
    @JvmStatic
    fun split(
        source: String?,
        separator: String?,
        limit: Int,
    ): Array<String>? {
        if (source == null) return null
        val kotlinLimit = if (limit < 0) 0 else limit
        return source.split(separator ?: "", limit = kotlinLimit).toTypedArray()
    }

    /**
     * Replaces the first occurrence of [pattern] in [source] with [replacement].
     *
     * @param source the string to search; returns empty string if null
     * @param pattern the literal substring to find
     * @param replacement the replacement string; null is treated as empty
     * @return the resulting string
     */
    @JvmStatic
    fun replaceFirst(
        source: String?,
        pattern: String,
        replacement: String?,
    ) = replace(source, pattern, replacement, 1)

    /**
     * Replaces occurrences of [pattern] in [source] with [replacement].
     *
     * @param source the string to search; returns empty string if null
     * @param pattern the literal substring to find
     * @param replacement the replacement string; null is treated as empty
     * @param limit maximum number of replacements; -1 for unlimited
     * @return the resulting string, never null
     */
    @JvmOverloads
    @JvmStatic
    fun replace(
        source: String?,
        pattern: String,
        replacement: String?,
        limit: Int = -1,
    ): String {
        if (source == null) {
            return ""
        }

        val sb = StringBuilder()
        var index = -1
        var fromIndex = 0
        var count = 0
        while ((
                    source
                        .indexOf(pattern, fromIndex)
                        .also { index = it }
                    ) != -1 &&
            (limit == -1 || count < limit)
        ) {
            sb.append(source.substring(fromIndex, index))
            sb.append(replacement)
            fromIndex = index + pattern.length
            count++
        }
        sb.append(source.substring(fromIndex))
        return sb.toString()
    }

    @JvmStatic
    fun contains(
        s: String,
        cs: String,
    ) = s.contains(cs)

    /**
     * Identical to [repr], but grammatically intended for Strings.
     *
     * @param value value
     * @return "null", or '\"' + value.toString + '\"', or value.toString()
     */
    @JvmStatic
    fun quote(value: Any?) = repr(value, false)

    /**
     * Identical to [quote], but grammatically intended for Objects.
     *
     * @param value    value
     * @param typeOnly typeOnly
     * @return "null", or '\"' + value.toString + '\"', or value.toString(), or getShortClassName(value)
     */
    @JvmOverloads
    @JvmStatic
    fun repr(
        value: Any?,
        typeOnly: Boolean = false,
    ): String {
        if (value == null) {
            return "null"
        }
        if (typeOnly) {
            return FooReflection.getShortClassName(value)
        }
        return when (value) {
            is String -> "\"$value\""
            is CharSequence -> "\"$value\""
            is Intent -> FooPlatformUtils.toString(value)
            is Bundle -> FooPlatformUtils.toString(value)
            else -> value.toString()
        }
    }

    @JvmStatic
    fun <T> toString(
        items: Iterable<T?>?,
        multiline: Boolean = false,
    ): String {
        val sb = StringBuilder()

        if (items == null) {
            sb.append("null")
        } else {
            sb.append('[')
            if (multiline) {
                sb.append(LINEFEED)
            }
            val it = items.iterator()
            while (it.hasNext()) {
                val item = it.next()
                if (multiline) {
                    sb.append("  ")
                }
                sb.append(quote(item))
                if (it.hasNext()) {
                    sb.append(", ")
                }
                if (multiline) {
                    sb.append(LINEFEED)
                }
            }
            sb.append(']')
        }
        return sb.toString()
    }

    @JvmStatic
    fun toString(items: Array<out Any?>?,
                 formatter: (Any?) -> String = { item -> quote(item) }): String {
        val sb = StringBuilder()

        if (items == null) {
            sb.append("null")
        } else {
            sb.append('[')
            for (i in items.indices) {
                val item = items[i]
                if (i != 0) {
                    sb.append(", ")
                }
                sb.append(formatter(item))
            }
            sb.append(']')
        }
        return sb.toString()
    }

    /**
     * Returns [s] with its first character uppercased. Returns an empty string if [s] is null or empty.
     *
     * @param s the string to capitalise
     * @return the capitalised string
     */
    @JvmStatic
    fun capitalize(s: String?): String {
        if (s.isNullOrEmpty()) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            first.uppercaseChar().toString() + s.substring(1)
        }
    }

    /**
     * @param flags flags
     * @return String in the form of "(flag1|flag3|flag5)"
     */
    @JvmStatic
    fun toFlagString(flags: Vector<*>): String {
        var flag: String?
        val sb = StringBuilder()
        sb.append('(')
        for (i in flags.indices) {
            flag = flags.elementAt(i) as String?
            if (i != 0) {
                sb.append('|')
            }
            sb.append(flag)
        }
        sb.append(')')
        return sb.toString()
    }

    /**
     * @param str1 str1
     * @param str2 str2
     * @return str1 != null ? str1.equals(str2) : str2 == null
     */
    @JvmStatic
    fun equals(
        str1: String?,
        str2: String?,
    ) = if (str1 != null) (str1 == str2) else str2 == null

    /*
    public static String plurality(int count)
    {
        return (count == 1) ? "" : "s";
    }

    /**
     * Generates a plurality of a given name of a given count.<br>
     * Examples:<br>
     * Plurality("item", 0)="items"<br>
     * Plurality("name", 1)="name"<br>
     * Plurality("answer", 42)="answers"<br>
     *
     * @param name  the name to potentially make plural
     * @param count the number of items
     * @return if (count == 1) then return name else return (name + "s")
     */
    public static String plurality(String name, int count)
    {
        return name + plurality(count);
    }
    */

    /**
     * @param msElapsed msElapsed
     * @return HH:MM:SS.MMM
     */
    @JvmStatic
    fun getTimeElapsedString(msElapsed: Long): String {
        var msElapsed = msElapsed
        var h: Long = 0
        var m: Long = 0
        var s: Long = 0
        if (msElapsed > 0) {
            h = (msElapsed / (3600 * 1000)).toInt().toLong()
            msElapsed -= (h * 3600 * 1000)
            m = (msElapsed / (60 * 1000)).toInt().toLong()
            msElapsed -= (m * 60 * 1000)
            s = (msElapsed / 1000).toInt().toLong()
            msElapsed -= (s * 1000)
        } else {
            msElapsed = 0
        }

        return formatNumber(h, 2) + ":" + formatNumber(m, 2) + ":" + formatNumber(s, 2) + "." +
                formatNumber(msElapsed, 3)
    }

    @JvmStatic
    fun getTimeDurationString(
        context: Context,
        elapsedMillis: Long,
    ) = getTimeDurationString(context, elapsedMillis, true)

    @JvmStatic
    fun getTimeDurationString(
        context: Context,
        elapsedMillis: Long,
        expanded: Boolean,
    ) = getTimeDurationString(context, elapsedMillis, expanded, null)

    @JvmStatic
    fun getTimeDurationString(
        context: Context,
        elapsedMillis: Long,
        minimumTimeUnit: TimeUnit?,
    ) = getTimeDurationString(context, elapsedMillis, true, minimumTimeUnit)

    /**
     * @param context         context
     * @param elapsedMillis   elapsedMillis
     * @param expanded        if true then formatted as "X days, X hours, X minutes, X seconds, …", otherwise,
     * formatted as "XX minutes", or "XX hours", or "X days"
     * @param minimumTimeUnit must be &gt;= TimeUnit.MILLISECONDS, or null to default to TimeUnit.SECONDS
     * @return null if elapsedMillis &lt; 0
     */
    @JvmStatic
    fun getTimeDurationString(
        context: Context,
        elapsedMillis: Long,
        expanded: Boolean,
        minimumTimeUnit: TimeUnit?,
    ): String? {
        var elapsedMillis = elapsedMillis
        var minimumTimeUnit = minimumTimeUnit

        if (minimumTimeUnit == null) {
            minimumTimeUnit = TimeUnit.SECONDS
        }

        require(TimeUnit.MILLISECONDS.compareTo(minimumTimeUnit) <= 0) { "minimumTimeUnit must be null or >= TimeUnit.MILLISECONDS" }

        var result: String? = null

        if (elapsedMillis >= 0) {
            val sb = StringBuilder()

            val res = context.resources

            if (expanded) {
                val days = TimeUnit.MILLISECONDS.toDays(elapsedMillis).toInt()
                if (days > 0 && TimeUnit.DAYS.compareTo(minimumTimeUnit) >= 0) {
                    elapsedMillis -= TimeUnit.DAYS.toMillis(days.toLong())
                    val temp = res.getQuantityString(R.plurals.days, days, days)
                    sb.append(' ').append(temp)
                }

                val hours = TimeUnit.MILLISECONDS.toHours(elapsedMillis).toInt()
                if (hours > 0 && TimeUnit.HOURS.compareTo(minimumTimeUnit) >= 0) {
                    elapsedMillis -= TimeUnit.HOURS.toMillis(hours.toLong())
                    val temp = res.getQuantityString(R.plurals.hours, hours, hours)
                    sb.append(' ').append(temp)
                }

                val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis).toInt()
                if (minutes > 0 && TimeUnit.MINUTES.compareTo(minimumTimeUnit) >= 0) {
                    elapsedMillis -= TimeUnit.MINUTES.toMillis(minutes.toLong())
                    val temp = res.getQuantityString(R.plurals.minutes, minutes, minutes)
                    sb.append(' ').append(temp)
                }

                val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis).toInt()
                if (seconds > 0 && TimeUnit.SECONDS.compareTo(minimumTimeUnit) >= 0) {
                    elapsedMillis -= TimeUnit.SECONDS.toMillis(seconds.toLong())
                    val temp = res.getQuantityString(R.plurals.seconds, seconds, seconds)
                    sb.append(' ').append(temp)
                }

                val milliseconds = elapsedMillis.toInt()
                if (TimeUnit.MILLISECONDS.compareTo(minimumTimeUnit) >= 0) {
                    val temp =
                        res.getQuantityString(R.plurals.milliseconds, milliseconds, milliseconds)
                    sb.append(' ').append(temp)
                }

                if (sb.isEmpty()) {
                    val timeUnitNameResId =
                        when (minimumTimeUnit) {
                            TimeUnit.DAYS -> R.plurals.days
                            TimeUnit.HOURS -> R.plurals.hours
                            TimeUnit.MINUTES -> R.plurals.minutes
                            TimeUnit.SECONDS -> R.plurals.seconds
                            TimeUnit.MILLISECONDS -> R.plurals.milliseconds
                            else -> R.plurals.milliseconds
                        }
                    val temp = res.getQuantityString(timeUnitNameResId, 0, 0)
                    sb.append(' ').append(temp)
                }
            } else {
                val timeUnitNameResId: Int

                var timeUnitValue = TimeUnit.MILLISECONDS.toDays(elapsedMillis).toInt()
                if (timeUnitValue > 0 || TimeUnit.DAYS.compareTo(minimumTimeUnit) <= 0) {
                    timeUnitNameResId = R.plurals.days
                } else {
                    timeUnitValue = TimeUnit.MILLISECONDS.toHours(elapsedMillis).toInt()
                    if (timeUnitValue > 0 || TimeUnit.HOURS.compareTo(minimumTimeUnit) <= 0) {
                        timeUnitNameResId = R.plurals.hours
                    } else {
                        timeUnitValue = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis).toInt()
                        if (timeUnitValue > 0 || TimeUnit.MINUTES.compareTo(minimumTimeUnit) <= 0) {
                            timeUnitNameResId = R.plurals.minutes
                        } else {
                            timeUnitValue = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis).toInt()
                            if (timeUnitValue > 0 || TimeUnit.SECONDS.compareTo(minimumTimeUnit) <= 0) {
                                timeUnitNameResId = R.plurals.seconds
                            } else {
                                timeUnitValue = elapsedMillis.toInt()
                                timeUnitNameResId = R.plurals.milliseconds
                            }
                        }
                    }
                }

                sb.append(res.getQuantityString(timeUnitNameResId, timeUnitValue, timeUnitValue))
            }

            result =
                sb
                    .toString()
                    // Remove any unspeakable/unprintable characters
                    //noinspection TrimLambda
                    .trim { it <= ' ' }
        }

        return result
    }

    /**
     * @param msElapsed msElapsed
     * @return HH:MM:SS.MMM
     */
    @JvmStatic
    fun getTimeDurationFormattedString(msElapsed: Long): String {
        var msElapsed = msElapsed
        var h: Long = 0
        var m: Long = 0
        var s: Long = 0
        if (msElapsed > 0) {
            h = (msElapsed / (3600 * 1000)).toInt().toLong()
            msElapsed -= (h * 3600 * 1000)
            m = (msElapsed / (60 * 1000)).toInt().toLong()
            msElapsed -= (m * 60 * 1000)
            s = (msElapsed / 1000).toInt().toLong()
            msElapsed -= (s * 1000)
        } else {
            msElapsed = 0
        }

        return formatNumber(h, 2) + ":" + formatNumber(m, 2) + ":" + formatNumber(s, 2) + "." +
                formatNumber(msElapsed, 3)
    }

    /**
     * Splits a camelCase or PascalCase string into space-separated words and trims the result.
     *
     * @param s the string to split; null produces an empty string
     * @return the separated, trimmed string
     */
    @JvmStatic
    fun separateCamelCaseWords(s: String?): String {
        val sb = StringBuilder()
        if (s != null) {
            val parts: Array<String?> =
                s.split("(?=\\p{Lu})".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (part in parts) {
                sb.append(part).append(' ')
            }
        }
        return sb
            .toString()
            // Remove any unspeakable/unprintable characters
            //noinspection TrimLambda
            .trim { it <= ' ' }
    }

    @JvmStatic
    fun bytesToHexString(value: Short, maxBytes: Int, lowerCase: Boolean): String {
        return bytesToHexString(value, false, maxBytes, lowerCase)
    }

    @JvmStatic
    fun bytesToHexString(
        value: Short,
        reverse: Boolean,
        maxBytes: Int,
        lowerCase: Boolean
    ): String {
        var value = value
        if (reverse) {
            value = java.lang.Short.reverseBytes(value)
        }
        var s = toHexString(value, maxBytes)
        if (lowerCase) {
            s = s.lowercase(Locale.getDefault())
        }
        return s
    }

    /**
     * Returns true if [s] starts with any character in [vowels].
     *
     * @param vowels a string whose characters are each treated as a potential starting vowel
     * @param s the string to test; null returns false
     * @return true if [s] starts with one of the specified characters
     */
    @JvmStatic
    fun startsWithVowel(
        vowels: String?,
        s: String?,
    ) = s != null && s.matches(("^[$vowels].*").toRegex())

//
//
//

    /**
     * @param text            text to set the SpannableString to
     * @param foregroundColor One of Color.*, or any other 0xaarrggbb value
     * @param backgroundColor One of Color.*, or any other 0xaarrggbb value; -1 to ignore
     * @param style           Typeface.*; -1 to ignore
     * @param family          The font family for this typeface.  Examples include "monospace", "serif", and
     * "sans-serif"; null to ignore
     * @param proportion      Size of the font; Example 1.0f, 0.8f, ...; Float.NaN to ignore
     * @return a SpannableString with the given text, color, and optional style
     */
    @JvmStatic
    fun newSpannableString(
        text: String?,
        foregroundColor: Int, backgroundColor: Int,
        style: Int,
        family: String?,
        proportion: Float
    ): SpannableString {
        val value = SpannableString(text)
        var length = value.length
        if (backgroundColor != -1) {
            value.setSpan(
                BackgroundColorSpan(backgroundColor),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            length = value.length
        }
        value.setSpan(
            ForegroundColorSpan(foregroundColor),
            0,
            length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (style != -1) {
            value.setSpan(StyleSpan(style), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            length = value.length
        }
        if (!FooString.isNullOrEmpty(family)) {
            value.setSpan(TypefaceSpan(family), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            length = value.length
        }
        if (!proportion.isNaN()) {
            value.setSpan(RelativeSizeSpan(proportion), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            //length = value.length
        }
        return value
    }
}
