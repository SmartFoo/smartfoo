package com.smartfoo.android.core.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class FooLogUnixJavaFormatter
        extends FooLogFormatter
{
    private final int mPid;

    public FooLogUnixJavaFormatter()
    {
        int pid;

        try
        {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "echo $PPID");
            Process process = pb.start();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            pid = Integer.parseInt(bufferedReader.readLine().trim());
        }
        catch (IOException e)
        {
            System.out.println("Cannot get Process Id from file /proc/self");

            e.printStackTrace(System.out);

            pid = -1;
        }

        mPid = pid;
    }

    @Override
    protected int getPid()
    {
        return mPid;
    }

    @Override
    protected int getTid()
    {
        return (int) Thread.currentThread().getId();
    }
}
