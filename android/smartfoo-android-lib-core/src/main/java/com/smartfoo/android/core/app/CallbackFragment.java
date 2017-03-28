package com.smartfoo.android.core.app;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.smartfoo.android.core.reflection.FooReflectionUtils;

/**
 * <p>
 * TODO:(pv) Get this to work for non-interface class instances
 * </p>
 * Usage:
 * <pre>
 * public class MyFragment extends CallbackFragment&lt;MyFragmentListener&gt;
 * {
 *     public interface MyFragmentListener
 *     {
 *         SomeObject getSomeObject();
 *     }
 *
 *     public MyFragment()
 *     {
 *         super(new MyFragmentListener()
 *         {
 *             &#064;Override
 *             public SomeObject getSomeObject()
 *             {
 *                 return null;
 *             }
 *         });
 *     }
 * }
 * </pre>
 *
 * @param <T> type
 */
public abstract class CallbackFragment<T>
        extends Fragment
{
    private final T mDummyCallback;

    protected T mCallback;

    protected CallbackFragment(@NonNull T dummyCallback)
    {
        mDummyCallback = dummyCallback;

        mCallback = mDummyCallback;
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        if (FooReflectionUtils.isAssignableFrom(mDummyCallback, this))
        {
            //noinspection unchecked
            mCallback = (T) this;
        }
        else
        {
            Fragment parentFragment = getParentFragment();
            if (FooReflectionUtils.isAssignableFrom(mDummyCallback, parentFragment))
            {
                //noinspection unchecked
                mCallback = (T) parentFragment;
            }
            else
            {
                FragmentActivity activity = getActivity();
                if (FooReflectionUtils.isAssignableFrom(mDummyCallback, activity))
                {
                    //noinspection unchecked
                    mCallback = (T) activity;
                }
                else
                {
                    throw new IllegalStateException("subclass[" + this + ']' +
                                                    ", getParentFragment()[" + parentFragment + ']' +
                                                    ", or getActivity()[" + activity + ']' +
                                                    " must be an instance of " +
                                                    FooReflectionUtils.getInstanceSignature(mDummyCallback));
                }
            }
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mCallback = mDummyCallback;
    }
}
