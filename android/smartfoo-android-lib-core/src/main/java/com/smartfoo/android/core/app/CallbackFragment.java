package com.smartfoo.android.core.app;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.smartfoo.android.core.FooReflection;

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

    /**
     * Resolves the callback target by inspecting the fragment itself, its parent fragment, and
     * its host activity in that order. The first object that implements {@code T} is stored in
     * {@link #mCallback}.
     *
     * @param context the host context; not used directly but required by the super-class contract
     * @throws IllegalStateException if none of the candidates implement the callback interface
     */
    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        if (FooReflection.isAssignableFrom(mDummyCallback, this))
        {
            //noinspection unchecked
            mCallback = (T) this;
        }
        else
        {
            Fragment parentFragment = getParentFragment();
            if (FooReflection.isAssignableFrom(mDummyCallback, parentFragment))
            {
                //noinspection unchecked
                mCallback = (T) parentFragment;
            }
            else
            {
                FragmentActivity activity = getActivity();
                if (FooReflection.isAssignableFrom(mDummyCallback, activity))
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
                            FooReflection.getInstanceSignature(mDummyCallback));
                }
            }
        }
    }

    /**
     * Resets {@link #mCallback} to the no-op dummy instance so that any callback invocations
     * after detachment are safely ignored.
     */
    @Override
    public void onDetach()
    {
        super.onDetach();
        mCallback = mDummyCallback;
    }
}
