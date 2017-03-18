package com.smartfoo.android.core;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FooListenerManager<T>
{
    private final List<T> mListeners;
    private final List<T> mListenersToAdd;
    private final List<T> mListenersToRemove;

    private boolean mIsTraversingListeners;

    public FooListenerManager()
    {
        mListeners = new LinkedList<>();
        mListenersToAdd = new LinkedList<>();
        mListenersToRemove = new LinkedList<>();
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

    public List<T> beginTraversing()
    {
        synchronized (mListeners)
        {
            mIsTraversingListeners = true;
            return Collections.unmodifiableList(mListeners);
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
