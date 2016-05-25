package com.smartfoo.android.core.collections;

import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;

import java.io.Serializable;
import java.util.ArrayList;

public class FooBundleBuilder
{
    private final Bundle mBundle;

    public FooBundleBuilder()
    {
        mBundle = new Bundle();
    }

    public Bundle build()
    {
        return mBundle;
    }

    public FooBundleBuilder putBoolean(String key, boolean value)
    {
        mBundle.putBoolean(key, value);
        return this;
    }

    public FooBundleBuilder putBooleanArray(String key, boolean[] value)
    {
        mBundle.putBooleanArray(key, value);
        return this;
    }

    public FooBundleBuilder putByte(String key, byte value)
    {
        mBundle.putByte(key, value);
        return this;
    }

    public FooBundleBuilder putByteArray(String key, byte[] value)
    {
        mBundle.putByteArray(key, value);
        return this;
    }

    public FooBundleBuilder putChar(String key, char value)
    {
        mBundle.putChar(key, value);
        return this;
    }

    public FooBundleBuilder putCharArray(String key, char[] value)
    {
        mBundle.putCharArray(key, value);
        return this;
    }

    public FooBundleBuilder putShort(String key, short value)
    {
        mBundle.putShort(key, value);
        return this;
    }

    public FooBundleBuilder putShortArray(String key, short[] value)
    {
        mBundle.putShortArray(key, value);
        return this;
    }

    public FooBundleBuilder putInt(String key, int value)
    {
        mBundle.putInt(key, value);
        return this;
    }

    public FooBundleBuilder putIntArray(String key, int[] value)
    {
        mBundle.putIntArray(key, value);
        return this;
    }

    public FooBundleBuilder putIntegerArrayList(String key, ArrayList<Integer> value)
    {
        mBundle.putIntegerArrayList(key, value);
        return this;
    }

    public FooBundleBuilder putLong(String key, long value)
    {
        mBundle.putLong(key, value);
        return this;
    }

    public FooBundleBuilder putLongArray(String key, long[] value)
    {
        mBundle.putLongArray(key, value);
        return this;
    }

    public FooBundleBuilder putFloat(String key, float value)
    {
        mBundle.putFloat(key, value);
        return this;
    }

    public FooBundleBuilder putFloatArray(String key, float[] value)
    {
        mBundle.putFloatArray(key, value);
        return this;
    }

    public FooBundleBuilder putDouble(String key, double value)
    {
        mBundle.putDouble(key, value);
        return this;
    }

    public FooBundleBuilder putDoubleArray(String key, double[] value)
    {
        mBundle.putDoubleArray(key, value);
        return this;
    }

    public FooBundleBuilder putString(String key, String value)
    {
        mBundle.putString(key, value);
        return this;
    }

    public FooBundleBuilder putStringArray(String key, String[] value)
    {
        mBundle.putStringArray(key, value);
        return this;
    }

    public FooBundleBuilder putStringArrayList(String key, ArrayList<String> value)
    {
        mBundle.putStringArrayList(key, value);
        return this;
    }

    public FooBundleBuilder putCharSequence(String key, CharSequence value)
    {
        mBundle.putCharSequence(key, value);
        return this;
    }

    public FooBundleBuilder putCharSequenceArray(String key, CharSequence[] value)
    {
        mBundle.putCharSequenceArray(key, value);
        return this;
    }

    public FooBundleBuilder putCharSequenceArrayList(String key, ArrayList<CharSequence> value)
    {
        mBundle.putCharSequenceArrayList(key, value);
        return this;
    }

    public FooBundleBuilder putAll(Bundle value)
    {
        mBundle.putAll(value);
        return this;
    }

    public FooBundleBuilder putBinder(String key, IBinder value)
    {
        mBundle.putBinder(key, value);
        return this;
    }

    public FooBundleBuilder putBundle(String key, Bundle value)
    {
        mBundle.putBundle(key, value);
        return this;
    }

    public FooBundleBuilder putParcelable(String key, Parcelable value)
    {
        mBundle.putParcelable(key, value);
        return this;
    }

    public FooBundleBuilder putParcelableArray(String key, Parcelable[] value)
    {
        mBundle.putParcelableArray(key, value);
        return this;
    }

    public FooBundleBuilder putParcelableArrayList(String key, ArrayList<? extends Parcelable> value)
    {
        mBundle.putParcelableArrayList(key, value);
        return this;
    }

    public FooBundleBuilder putSerializable(String key, Serializable value)
    {
        mBundle.putSerializable(key, value);
        return this;
    }

    public FooBundleBuilder putSparseParcelableArray(String key, SparseArray<? extends Parcelable> value)
    {
        mBundle.putSparseParcelableArray(key, value);
        return this;
    }

    //
    // LOLLIPOP
    //

    @TargetApi(VERSION_CODES.LOLLIPOP)
    public FooBundleBuilder putAll(PersistableBundle value)
    {
        mBundle.putAll(value);
        return this;
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    public FooBundleBuilder putSize(String key, Size value)
    {
        mBundle.putSize(key, value);
        return this;
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    public FooBundleBuilder putSize(String key, SizeF value)
    {
        mBundle.putSizeF(key, value);
        return this;
    }
}
