package com.smartfoo.android.core.collections

import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.os.PersistableBundle
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import java.io.Serializable

@Suppress("unused")
class FooBundleBuilder {
    private val mBundle = Bundle()

    fun build(): Bundle {
        return mBundle
    }

    fun putBoolean(key: String?, value: Boolean): FooBundleBuilder {
        mBundle.putBoolean(key, value)
        return this
    }

    fun putBooleanArray(key: String?, value: BooleanArray?): FooBundleBuilder {
        mBundle.putBooleanArray(key, value)
        return this
    }

    fun putByte(key: String?, value: Byte): FooBundleBuilder {
        mBundle.putByte(key, value)
        return this
    }

    fun putByteArray(key: String?, value: ByteArray?): FooBundleBuilder {
        mBundle.putByteArray(key, value)
        return this
    }

    fun putChar(key: String?, value: Char): FooBundleBuilder {
        mBundle.putChar(key, value)
        return this
    }

    fun putCharArray(key: String?, value: CharArray?): FooBundleBuilder {
        mBundle.putCharArray(key, value)
        return this
    }

    fun putShort(key: String?, value: Short): FooBundleBuilder {
        mBundle.putShort(key, value)
        return this
    }

    fun putShortArray(key: String?, value: ShortArray?): FooBundleBuilder {
        mBundle.putShortArray(key, value)
        return this
    }

    fun putInt(key: String?, value: Int): FooBundleBuilder {
        mBundle.putInt(key, value)
        return this
    }

    fun putIntArray(key: String?, value: IntArray?): FooBundleBuilder {
        mBundle.putIntArray(key, value)
        return this
    }

    fun putIntegerArrayList(key: String?, value: ArrayList<Int?>?): FooBundleBuilder {
        mBundle.putIntegerArrayList(key, value)
        return this
    }

    fun putLong(key: String?, value: Long): FooBundleBuilder {
        mBundle.putLong(key, value)
        return this
    }

    fun putLongArray(key: String?, value: LongArray?): FooBundleBuilder {
        mBundle.putLongArray(key, value)
        return this
    }

    fun putFloat(key: String?, value: Float): FooBundleBuilder {
        mBundle.putFloat(key, value)
        return this
    }

    fun putFloatArray(key: String?, value: FloatArray?): FooBundleBuilder {
        mBundle.putFloatArray(key, value)
        return this
    }

    fun putDouble(key: String?, value: Double): FooBundleBuilder {
        mBundle.putDouble(key, value)
        return this
    }

    fun putDoubleArray(key: String?, value: DoubleArray?): FooBundleBuilder {
        mBundle.putDoubleArray(key, value)
        return this
    }

    fun putString(key: String?, value: String?): FooBundleBuilder {
        mBundle.putString(key, value)
        return this
    }

    fun putStringArray(key: String?, value: Array<String?>?): FooBundleBuilder {
        mBundle.putStringArray(key, value)
        return this
    }

    fun putStringArrayList(key: String?, value: ArrayList<String?>?): FooBundleBuilder {
        mBundle.putStringArrayList(key, value)
        return this
    }

    fun putCharSequence(key: String?, value: CharSequence?): FooBundleBuilder {
        mBundle.putCharSequence(key, value)
        return this
    }

    fun putCharSequenceArray(key: String?, value: Array<CharSequence?>?): FooBundleBuilder {
        mBundle.putCharSequenceArray(key, value)
        return this
    }

    fun putCharSequenceArrayList(key: String?, value: ArrayList<CharSequence?>?): FooBundleBuilder {
        mBundle.putCharSequenceArrayList(key, value)
        return this
    }

    fun putAll(value: Bundle?): FooBundleBuilder {
        mBundle.putAll(value)
        return this
    }

    fun putBinder(key: String?, value: IBinder?): FooBundleBuilder {
        mBundle.putBinder(key, value)
        return this
    }

    fun putBundle(key: String?, value: Bundle?): FooBundleBuilder {
        mBundle.putBundle(key, value)
        return this
    }

    fun putParcelable(key: String?, value: Parcelable?): FooBundleBuilder {
        mBundle.putParcelable(key, value)
        return this
    }

    fun putParcelableArray(key: String?, value: Array<Parcelable?>?): FooBundleBuilder {
        mBundle.putParcelableArray(key, value)
        return this
    }

    fun putParcelableArrayList(key: String?, value: ArrayList<out Parcelable?>?): FooBundleBuilder {
        mBundle.putParcelableArrayList(key, value)
        return this
    }

    fun putSerializable(key: String?, value: Serializable?): FooBundleBuilder {
        mBundle.putSerializable(key, value)
        return this
    }

    fun putSparseParcelableArray(
        key: String?,
        value: SparseArray<out Parcelable?>?
    ): FooBundleBuilder {
        mBundle.putSparseParcelableArray(key, value)
        return this
    }

    //
    // LOLLIPOP
    //
    fun putAll(value: PersistableBundle?): FooBundleBuilder {
        mBundle.putAll(value)
        return this
    }

    fun putSize(key: String?, value: Size?): FooBundleBuilder {
        mBundle.putSize(key, value)
        return this
    }

    fun putSize(key: String?, value: SizeF?): FooBundleBuilder {
        mBundle.putSizeF(key, value)
        return this
    }
}
