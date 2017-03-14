package com.smartfoo.android.core.reflection;

public class FooReflectionUtils
{
    private FooReflectionUtils()
    {
    }

    public static Class<?> getClass(Object o)
    {
        return (o instanceof Class<?>) ? (Class<?>) o : (o != null ? o.getClass() : null);
    }

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

        Class<?> expectedInstanceClass = getClass(expectedInstance);

        Class<?> actualInstanceClass = getClass(actualInstance);

        //
        // Verify that actualInstanceClass is an instance of all subclasses and interfaces of expectedClass...
        //

        if (!expectedInstanceClass.isInterface())
        {
            Class<?>[] expectedSubclasses = expectedInstanceClass.getClasses();
            for (Class<?> expectedSubclass : expectedSubclasses)
            {
                if (!expectedSubclass.isAssignableFrom(actualInstanceClass))
                {
                    return false;
                }
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
}
