package com.smartfoo.android.core.app;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.smartfoo.android.core.FooReflection;

/**
 * <p>
 * TODO:(pv) Get this to work for non-interface class instances
 * </p>
 * Usage:
 * <pre>
 * public class MyDialogFragment extends CallbackDialogFragment&lt;MyDialogFragmentListener&gt;
 * {
 *     public interface MyDialogFragmentListener
 *     {
 *         SomeObject getSomeObject();
 *     }
 *
 *     public MyDialogFragment()
 *     {
 *         super(new MyDialogFragmentListener()
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
public abstract class CallbackDialogFragment<T>
        extends AppCompatDialogFragment
{
    private final T mDummyCallback;

    protected T mCallback;

    protected CallbackDialogFragment(T dummyCallback)
    {
        if (dummyCallback == null)
        {
            throw new IllegalArgumentException("dummyCallback must not be null");
        }

        mDummyCallback = dummyCallback;

        mCallback = mDummyCallback;
    }

    @Override
    public void onCreate(
            @Nullable
                    Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Fragment parentFragment = getParentFragment();
        if (parentFragment == null)
        {
            //
            // Prevent [sic] "java.lang.IllegalStateException: Can't retain fragements that are nested in other fragments"
            //
            setRetainInstance(true);
        }
    }

    @Override
    public void onAttach(@NonNull Context context)
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

    @Override
    public void onDetach()
    {
        super.onDetach();
        mCallback = mDummyCallback;
    }

    @Override
    public void onDestroyView()
    {
        // If we don't do this, the DialogFragment is not recreated after a rotation.
        // See bug: https://code.google.com/p/android/issues/detail?id=17423
        if (getRetainInstance())
        {
            Dialog dialog = getDialog();
            if (dialog != null)
            {
                dialog.setDismissMessage(null);
            }
        }
        super.onDestroyView();
    }
}
