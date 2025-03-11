package com.smartfoo.android.core;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.annotations.NonNullNonEmpty;
import com.smartfoo.android.core.reflection.FooReflectionUtils;

public class FooListenerAutoStartManager<T>
        extends FooListenerManager<T>
{
    public interface FooListenerAutoStartManagerCallbacks
    {
        void onFirstAttach();

        /**
         * @return true to automatically detach this {@link FooListenerAutoStartManagerCallbacks} from {@link
         * FooListenerAutoStartManager#mAutoStartListeners}
         */
        boolean onLastDetach();
    }

    private final FooListenerManager<FooListenerAutoStartManagerCallbacks> mAutoStartListeners;

    private boolean mIsStarted;

    public FooListenerAutoStartManager(@NonNull Object name)
    {
        this(FooReflectionUtils.getShortClassName(name));
    }

    public FooListenerAutoStartManager(@NonNullNonEmpty String name)
    {
        super(name);
        mAutoStartListeners = new FooListenerManager<>(this);
    }

    public void attach(@NonNull FooListenerAutoStartManagerCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");
        mAutoStartListeners.attach(callbacks);
    }

    public void detach(@NonNull FooListenerAutoStartManagerCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");
        mAutoStartListeners.detach(callbacks);
    }

    @Override
    protected void onListenersUpdated(int listenersSize)
    {
        super.onListenersUpdated(listenersSize);

        if (mIsStarted)
        {
            if (listenersSize == 0)
            {
                mIsStarted = false;
                for (FooListenerAutoStartManagerCallbacks callbacks : mAutoStartListeners.beginTraversing())
                {
                    if (callbacks.onLastDetach())
                    {
                        //mAutoStartListeners.detach(callbacks);
                    }
                }
                mAutoStartListeners.endTraversing();
            }
        }
        else
        {
            if (listenersSize > 0)
            {
                mIsStarted = true;
                for (FooListenerAutoStartManagerCallbacks callbacks : mAutoStartListeners.beginTraversing())
                {
                    callbacks.onFirstAttach();
                }
                mAutoStartListeners.endTraversing();
            }
        }
    }
}
