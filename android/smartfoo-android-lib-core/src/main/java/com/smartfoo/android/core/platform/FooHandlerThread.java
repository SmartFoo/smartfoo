package com.smartfoo.android.core.platform;

import android.os.HandlerThread;

import com.smartfoo.android.core.logging.FooLog;

/**
 * Replacement for {@link HandlerThread} to fix start-up race conditions.
 */
public class FooHandlerThread
        extends HandlerThread
{
    private static final String TAG = FooLog.TAG(FooHandlerThread.class);

    /*
    private final Object mSyncLock = new Object();
    private int mPriority;
    int mTid = -1;
    Looper mLooper;
    */

    public FooHandlerThread(String threadName)
    {
        super(threadName);
    }

    public FooHandlerThread(String threadName, int priority)
    {
        super(threadName, priority);
    }

    @Override
    protected void onLooperPrepared()
    {
        String threadName = getName();
        FooLog.v(TAG, threadName + " +onLooperPrepared()");
        super.onLooperPrepared();
        FooLog.v(TAG, threadName + " -onLooperPrepared()");
    }

    @Override
    public void run()
    {
        String threadName = getName();
        FooLog.v(TAG, threadName + " +run()");

        super.run();

        /*
        mTid = android.os.Process.myTid();

        FooLog.v(TAG, threadName + " run: +Looper.prepare()");
        Looper.prepare();
        FooLog.v(TAG, threadName + " run: -Looper.prepare()");

        FooLog.v(TAG, threadName + " run: +synchronized (this)");
        synchronized (this)
        {
            FooLog.v(TAG, threadName + " run: synchronized (this)");

            mLooper = Looper.myLooper();

            FooLog.v(TAG, threadName + " run: this.notifyAll()");
            notifyAll();
        }
        FooLog.v(TAG, threadName + " run: -synchronized (this)");

        Process.setThreadPriority(mPriority);

        onLooperPrepared();

        FooLog.v(TAG, threadName + " run: +Looper.loop()");
        Looper.loop();
        FooLog.v(TAG, threadName + " run: -Looper.loop()");

        mTid = -1;
        */

        FooLog.v(TAG, threadName + " -run()");
    }

    /*
    public Looper getLooper()
    {
        if (!isAlive())
        {
            return null;
        }

        // If the thread has been started, wait until the looper has been created.
        synchronized (this)
        {
            while (isAlive() && mLooper == null)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    // ignore
                }
            }
        }
        return mLooper;
    }

    public boolean quit()
    {
        Looper looper = getLooper();
        if (looper != null)
        {
            looper.quit();
            return true;
        }
        return false;
    }

    public boolean quitSafely()
    {
        Looper looper = getLooper();
        if (looper != null)
        {
            looper.quitSafely();
            return true;
        }
        return false;
    }

    public int getThreadId()
    {
        return mTid;
    }
    */
}
