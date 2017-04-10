package com.smartfoo.android.core;

import com.smartfoo.android.core.logging.FooLog;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class FooListenerManager<T>
{
    private static final String TAG = FooLog.TAG(FooListenerManager.class);

    private final String mName;
    private final Set<T> mListeners;
    private final Set<T> mListenersToAdd;
    private final Set<T> mListenersToRemove;

    private boolean mIsTraversingListeners;

    public FooListenerManager()
    {
        this(null);
    }

    public FooListenerManager(String name)
    {
        mName = name != null ? name.trim() : null;
        mListeners = new LinkedHashSet<>();
        mListenersToAdd = new LinkedHashSet<>();
        mListenersToRemove = new LinkedHashSet<>();
    }

    private String getLogPrefix()
    {
        return mName != null ? (mName + ' ') : "";
    }

    public int size()
    {
        synchronized (mListeners)
        {
            if (mIsTraversingListeners)
            {
                throw new IllegalStateException("must not call size() between beginTraversing() and endTraversing()");
            }
            updateListeners();
            return mListeners.size();
        }
    }

    public boolean isEmpty()
    {
        return size() == 0;
    }

    public void attach(T listener)
    {
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
            }
        }
    }

    public void detach(T listener)
    {
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
            }
        }
    }

    public void clear()
    {
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
        synchronized (mListeners)
        {
            mIsTraversingListeners = true;
            return Collections.unmodifiableSet(mListeners);
        }
    }

    public void endTraversing()
    {
        synchronized (mListeners)
        {
            updateListeners();
            mIsTraversingListeners = false;
        }
    }

    private void updateListeners()
    {
        synchronized (mListeners)
        {
            for (Iterator<T> iterator = mListenersToAdd.iterator(); iterator.hasNext(); )
            {
                T listener = iterator.next();
                iterator.remove();
                mListeners.add(listener);
            }
            for (Iterator<T> iterator = mListenersToRemove.iterator(); iterator.hasNext(); )
            {
                T listener = iterator.next();
                iterator.remove();
                mListeners.remove(listener);
            }
        }
    }
}
