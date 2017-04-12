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
            Set<T> consolidated = new LinkedHashSet<>(mListeners);
            consolidated.addAll(mListenersToAdd);
            consolidated.removeAll(mListenersToRemove);
            return consolidated.size();
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
                updateListeners();
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
                updateListeners();
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
