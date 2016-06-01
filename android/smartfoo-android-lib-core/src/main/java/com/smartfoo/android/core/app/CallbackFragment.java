package com.smartfoo.android.core.app;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

/**
 * TODO:(pv) Get this to work for non-interface class instances
 * <p/>
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
 *             &amp;Override
 *             public SomeObject getSomeObject()
 *             {
 *                 return null;
 *             }
 *         });
 *     }
 * }
 * </pre>
 *
 * @param <T>
 */
public abstract class CallbackFragment<T>
        extends Fragment
{
    public static <T> String getInstanceSignature(T instance)
    {
        if (instance == null)
        {
            throw new IllegalArgumentException("instance must not be null");
        }

        StringBuilder sb = new StringBuilder();

        Class<?> instanceClass = instance.getClass();

        Class<?>[] instanceSubclasses = instanceClass.getClasses();
        if (instanceSubclasses.length > 0)
        {
            sb.append(" extends");
            Class<?> instanceSubclass;
            for (int i = 0; i < instanceSubclasses.length; i++)
            {
                if (i > 0)
                {
                    sb.append(", ");
                }
                instanceSubclass = instanceSubclasses[i];
                sb.append(' ').append(instanceSubclass);
            }
        }

        Class<?>[] instanceInterfaces = instanceClass.getInterfaces();
        if (instanceInterfaces.length > 0)
        {
            sb.append(" implements");
            Class<?> instanceInterface;
            for (int i = 0; i < instanceInterfaces.length; i++)
            {
                if (i > 0)
                {
                    sb.append(", ");
                }
                instanceInterface = instanceInterfaces[i];
                sb.append(' ').append(instanceInterface);
            }
        }

        return sb.toString().trim();
    }

    public static boolean isAssignableFrom(Object expectedInstance, Object actualInstance)
    {
        if (expectedInstance == null)
        {
            throw new IllegalArgumentException("expectedInstance must not be null");
        }

        if (actualInstance == null)
        {
            return false;
        }

        Class<?> expectedInstanceClass = expectedInstance.getClass();

        Class<?> actualInstanceClass;
        if (actualInstance instanceof Class<?>)
        {
            actualInstanceClass = (Class<?>) actualInstance;
        }
        else
        {
            actualInstanceClass = actualInstance.getClass();
        }

        //
        // Verify that actualInstanceClass is an instance of all subclasses and interfaces of expectedClass...
        //

        Class<?>[] expectedSubclasses = expectedInstanceClass.getClasses();
        for (Class<?> expectedSubclass : expectedSubclasses)
        {
            if (!expectedSubclass.isAssignableFrom(actualInstanceClass))
            {
                return false;
            }
        }

        Class<?>[] expectedInterfaces = expectedInstanceClass.getInterfaces();
        for (Class<?> expectedInterface : expectedInterfaces)
        {
            if (!expectedInterface.isAssignableFrom(actualInstanceClass))
            {
                return false;
            }
        }

        return true;
    }

    private final T mDummyCallback;

    protected T mCallback;

    protected CallbackFragment(T dummyCallback)
    {
        if (dummyCallback == null)
        {
            throw new IllegalArgumentException("dummyCallback must not be null");
        }

        mDummyCallback = dummyCallback;

        mCallback = mDummyCallback;
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        if (isAssignableFrom(mDummyCallback, this))
        {
            //noinspection unchecked
            mCallback = (T) this;
        }
        else
        {
            Fragment parentFragment = getParentFragment();
            if (isAssignableFrom(mDummyCallback, parentFragment))
            {
                //noinspection unchecked
                mCallback = (T) parentFragment;
            }
            else
            {
                FragmentActivity activity = getActivity();
                if (isAssignableFrom(mDummyCallback, activity))
                {
                    //noinspection unchecked
                    mCallback = (T) activity;
                }
                else
                {
                    throw new IllegalStateException("subclass[" + this + ']' +
                                                    ", getParentFragment()[" + parentFragment + ']' +
                                                    ", or getActivity()[" + activity + ']' +
                                                    " must be an instance of class that " +
                                                    getInstanceSignature(mDummyCallback));
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
