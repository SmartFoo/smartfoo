package com.smartfoo.android.core.collections;

import com.smartfoo.android.core.FooObjects;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class FooCollections
{
    private FooCollections()
    {
    }

    public static <T> boolean identical(Collection<T> a, Collection<T> b)
    {
        if (a == b)
        {
            return true;
        }

        if (a == null || b == null)
        {
            return false;
        }

        int length = a.size();
        if (b.size() != length)
        {
            return false;
        }

        Iterator<T> itA = a.iterator();
        Iterator<T> itB = b.iterator();
        boolean identical = itA.hasNext() == itB.hasNext();
        while (identical && itA.hasNext() && itB.hasNext())
        {
            identical = FooObjects.equals(itA.next(), itB.next());
            identical &= itA.hasNext() == itB.hasNext();
        }

        return identical;
    }

    /**
     * Order and duplicate independent comparison of two collections
     *
     * @param a
     * @param b
     * @param <T>
     * @return
     */
    public static <T> boolean equivalent(Collection<T> a, Collection<T> b)
    {
        // TODO:(pv) Make this more efficient...

        if (false)
        {
            boolean t1 = a.containsAll(b);
            boolean t2 = b.containsAll(a);
            boolean result = t1 && t2;
            return result;
            //return a.containsAll(b) && b.containsAll(a);
            //return new TreeSet<>(a).equals(new TreeSet<>(b));
        }
        else
        {
            Set<T> a2 = new LinkedHashSet<>(a);
            Set<T> b2 = new LinkedHashSet<>(b);
            boolean result = a2.equals(b2);
            return result;
        }
    }
}
