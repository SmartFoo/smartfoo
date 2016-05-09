package com.smartfoo.android.core;

import android.content.Context;
import android.content.res.Resources;

import java.io.UnsupportedEncodingException;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class FooString
{
    private FooString()
    {
    }

    public static final String LINEFEED = System.getProperty("line.separator");

    public static final String Utf8Encoding = "utf-8";

    public static final byte[] EMPTY_BYTES = new byte[]
            {
                    0
            };

    public static byte[] getBytes(String value)
    {
        try
        {
            return value.getBytes(Utf8Encoding);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException("UnsupportedEncodingException: Should never happen as long as Utf8Encoding is valid");
        }
    }

    public static String getString(byte[] bytes, int offset, int length)
    {
        try
        {
            // TODO:(pv) Does this work for *all* UTF8 strings?
            return new String(bytes, offset, length, Utf8Encoding);
        }
        catch (UnsupportedEncodingException e)
        {
            // Should *NEVER* happen since this method always uses a supported encoding
            return null;
        }
    }

    /**
     * Tests if a String value is null or empty.
     *
     * @param value the String value to test
     * @return true if the String is null, zero length, or ""
     */
    public static boolean isNullOrEmpty(String value)
    {
        return (value == null || value.length() == 0 || value.equals(""));
    }

    public static boolean isNullOrEmpty(CharSequence value)
    {
        return (value == null || value.length() == 0 || value.equals(""));
    }

    /**
     * Creates a String from a null-terminated array of default String encoded bytes.
     *
     * @param bytes  the array of bytes that contains the string
     * @param offset the offset in the bytes to start testing for null
     * @return the resulting String value of the bytes from offset to null or end (whichever comes first)
     */
    public static String fromNullTerminatedBytes(byte[] bytes, int offset)
    {
        if (bytes == null)
        {
            return null;
        }

        if (offset < 0)
        {
            throw new IllegalArgumentException("offset must be >= 0");
        }

        int length = 0;
        while (offset + length < bytes.length && bytes[offset + length] != '\0')
        {
            length++;
        }

        if (length == 0)
        {
            return null;
        }

        return new String(bytes, offset, length);
    }

    /**
     * Creates a default encoded array of bytes of the given String value. This is not an efficient implementation, so
     * call sparingly.
     *
     * @param s the String value to convert to bytes
     * @return the bytes of the String followed by the null terminator '\0'
     */
    public static byte[] toNullTerminatedBytes(String s)
    {
        byte[] temp = s.getBytes();
        byte[] bytes = new byte[temp.length + 1]; // +1 for null terminator
        System.arraycopy(temp, 0, bytes, 0, bytes.length - 1);
        return bytes;
    }

    public static String toHexString(byte[] bytes)
    {
        return toHexString(bytes, true);
    }

    public static String toHexString(byte[] bytes, //
                                     boolean asByteArray)
    {
        if (bytes == null)
        {
            return "";
        }
        return toHexString(bytes, 0, bytes.length, asByteArray);
    }

    public static String toHexString(byte[] bytes, int offset, int count)
    {
        return toHexString(bytes, offset, count, true);
    }

    public static String toHexString(byte[] bytes, int offset, int count, //
                                     boolean asByteArray)
    {
        if (bytes == null)
        {
            return "";
        }

        final char[] hexChars =
                {
                        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
                };

        StringBuffer sb = new StringBuffer();
        if (asByteArray)
        {
            for (int i = offset; i < count; i++)
            {
                if (i != offset)
                {
                    sb.append('-');
                }
                sb.append(hexChars[((bytes[i]) & 0x000000f0) >> 4]);
                sb.append(hexChars[((bytes[i]) & 0x0000000f)]);
            }
        }
        else
        {
            for (int i = count - 1; i >= 0; i--)
            {
                sb.append(hexChars[((bytes[i]) & 0x000000f0) >> 4]);
                sb.append(hexChars[((bytes[i]) & 0x0000000f)]);
            }
        }
        return sb.toString();
    }

    public static String toHexString(short value, int maxBytes)
    {
        return toHexString(FooMemoryStream.getBytes(value), 0, maxBytes, false);
    }

    public static String toHexString(int value, int maxBytes)
    {
        return toHexString(FooMemoryStream.getBytes(value), 0, maxBytes, false);
    }

    public static String toHexString(long value, int maxBytes)
    {
        return toHexString(FooMemoryStream.getBytes(value), 0, maxBytes, false);
    }

    public static String toHexString(String value)
    {
        return toHexString(value.getBytes());
    }

    public static String toBitString(byte[] bytes, int maxBits, int spaceEvery)
    {
        FooBitSet bits = new FooBitSet(bytes);
        maxBits = Math.max(0, Math.min(maxBits, bits.getLength()));
        StringBuffer sb = new StringBuffer();
        for (int i = maxBits - 1; i >= 0; i--)
        {
            sb.append(bits.get(i) ? '1' : '0');
            if ((spaceEvery != 0) && (i > 0) && (i % spaceEvery == 0))
            {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    public static String toBitString(byte value, int maxBits)
    {
        return toBitString(new byte[]
                {
                        value
                }, maxBits, 0);
    }

    public static String toBitString(short value, int maxBits)
    {
        return toBitString(value, maxBits, 8);
    }

    public static String toBitString(short value, int maxBits, int spaceEvery)
    {
        return toBitString(FooMemoryStream.getBytes(value), maxBits, spaceEvery);
    }

    public static String toBitString(int value, int maxBits)
    {
        return toBitString(value, maxBits, 8);
    }

    public static String toBitString(int value, int maxBits, int spaceEvery)
    {
        return toBitString(FooMemoryStream.getBytes(value), maxBits, spaceEvery);
    }

    public static String toBitString(long value, int maxBits)
    {
        return toBitString(value, maxBits, 8);
    }

    public static String toBitString(long value, int maxBits, int spaceEvery)
    {
        return toBitString(FooMemoryStream.getBytes(value), maxBits, spaceEvery);
    }

    public static char toChar(boolean value)
    {
        return (value) ? '1' : '0';
    }

    public static String padNumber(long number, char ch, int minimumLength)
    {
        String s = String.valueOf(number);
        while (s.length() < minimumLength)
        {
            s = ch + s;
        }
        return s;
    }

    public static String formatNumber(long number, int minimumLength)
    {
        return padNumber(number, '0', minimumLength);
    }

    public static String formatNumber(double number, int leading, int trailing)
    {
        if (number == Double.NaN || number == Double.NEGATIVE_INFINITY || number == Double.POSITIVE_INFINITY)
        {
            return String.valueOf(number);
        }

        // String.valueOf(1) is guranteed to at least be of the form "1.0"
        String[] parts = split(String.valueOf(number), ".", 0);
        while (parts[0].length() < leading)
        {
            parts[0] = '0' + parts[0];
        }
        while (parts[1].length() < trailing)
        {
            parts[1] = parts[1] + '0';
        }
        parts[1] = parts[1].substring(0, trailing);
        return parts[0] + '.' + parts[1];
    }

    /**
     * Returns a string array that contains the substrings in a source string that are delimited by a specified string.
     *
     * @param source    String to split with the given delimiter.
     * @param separator String that delimits the substrings in the source string.
     * @param limit     Determines the maximum number of entries in the resulting array, and the treatment of trailing
     *                  empty strings.
     *                  <ul>
     *                  <li>For n > 0, the resulting array contains at most n entries. If this is fewer than the number
     *                  of matches, the final entry will contain all remaining input.</li>
     *                  <li>For n < 0, the length of the resulting array is exactly the number of occurrences of the
     *                  Pattern plus one for the text after the final separator. All entries are included.</li>
     *                  <li>For n == 0, the result is as for n < 0, except trailing empty strings will not be returned.
     *                  (Note that the case where the input is itself an empty string is special, as described above,
     *                  and the limit parameter does not apply there.)</li>
     *                  </ul>
     * @return An array whose elements contain the substrings in a source string that are delimited by a separator
     * string.
     */
    public static String[] split(String source, String separator, int limit)
    {
        if (isNullOrEmpty(source) || isNullOrEmpty(separator))
        {
            return new String[]
                    {
                            source
                    };
        }

        int indexB = source.indexOf(separator);
        if (indexB == -1)
        {
            return new String[]
                    {
                            source
                    };
        }

        int indexA = 0;
        String value;
        Vector values = new Vector();

        while (indexB != -1 && (limit < 1 || values.size() < (limit - 1)))
        {
            value = source.substring(indexA, indexB);
            if (!isNullOrEmpty(value) || limit < 0)
            {
                values.addElement(value);
            }
            indexA = indexB + separator.length();
            indexB = source.indexOf(separator, indexA);
        }

        indexB = source.length();
        value = source.substring(indexA, indexB);
        if (!isNullOrEmpty(value) || limit < 0)
        {
            values.addElement(value);
        }

        String[] result = new String[values.size()];
        values.copyInto(result);
        return result;
    }

    public static String replace(String source, String pattern, String replacement)
    {
        return replace(source, pattern, replacement, -1);
    }

    public static String replaceFirst(String source, String pattern, String replacement)
    {
        return replace(source, pattern, replacement, 1);
    }

    public static String replace(String source, String pattern, String replacement, int limit)
    {
        if (source == null)
        {
            return "";
        }

        StringBuffer sb = new StringBuffer();
        int index = -1;
        int fromIndex = 0;
        int count = 0;
        while ((index = source.indexOf(pattern, fromIndex)) != -1 && (limit == -1 || count < limit))
        {
            sb.append(source.substring(fromIndex, index));
            sb.append(replacement);
            fromIndex = index + pattern.length();
            count++;
        }
        sb.append(source.substring(fromIndex));
        return sb.toString();
    }

    public static boolean contains(String s, String cs)
    {
        return s.indexOf(cs) != -1;
    }

    /**
     * @param msElapsed
     * @return HH:MM:SS.MMM
     */
    public static String getTimeElapsedString(long msElapsed)
    {
        long h = 0;
        long m = 0;
        long s = 0;
        if (msElapsed > 0)
        {
            h = (int) (msElapsed / (3600 * 1000));
            msElapsed -= (h * 3600 * 1000);
            m = (int) (msElapsed / (60 * 1000));
            msElapsed -= (m * 60 * 1000);
            s = (int) (msElapsed / 1000);
            msElapsed -= (s * 1000);
        }
        else
        {
            msElapsed = 0;
        }

        return formatNumber(h, 2) + ":" + formatNumber(m, 2) + ":" + formatNumber(s, 2) + "." +
               formatNumber(msElapsed, 3);
    }

    public static String getShortClassName(String className)
    {
        if (isNullOrEmpty(className))
        {
            return "null";
        }
        return className.substring(className.lastIndexOf('.') + 1);
    }

    public static String getShortClassName(Object o)
    {
        Class c = (o == null) ? null : o.getClass();
        return getShortClassName(c);
    }

    public static String getShortClassName(Class c)
    {
        String className = (c == null) ? null : c.getName();
        return getShortClassName(className);
    }

    public static String getMethodName(String methodName)
    {
        if (methodName == null)
        {
            methodName = "()";
        }
        if (methodName.compareTo("()") != 0)
        {
            methodName = "." + methodName;
        }
        return methodName;
    }

    public static String getShortClassAndMethodName(Object o, String methodName)
    {
        return getShortClassName(o) + getMethodName(methodName);
    }

    /**
     * Identical to {@link #repr}, but grammatically intended for Strings.
     *
     * @param value
     * @return "null", or '\"' + value.toString + '\"', or value.toString()
     */
    public static String quote(Object value)
    {
        return repr(value, false);
    }

    /**
     * Identical to {@link #quote}, but grammatically intended for Objects.
     *
     * @param value
     * @return "null", or '\"' + value.toString + '\"', or value.toString()
     */
    public static String repr(Object value)
    {
        return repr(value, false);
    }

    /**
     * @param value
     * @param typeOnly
     * @return "null", or '\"' + value.toString + '\"', or value.toString(), or getShortClassName(value)
     */
    public static String repr(Object value, boolean typeOnly)
    {
        return (value == null) ? "null" : (value instanceof String) ? ('\"' + value.toString() + '\"') //
                : ((typeOnly) ? getShortClassName(value) : value.toString());
    }

    public static String toString(Object[] items)
    {
        StringBuffer sb = new StringBuffer();

        if (items == null)
        {
            sb.append("null");
        }
        else
        {
            sb.append('[');
            for (int i = 0; i < items.length; i++)
            {
                Object item = items[i];
                if (i != 0)
                {
                    sb.append(", ");
                }
                sb.append(quote(item));
            }
            sb.append(']');
        }
        return sb.toString();
    }

    public static String capitalize(String s)
    {
        if (s == null || s.length() == 0)
        {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first))
        {
            return s;
        }
        else
        {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    /**
     * @param flags
     * @return String in the form of "(flag1|flag3|flag5)"
     */
    public static String toFlagString(Vector flags)
    {
        String flag;
        StringBuffer sb = new StringBuffer();
        sb.append('(');
        for (int i = 0; i < flags.size(); i++)
        {
            flag = (String) flags.elementAt(i);
            if (i != 0)
            {
                sb.append('|');
            }
            sb.append(flag);
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * @param str1
     * @param str2
     * @return (str1 == null) ? str1 == str2 : str1.equals(str2)
     */
    public static boolean equals(String str1, String str2)
    {
        return (str1 == null) ? str1 == str2 : str1.equals(str2);
    }

    /*
    public static String plurality(int count)
    {
        return (count == 1) ? "" : "s";
    }

    /* *
     * Generates a plurality of a given name of a given count.<br>
     * Examples:<br>
     * Plurality("item", 0)="items"<br>
     * Plurality("name", 1)="name"<br>
     * Plurality("answer", 42)="answers"<br>
     *
     * @param name  the name to potentially make plural
     * @param count the number of items
     * @return if (count == 1) then return name else return (name + "s")
     * /
    public static String plurality(String name, int count)
    {
        return name + plurality(count);
    }
    */

    public static String getTimeDurationString(Context context, long elapsedMillis)
    {
        return getTimeDurationString(context, elapsedMillis, true);
    }

    public static String getTimeDurationString(Context context, long elapsedMillis, boolean expanded)
    {
        return getTimeDurationString(context, elapsedMillis, expanded, null);
    }

    public static String getTimeDurationString(Context context, long elapsedMillis, TimeUnit minimumTimeUnit)
    {
        return getTimeDurationString(context, elapsedMillis, true, minimumTimeUnit);
    }

    /**
     * @param context
     * @param elapsedMillis
     * @param expanded        if true then formatted as "X days, X hours, X minutes, X seconds, ...", otherwise,
     *                        formatted as "XX minutes", or "XX hours", or "X days"
     * @param minimumTimeUnit must be >= TimeUnit.MILLISECONDS, or null to default to TimeUnit.SECONDS
     * @return
     */
    public static String getTimeDurationString(Context context, long elapsedMillis, boolean expanded, TimeUnit minimumTimeUnit)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context must not be null");
        }

        if (minimumTimeUnit == null)
        {
            minimumTimeUnit = TimeUnit.SECONDS;
        }

        if (TimeUnit.MILLISECONDS.compareTo(minimumTimeUnit) > 0)
        {
            throw new IllegalArgumentException("minimumTimeUnit must be null or >= TimeUnit.MILLISECONDS");
        }

        String result = null;

        if (elapsedMillis >= 0)
        {
            StringBuilder sb = new StringBuilder();

            Resources res = context.getResources();

            if (expanded)
            {
                int days = (int) TimeUnit.MILLISECONDS.toDays(elapsedMillis);
                if (days > 0 && TimeUnit.DAYS.compareTo(minimumTimeUnit) >= 0)
                {
                    elapsedMillis -= TimeUnit.DAYS.toMillis(days);
                    String temp = res.getQuantityString(R.plurals.days, days, days);
                    sb.append(' ').append(temp);
                }

                int hours = (int) TimeUnit.MILLISECONDS.toHours(elapsedMillis);
                if (hours > 0 && TimeUnit.HOURS.compareTo(minimumTimeUnit) >= 0)
                {
                    elapsedMillis -= TimeUnit.HOURS.toMillis(hours);
                    String temp = res.getQuantityString(R.plurals.hours, hours, hours);
                    sb.append(' ').append(temp);
                }

                int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(elapsedMillis);
                if (minutes > 0 && TimeUnit.MINUTES.compareTo(minimumTimeUnit) >= 0)
                {
                    elapsedMillis -= TimeUnit.MINUTES.toMillis(minutes);
                    String temp = res.getQuantityString(R.plurals.minutes, minutes, minutes);
                    sb.append(' ').append(temp);
                }

                int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(elapsedMillis);
                if (seconds > 0 && TimeUnit.SECONDS.compareTo(minimumTimeUnit) >= 0)
                {
                    elapsedMillis -= TimeUnit.SECONDS.toMillis(seconds);
                    String temp = res.getQuantityString(R.plurals.seconds, seconds, seconds);
                    sb.append(' ').append(temp);
                }

                int milliseconds = (int) elapsedMillis;
                if (TimeUnit.MILLISECONDS.compareTo(minimumTimeUnit) >= 0)
                {
                    String temp = res.getQuantityString(R.plurals.milliseconds, milliseconds, milliseconds);
                    sb.append(' ').append(temp);
                }

                if (sb.length() == 0)
                {
                    int timeUnitNameResId;
                    switch (minimumTimeUnit)
                    {
                        case DAYS:
                            timeUnitNameResId = R.plurals.days;
                            break;
                        case HOURS:
                            timeUnitNameResId = R.plurals.hours;
                            break;
                        case MINUTES:
                            timeUnitNameResId = R.plurals.minutes;
                            break;
                        case SECONDS:
                            timeUnitNameResId = R.plurals.seconds;
                            break;
                        case MILLISECONDS:
                        default:
                            timeUnitNameResId = R.plurals.milliseconds;
                            break;
                    }
                    String temp = res.getQuantityString(timeUnitNameResId, 0, 0);
                    sb.append(' ').append(temp);
                }
            }
            else
            {
                int timeUnitNameResId;

                int timeUnitValue = (int) TimeUnit.MILLISECONDS.toDays(elapsedMillis);
                if (timeUnitValue > 0 || TimeUnit.DAYS.compareTo(minimumTimeUnit) <= 0)
                {
                    timeUnitNameResId = R.plurals.days;
                }
                else
                {
                    timeUnitValue = (int) TimeUnit.MILLISECONDS.toHours(elapsedMillis);
                    if (timeUnitValue > 0 || TimeUnit.HOURS.compareTo(minimumTimeUnit) <= 0)
                    {
                        timeUnitNameResId = R.plurals.hours;
                    }
                    else
                    {
                        timeUnitValue = (int) TimeUnit.MILLISECONDS.toMinutes(elapsedMillis);
                        if (timeUnitValue > 0 || TimeUnit.MINUTES.compareTo(minimumTimeUnit) <= 0)
                        {
                            timeUnitNameResId = R.plurals.minutes;
                        }
                        else
                        {
                            timeUnitValue = (int) TimeUnit.MILLISECONDS.toSeconds(elapsedMillis);
                            if (timeUnitValue > 0 || TimeUnit.SECONDS.compareTo(minimumTimeUnit) <= 0)
                            {
                                timeUnitNameResId = R.plurals.seconds;
                            }
                            else
                            {
                                timeUnitValue = (int) elapsedMillis;
                                timeUnitNameResId = R.plurals.milliseconds;
                            }
                        }
                    }
                }

                sb.append(res.getQuantityString(timeUnitNameResId, timeUnitValue, timeUnitValue));
            }

            result = sb.toString().trim();
        }

        return result;
    }

    /**
     * @param msElapsed
     * @return HH:MM:SS.MMM
     */
    public static String getTimeDurationFormattedString(long msElapsed)
    {
        long h = 0;
        long m = 0;
        long s = 0;
        if (msElapsed > 0)
        {
            h = (int) (msElapsed / (3600 * 1000));
            msElapsed -= (h * 3600 * 1000);
            m = (int) (msElapsed / (60 * 1000));
            msElapsed -= (m * 60 * 1000);
            s = (int) (msElapsed / 1000);
            msElapsed -= (s * 1000);
        }
        else
        {
            msElapsed = 0;
        }

        return formatNumber(h, 2) + ":" + formatNumber(m, 2) + ":" + formatNumber(s, 2) + "." +
               formatNumber(msElapsed, 3);
    }

    public static String separateCamelCaseWords(String s)
    {
        StringBuilder sb = new StringBuilder();
        if (s != null)
        {
            String[] parts = s.split("(?=\\p{Lu})");
            for (String part : parts)
            {
                sb.append(part).append(' ');
            }
        }
        return sb.toString().trim();
    }

    public static String bytesToHexString(short value, int maxBytes, boolean lowerCase)
    {
        return bytesToHexString(value, false, maxBytes, lowerCase);
    }

    public static String bytesToHexString(short value, boolean reverse, int maxBytes, boolean lowerCase)
    {
        if (reverse)
        {
            value = Short.reverseBytes(value);
        }
        String s = toHexString(value, maxBytes);
        if (lowerCase)
        {
            s = s.toLowerCase();
        }
        return s;
    }

    public static boolean startsWithVowel(String vowels, String s)
    {
        return s != null && s.matches("^[" + vowels + "].*");
    }
}
