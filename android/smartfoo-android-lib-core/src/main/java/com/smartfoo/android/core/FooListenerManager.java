package com.smartfoo.android.core;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class FooListenerManager<T>
{
    private final Set<T> mListeners;
    private final Set<T> mListenersToAdd;
    private final Set<T> mListenersToRemove;

    private boolean mIsTraversingListeners;

    public FooListenerManager()
    {
        mListeners = new LinkedHashSet<>();
        mListenersToAdd = new LinkedHashSet<>();
        mListenersToRemove = new LinkedHashSet<>();
    }

    public boolean isEmpty()
    {
        synchronized (mListeners)
        {
            return mListeners.size() == 0;
        }
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
            return mListeners;
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
