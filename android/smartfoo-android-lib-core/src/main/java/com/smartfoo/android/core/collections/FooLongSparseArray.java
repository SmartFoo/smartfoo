package com.smartfoo.android.core.collections;

import android.util.Log;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Mutation of {@link android.support.v4.util.LongSparseArray} that changes the following:
 * <ul>
 * <li>{@link #delete(long)} calls {@link #remove(long)}, instead of the other way around</li>
 * <li>{@link #remove(long)} returns the removed value (or null)</li>
 * <li>{@link #removeAt(int)} returns the removed value (or null)</li>
 * <li>{@link #put(long, Object)} returns the index returned from {@link ContainerHelpers#binarySearch(long[], int,
 * long)}</li>
 * <li>Adds {@link #iterateKeys()}</li>
 * <li>Adds {@link #iterateValues()}</li>
 * <li>throws IllegalArgumentException for null values</li>
 * <li>Extra debug logging</li>
 * </ul>
 * <p/>
 * One note to the original code's below comment is that the advantage of using this type of collection is that the
 * keys are always sorted!
 * <p/>
 * SparseArray mapping longs to Objects, a version of the platform's {@link android.util.LongSparseArray} that can be
 * used on older versions of the platform.  Unlike a normal array of Objects, there can be gaps in the indices.  It is
 * intended to be more memory efficient than using a HashMap to map Longs to Objects, both because it avoids
 * auto-boxing keys and its data structure doesn't rely on an extra entry object for each mapping.
 * <p/>
 * <p>Note that this container keeps its mappings in an array data structure, using a binary search to find keys.  The
 * implementation is not intended to be appropriate for data structures that may contain large numbers of items.  It is
 * generally slower than a traditional HashMap, since lookups require a binary search and adds and removes require
 * inserting and deleting entries in the array.  For containers holding up to hundreds of items, the performance
 * difference is not significant, less than 50%.</p>
 * <p/>
 * <p>To help with performance, the container includes an optimization when removing keys: instead of compacting its
 * array immediately, it leaves the removed entry marked as deleted.  The entry can then be re-used for the same key,
 * or compacted later in a single garbage collection step of all removed entries.  This garbage collection will need to
 * be performed at any time the array needs to be grown or the the map size or entry values are retrieved.</p>
 */
public class FooLongSparseArray<E>
        implements Cloneable
{
    private static final String TAG = FooLog.TAG(FooLongSparseArray.class);

    /**
     * From {@link android.support.v4.util.ContainerHelpers}.
     * <p/>
     * Copied here because ContainerHelpers is only package accessible.
     */
    static class ContainerHelpers
    {
        private ContainerHelpers()
        {
        }

        static final long[]   EMPTY_LONGS   = new long[0];
        static final Object[] EMPTY_OBJECTS = new Object[0];

        static int idealLongArraySize(int need)
        {
            return idealByteArraySize(need * 8) / 8;
        }

        static int idealByteArraySize(int need)
        {
            for (int i = 4; i < 32; i++)
            {
                if (need <= (1 << i) - 12)
                {
                    return (1 << i) - 12;
                }
            }

            return need;
        }

        /**
         * This is Arrays.binarySearch(), but doesn't do any argument validation.
         * <p/>
         * Performs a binary search for {@code value} in the ascending sorted array {@code array},
         * in the range specified by fromIndex (inclusive) and toIndex (exclusive).
         * Searching in an unsorted array has an undefined result. It's also undefined which element
         * is found if there are multiple occurrences of the same element.
         *
         * @param array the sorted array to search.
         * @param size  the exclusive end index.
         * @param value the element to find.
         * @return the non-negative index of the element, or a negative index which
         * is {@code -index - 1} where the element would be inserted.
         * @throws IllegalArgumentException       if {@code startIndex > endIndex}
         * @throws ArrayIndexOutOfBoundsException if {@code startIndex < 0 || endIndex > array.length}
         */
        static int binarySearch(long[] array, int size, long value)
        {
            int lo = 0;
            int hi = size - 1;

            while (lo <= hi)
            {
                final int mid = (lo + hi) >>> 1;
                final long midVal = array[mid];

                if (midVal < value)
                {
                    lo = mid + 1;
                }
                else if (midVal > value)
                {
                    hi = mid - 1;
                }
                else
                {
                    return mid;  // value found
                }
            }

            return ~lo;  // value not present
        }
    }

    private static final Object DELETED = new Object();

    private final String mDebugName;

    private int      mSize;
    private boolean  mGarbage;
    private long[]   mKeys;
    private Object[] mValues;

    public FooLongSparseArray()
    {
        this(null);
    }

    /**
     * Creates a new LongSparseArray containing no mappings.
     */
    public FooLongSparseArray(String debugName)
    {
        this(debugName, 10);
    }

    /**
     * Creates a new LongSparseArray containing no mappings that will not
     * require any additional memory allocation to store the specified
     * number of mappings.  If you supply an initial capacity of 0, the
     * sparse array will be initialized with a light-weight representation
     * not requiring any additional array allocations.
     */
    public FooLongSparseArray(String debugName, int initialCapacity)
    {
        if (FooString.isNullOrEmpty(debugName))
        {
            debugName = null;
        }
        mDebugName = debugName;

        if (initialCapacity == 0)
        {
            mKeys = ContainerHelpers.EMPTY_LONGS;
            mValues = ContainerHelpers.EMPTY_OBJECTS;
        }
        else
        {
            initialCapacity = ContainerHelpers.idealLongArraySize(initialCapacity);
            mKeys = new long[initialCapacity];
            mValues = new Object[initialCapacity];
        }
        mSize = 0;
    }

    @Override
    @SuppressWarnings({ "unchecked", "CloneDoesntDeclareCloneNotSupportedException" })
    public FooLongSparseArray<E> clone()
    {
        FooLongSparseArray<E> clone = null;
        try
        {
            //noinspection unchecked
            clone = (FooLongSparseArray<E>) super.clone();
            clone.mKeys = mKeys.clone();
            clone.mValues = mValues.clone();
        }
        catch (CloneNotSupportedException cnse)
        {
            /* ignore */
        }
        return clone;
    }

    /**
     * Gets the Object mapped from the specified key, or <code>null</code>
     * if no such mapping has been made.
     */
    public E get(long key)
    {
        return get(key, null);
    }

    /**
     * Gets the Object mapped from the specified key, or the specified Object
     * if no such mapping has been made.
     */
    public E get(long key, E valueIfKeyNotFound)
    {
        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " BEFORE get(" + key + "): " + toDebugString());
        }

        int i = ContainerHelpers.binarySearch(mKeys, mSize, key);

        E value;

        if (i < 0 || mValues[i] == DELETED)
        {
            value = valueIfKeyNotFound;
        }
        else
        {
            //noinspection unchecked
            value = (E) mValues[i];
        }

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " get(" + key + "): value=" + value);
            Log.e(TAG, '#' + mDebugName + "  AFTER get(" + key + "): " + toDebugString());
        }

        return value;
    }

    /**
     * Alias for {@link #remove(long)}.
     *
     * @deprecated Use {@link #remove(long)}
     */
    public void delete(long key)
    {
        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " BEFORE delete(" + key + "): " + toDebugString());
        }

        remove(key);

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + "  AFTER delete(" + key + "): " + toDebugString());
        }
    }

    /**
     * Removes the mapping from the specified key, if there was any.
     *
     * @return the removed value, or null
     */
    public E remove(long key)
    {
        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " BEFORE remove(" + key + "): " + toDebugString());
        }

        E value = null;

        int index = ContainerHelpers.binarySearch(mKeys, mSize, key);

        if (index >= 0)
        {
            value = removeAt(index);
        }

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + "  AFTER remove(" + key + "): " + toDebugString());
        }

        return value;
    }

    /**
     * Removes the mapping at the specified index.
     *
     * @return the removed value, or null
     */
    public E removeAt(int index)
    {
        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " BEFORE removeAt(" + index + "): " + toDebugString());
        }

        E value = null;

        if (mValues[index] != DELETED)
        {
            //noinspection unchecked
            value = (E) mValues[index];

            mValues[index] = DELETED;
            mGarbage = true;
        }

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + "  AFTER removeAt(" + index + "): " + toDebugString());
        }

        return value;
    }

    private void gc()
    {
        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " BEFORE gc(): " + toDebugString());
        }

        int n = mSize;
        int o = 0;
        long[] keys = mKeys;
        Object[] values = mValues;

        for (int i = 0; i < n; i++)
        {
            Object val = values[i];

            if (val != DELETED)
            {
                if (i != o)
                {
                    keys[o] = keys[i];
                    values[o] = val;
                    values[i] = null;
                }

                o++;
            }
        }

        mGarbage = false;
        mSize = o;

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + "  AFTER gc(): " + toDebugString());
        }
    }

    /**
     * Adds a mapping from the specified key to the specified value,
     * replacing the previous mapping from the specified key if there
     * was one.
     *
     * @return the non-negative index of the updated element, or the negative index which
     * is {@code -index - 1} where of the newly inserted element.
     */
    public int put(long key, E value)
    {
        if (value == null)
        {
            throw new IllegalArgumentException("value must not be null");
        }

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " BEFORE put(" + key + ", " + value + "): " + toDebugString());
        }

        int i = ContainerHelpers.binarySearch(mKeys, mSize, key);

        if (i >= 0)
        {
            mValues[i] = value;
        }
        else
        {
            i = ~i;

            if (i < mSize && mValues[i] == DELETED)
            {
                mKeys[i] = key;
                mValues[i] = value;
            }
            else
            {
                if (mGarbage && mSize >= mKeys.length)
                {
                    gc();

                    // Search again because indices may have changed.
                    i = ~ContainerHelpers.binarySearch(mKeys, mSize, key);
                }

                if (mSize >= mKeys.length)
                {
                    int n = ContainerHelpers.idealLongArraySize(mSize + 1);

                    long[] nkeys = new long[n];
                    Object[] nvalues = new Object[n];

                    // Log.e("SparseArray", "grow " + mKeys.length + " to " + n);
                    System.arraycopy(mKeys, 0, nkeys, 0, mKeys.length);
                    System.arraycopy(mValues, 0, nvalues, 0, mValues.length);

                    mKeys = nkeys;
                    mValues = nvalues;
                }

                if (mSize - i != 0)
                {
                    // Log.e("SparseArray", "move " + (mSize - i));
                    System.arraycopy(mKeys, i, mKeys, i + 1, mSize - i);
                    System.arraycopy(mValues, i, mValues, i + 1, mSize - i);
                }

                mKeys[i] = key;
                mValues[i] = value;
                mSize++;
            }

            i = ~i;
        }

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + "  AFTER put(" + key + ", " + value + "): " + toDebugString());
        }

        return i;
    }

    /**
     * Returns the number of key-value mappings that this LongSparseArray
     * currently stores.
     */
    public int size()
    {
        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " BEFORE size(): " + toDebugString());
        }

        if (mGarbage)
        {
            gc();
        }

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + "  AFTER size(): " + toDebugString());
        }

        return mSize;
    }

    /**
     * Given an index in the range <code>0...size()-1</code>, returns
     * the key from the <code>index</code>th key-value mapping that this
     * LongSparseArray stores.
     */
    public long keyAt(int index)
    {
        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " BEFORE keyAt(" + index + "): " + toDebugString());
        }

        if (mGarbage)
        {
            gc();
        }

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + "  AFTER keyAt(" + index + "): " + toDebugString());
        }

        return mKeys[index];
    }

    /**
     * Given an index in the range <code>0...size()-1</code>, returns
     * the value from the <code>index</code>th key-value mapping that this
     * LongSparseArray stores.
     */
    public E valueAt(int index)
    {
        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " BEFORE valueAt(" + index + "): " + toDebugString());
        }

        if (mGarbage)
        {
            gc();
        }

        //noinspection unchecked
        E value = (E) mValues[index];

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + "  valueAt(" + index + "): value=" + value);
            Log.e(TAG, '#' + mDebugName + "  AFTER valueAt(" + index + "): " + toDebugString());
        }

        return value;
    }

    /**
     * Given an index in the range <code>0...size()-1</code>, sets a new
     * value for the <code>index</code>th key-value mapping that this
     * LongSparseArray stores.
     */
    public void setValueAt(int index, E value)
    {
        if (value == null)
        {
            throw new IllegalArgumentException("value must not be null");
        }

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " BEFORE setValueAt(" + index + "): " + toDebugString());
        }

        if (mGarbage)
        {
            gc();
        }

        mValues[index] = value;

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + "  AFTER setValueAt(" + index + "): " + toDebugString());
        }
    }

    /**
     * Returns the index for which {@link #keyAt} would return the
     * specified key, or a negative number if the specified
     * key is not mapped.
     */
    public int indexOfKey(long key)
    {
        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " BEFORE indexOfKey(" + key + "): " + toDebugString());
        }

        if (mGarbage)
        {
            gc();
        }

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + "  AFTER indexOfKey(" + key + "): " + toDebugString());
        }

        return ContainerHelpers.binarySearch(mKeys, mSize, key);
    }

    /**
     * Returns an index for which {@link #valueAt} would return the
     * specified key, or a negative number if no keys map to the
     * specified value.
     * Beware that this is a linear search, unlike lookups by key,
     * and that multiple keys can map to the same value and this will
     * find only one of them.
     */
    public int indexOfValue(E value)
    {
        if (value == null)
        {
            throw new IllegalArgumentException("value must not be null");
        }

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " BEFORE indexOfValue(" + value + "): " + toDebugString());
        }

        if (mGarbage)
        {
            gc();
        }

        for (int i = 0; i < mSize; i++)
        {
            if (mValues[i] == value)
            {
                return i;
            }
        }

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " AFTER indexOfValue(" + value + "): " + toDebugString());
        }

        return -1;
    }

    /**
     * Removes all key-value mappings from this LongSparseArray.
     */
    public void clear()
    {
        int n = mSize;
        Object[] values = mValues;

        for (int i = 0; i < n; i++)
        {
            values[i] = null;
        }

        mSize = 0;
        mGarbage = false;
    }

    /**
     * Puts a key/value pair into the array, optimizing for the case where
     * the key is greater than all existing keys in the array.
     */
    public void append(long key, E value)
    {
        if (value == null)
        {
            throw new IllegalArgumentException("value must not be null");
        }

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " BEFORE append(" + key + ", " + value + "): " + toDebugString());
        }

        if (mSize != 0 && key <= mKeys[mSize - 1])
        {
            put(key, value);
        }
        else
        {
            if (mGarbage && mSize >= mKeys.length)
            {
                gc();
            }

            int pos = mSize;
            if (pos >= mKeys.length)
            {
                int n = ContainerHelpers.idealLongArraySize(pos + 1);

                long[] nkeys = new long[n];
                Object[] nvalues = new Object[n];

                // Log.e("SparseArray", "grow " + mKeys.length + " to " + n);
                System.arraycopy(mKeys, 0, nkeys, 0, mKeys.length);
                System.arraycopy(mValues, 0, nvalues, 0, mValues.length);

                mKeys = nkeys;
                mValues = nvalues;
            }

            mKeys[pos] = key;
            mValues[pos] = value;
            mSize = pos + 1;
        }

        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + "  AFTER append(" + key + ", " + value + "): " + toDebugString());
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p>This implementation composes a string by iterating over its mappings. If
     * this map contains itself as a value, the string "(this Map)"
     * will appear in its place.
     */
    @Override
    public String toString()
    {
        if (size() <= 0)
        {
            return "{}";
        }

        StringBuilder buffer = new StringBuilder(mSize * 28);
        buffer.append('{');
        for (int i = 0; i < mSize; i++)
        {
            if (i > 0)
            {
                buffer.append(", ");
            }
            long key = keyAt(i);
            buffer.append(key);
            buffer.append('=');
            Object value = valueAt(i);
            if (value != this)
            {
                buffer.append(value);
            }
            else
            {
                buffer.append("(this Map)");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }

    public String toDebugString()
    {
        return toDebugString(false);
    }

    public String toDebugString(boolean simple)
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("\n{")
                .append("\n\tmSize=").append(mSize).append(',')
                .append("\n\tmGarbage=").append(mGarbage).append(',');
        buffer.append("\n\tmKeys=\n\t[");
        for (int i = 0; i < mKeys.length; i++)
        {
            if (i != 0)
            {
                buffer.append(',');
            }
            buffer.append("\n\t\t").append(mKeys[i]);
        }
        buffer.append("\n\t],");
        buffer.append("\n\tmValues=\n\t[");
        for (int i = 0; i < mValues.length; i++)
        {
            if (i != 0)
            {
                buffer.append(",");
            }
            Object value = mValues[i];
            buffer.append("\n\t\t");
            if (value != this)
            {
                if (value == DELETED)
                {
                    buffer.append("DELETED");
                }
                else
                {
                    if (simple)
                    {
                        buffer.append(value.getClass().getSimpleName())
                                .append('@')
                                .append(Integer.toHexString(value.hashCode()));
                    }
                    else
                    {
                        buffer.append(value);
                    }
                }
            }
            else
            {
                buffer.append("(this Map)");
            }
        }
        buffer.append("\n\t]");
        buffer.append("\n}");
        return buffer.toString();
    }

    //
    // New iterators...
    //

    public Iterator<Long> iterateKeys()
    {
        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " iterateKeys(): " + toDebugString());
        }
        return new SparseArrayKeysIterator<>(this);
    }

    public Iterator<E> iterateValues()
    {
        if (mDebugName != null)
        {
            Log.e(TAG, '#' + mDebugName + " iterateValues(): " + toDebugString());
        }
        return new SparseArrayValuesIterator<>(this);
    }

    // TODO:(pv) Implement this to use mKeys directly so that it doesn't cause a gc() hit for every iteration.
    private static final class SparseArrayKeysIterator<E>
            implements Iterator<Long>
    {
        private final FooLongSparseArray<E> mArray;

        private int     mIndex;
        private boolean mCanRemove;

        private SparseArrayKeysIterator(FooLongSparseArray<E> array)
        {
            mArray = array;
        }

        @Override
        public boolean hasNext()
        {
            //
            // NOTE:(pv) mArray.size() causes mArray.gc() to be called
            //
            return mIndex < mArray.size();
        }

        @Override
        public Long next()
        {
            if (mArray.mDebugName != null)
            {
                Log.e(TAG, '#' + mArray.mDebugName + " next(): " + mArray.toString());
            }
            //
            // NOTE:(pv) hasNext() causes mArray.gc() to be called
            //
            if (hasNext())
            {
                mCanRemove = true;
                //
                // NOTE:(pv) mArray.keyAt(...) causes mArray.gc() to be called
                //
                return mArray.keyAt(mIndex++);
            }
            else
            {
                throw new NoSuchElementException("No more elements");
            }
        }

        @Override
        public void remove()
        {
            //if (mArray.mDebugName != null)
            //{
            //    Log.e(TAG, '#' + mArray.mDebugName + " remove(): BEFORE " + mArray.toString());
            //}
            if (mCanRemove)
            {
                mCanRemove = false;
                mArray.removeAt(--mIndex);
            }
            else
            {
                throw new IllegalStateException("next() must be called");
            }
            //if (mArray.mDebugName != null)
            //{
            //    Log.e(TAG, '#' + mArray.mDebugName + " remove():  AFTER " + mArray.toString());
            //}
        }
    }

    // TODO:(pv) Implement this to use mValues directly so that it doesn't cause a gc() hit for every iteration.
    private static final class SparseArrayValuesIterator<E>
            implements Iterator<E>
    {
        private final FooLongSparseArray<E> mArray;

        private int     mIndex;
        private boolean mCanRemove;

        private SparseArrayValuesIterator(FooLongSparseArray<E> array)
        {
            mArray = array;
        }

        @Override
        public boolean hasNext()
        {
            //
            // NOTE:(pv) mArray.size() causes mArray.gc() to be called
            //
            return mIndex < mArray.size();
        }

        @Override
        public E next()
        {
            if (mArray.mDebugName != null)
            {
                Log.e(TAG, '#' + mArray.mDebugName + " next(): " + mArray.toString());
            }
            //
            // NOTE:(pv) hasNext() causes mArray.gc() to be called
            //
            if (hasNext())
            {
                mCanRemove = true;
                //
                // NOTE:(pv) mArray.valueAt(...) causes mArray.gc() to be called
                //
                return mArray.valueAt(mIndex++);
            }
            else
            {
                throw new NoSuchElementException("No more elements");
            }
        }

        @Override
        public void remove()
        {
            //if (mArray.mDebugName != null)
            //{
            //    Log.e(TAG, '#' + mArray.mDebugName + " remove(): BEFORE " + mArray.toString());
            //}
            if (mCanRemove)
            {
                mCanRemove = false;
                mArray.removeAt(--mIndex);
            }
            else
            {
                throw new IllegalStateException("next() must be called");
            }
            //if (mArray.mDebugName != null)
            //{
            //    Log.e(TAG, '#' + mArray.mDebugName + " remove():  AFTER " + mArray.toString());
            //}
        }
    }
}


