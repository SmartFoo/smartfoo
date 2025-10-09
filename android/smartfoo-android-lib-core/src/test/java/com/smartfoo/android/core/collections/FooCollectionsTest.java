package com.smartfoo.android.core.collections;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class FooCollectionsTest
{
    @Test
    public void identicalNull()
            throws Exception
    {
        List<Integer> a = null;
        List<Integer> b = null;
        @SuppressWarnings("ConstantConditions")
        boolean identical = FooCollections.identical(a, b);
        Assert.assertTrue("FooCollections.identical(a, b) != true", identical);
    }

    @Test
    public void notIdentical1()
            throws Exception
    {
        List<Integer> a = null;
        List<Integer> b = new LinkedList<>();
        @SuppressWarnings("ConstantConditions")
        boolean identical = FooCollections.identical(a, b);
        Assert.assertFalse("FooCollections.identical(a, b) == true", identical);
    }

    @Test
    public void notIdentical2()
            throws Exception
    {
        List<Integer> a = new LinkedList<>();
        List<Integer> b = null;
        @SuppressWarnings("ConstantConditions")
        boolean identical = FooCollections.identical(a, b);
        Assert.assertFalse("FooCollections.identical(a, b) == true", identical);
    }

    @Test
    public void identicalEmpty()
            throws Exception
    {
        List<Integer> a = new LinkedList<>();
        List<Integer> b = new LinkedList<>(a);
        boolean identical = FooCollections.identical(a, b);
        Assert.assertTrue("FooCollections.identical(a, b) != true", identical);
    }

    @Test
    public void notIdentical3()
            throws Exception
    {
        List<Integer> a = new LinkedList<>();
        a.add(1);
        a.add(2);
        a.add(3);
        List<Integer> b = new LinkedList<>();
        boolean identical = FooCollections.identical(a, b);
        Assert.assertFalse("FooCollections.identical(a, b) == true", identical);
    }

    @Test
    public void notIdentical4()
            throws Exception
    {
        List<Integer> a = new LinkedList<>();
        List<Integer> b = new LinkedList<>();
        b.add(1);
        b.add(2);
        b.add(3);
        boolean identical = FooCollections.identical(a, b);
        Assert.assertFalse("FooCollections.identical(a, b) == true", identical);
    }

    @Test
    public void identical123()
            throws Exception
    {
        List<Integer> a = new LinkedList<>();
        a.add(1);
        a.add(2);
        a.add(3);
        List<Integer> b = new LinkedList<>(a);
        boolean identical = FooCollections.identical(a, b);
        Assert.assertTrue("FooCollections.identical(a, b) != true", identical);
    }
}