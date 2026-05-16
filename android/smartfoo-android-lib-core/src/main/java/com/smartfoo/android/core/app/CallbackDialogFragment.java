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

    /**
     * Enables instance retention when the fragment is not nested inside another fragment, which
     * prevents "Can't retain fragments that are nested in other fragments" from being thrown on
     * a configuration change.
     *
     * @param savedInstanceState the previously saved state, or {@code null} on first creation
     */
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

    /**
     * Resolves the callback target by inspecting the fragment itself, its parent fragment, and
     * its host activity in that order. The first object that implements {@code T} is stored in
     * {@link #mCallback}.
     *
     * @param context the host context; not used directly but required by the super-class contract
     * @throws IllegalStateException if none of the candidates implement the callback interface
     */
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

    /**
     * Clears the dismiss message on the retained dialog to work around a framework bug
     * (https://code.google.com/p/android/issues/detail?id=17423) that prevents the dialog
     * fragment from being correctly recreated after a screen rotation.
     */
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
