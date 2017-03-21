package com.smartfoo.android.core;

public class FooBoolean
{
    private FooBoolean()
    {
    }

    public static boolean toBoolean(Boolean value)
    {
        return toBoolean(value, false);
    }

    public static boolean toBoolean(Boolean value, boolean defaultValue)
    {
        return value != null ? value : defaultValue;
    }

    public static byte toByte(Boolean value)
    {
        return toByte(value != null && value);
    }

    public static byte toByte(boolean value)
    {
        return (byte) (value ? 1 : 0);
    }

    public static boolean toBoolean(int value)
    {
        return value != 0;
    }

    public static boolean toBoolean(String value)
    {
        if (value == null)
        {
            return false;
        }

        try
        {
            return toBoolean(Integer.parseInt(value));
        }
        catch (NumberFormatException e)
        {
            // ignore
        }

        if ("true".equalsIgnoreCase(value))
        {
            return true;
        }

        if ("false".equalsIgnoreCase(value))
        {
            return false;
        }

        if ("yes".equalsIgnoreCase(value))
        {
            return true;
        }

        if ("no".equalsIgnoreCase(value))
        {
            return false;
        }

        if ("y".equalsIgnoreCase(value))
        {
            return true;
        }

        if ("n".equalsIgnoreCase(value))
        {
            return false;
        }

        throw new IllegalArgumentException("value must be an integer string, \"true\", \"false\", \"yes\", \"no\", \"y\", or \"n\"");
    }

    public static String toString(boolean value)
    {
        return value ? "true" : "false";
    }
}
