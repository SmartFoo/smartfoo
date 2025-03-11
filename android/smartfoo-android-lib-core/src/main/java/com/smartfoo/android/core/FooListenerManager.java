package com.smartfoo.android.core;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.annotations.NonNullNonEmpty;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.reflection.FooReflectionUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/** @noinspection unused*/
public class FooListenerManager<T>
{
    private static final String TAG = FooLog.TAG(FooListenerManager.class);

    @SuppressWarnings("PointlessBooleanExpression")
    private static final boolean VERBOSE_LOG = false && BuildConfig.DEBUG;

    private final String mName;
    private final Set<T> mListeners;
    private final Set<T> mListenersToAdd;
    private final Set<T> mListenersToRemove;

    private boolean mIsTraversingListeners;

    public FooListenerManager(@NonNull Object name)
    {
        this(FooReflectionUtils.getShortClassName(name));
    }

    public FooListenerManager(@NonNullNonEmpty String name)
    {
        mName = FooString.quote(FooRun.toNonNullNonEmpty(name, "name").trim());
        mListeners = new LinkedHashSet<>();
        mListenersToAdd = new LinkedHashSet<>();
        mListenersToRemove = new LinkedHashSet<>();
    }

    @NonNull
    @Override
    public String toString()
    {
        return "{ mName=" + FooString.quote(mName) + ", size()=" + size() + " }";
    }

    public int size()
    {
        int size;
        synchronized (mListeners)
        {
            Set<T> consolidated = new LinkedHashSet<>(mListeners);
            consolidated.addAll(mListenersToAdd);
            consolidated.removeAll(mListenersToRemove);
            size = consolidated.size();
        }
        /*
        if (VERBOSE_LOG)
        {
            FooLog.v(TAG, mName + " size() == " + size);
        }
        */
        return size;
    }

    public boolean isEmpty()
    {
        return size() == 0;
    }

    public void attach(T listener)
    {
        if (VERBOSE_LOG)
        {
            FooLog.v(TAG, mName + " attach(...)");
        }

        if (listener == null)
        {
            return;
        }

        synchronized (mListeners)
        {
            if (mIsTraversingListeners)
            {
                mListenersToAdd.add(listener);
            }
            else
            {
                mListeners.add(listener);
                updateListeners();
            }
        }
    }

    public void detach(T listener)
    {
        if (VERBOSE_LOG)
        {
            FooLog.v(TAG, mName + " detach(...)");
        }

        if (listener == null)
        {
            return;
        }

        synchronized (mListeners)
        {
            if (mIsTraversingListeners)
            {
                mListenersToRemove.add(listener);
            }
            else
            {
                mListeners.remove(listener);
                updateListeners();
            }
        }
    }

    public void clear()
    {
        if (VERBOSE_LOG)
        {
            FooLog.v(TAG, mName + " clear()");
        }
        synchronized (mListeners)
        {
            mListenersToAdd.clear();
            if (mIsTraversingListeners)
            {
                mListenersToRemove.addAll(mListeners);
            }
            else
            {
                mListeners.clear();
                mListenersToRemove.clear();
            }
        }
    }

    public Set<T> beginTraversing()
    {
        if (VERBOSE_LOG)
        {
            FooLog.v(TAG, mName + " beginTraversing()");
        }
        synchronized (mListeners)
        {
            mIsTraversingListeners = true;
            return Collections.unmodifiableSet(mListeners);
        }
    }

    public void endTraversing()
    {
        if (VERBOSE_LOG)
        {
            FooLog.v(TAG, mName + " endTraversing()");
        }
        synchronized (mListeners)
        {
            updateListeners();
            mIsTraversingListeners = false;
        }
    }

    private void updateListeners()
    {
        if (VERBOSE_LOG)
        {
            FooLog.v(TAG, mName + " updateListeners()");
        }
        synchronized (mListeners)
        {
            Iterator<T> it = mListenersToAdd.iterator();
            while (it.hasNext())
            {
                mListeners.add(it.next());
                it.remove();
            }
            it = mListenersToRemove.iterator();
            while (it.hasNext())
            {
                mListeners.remove(it.next());
                it.remove();
            }

            onListenersUpdated(mListeners.size());
        }
    }

    protected void onListenersUpdated(int listenersSize)
    {
    }
}
