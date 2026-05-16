package com.smartfoo.android.core.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A {@link FooLogFormatter} that resolves the process ID by reading {@code $PPID} from the Unix
 * shell and uses {@link Thread#getId()} for the thread ID.
 *
 * <p>Intended for use in plain-JVM (non-Android) test or tool contexts where
 * {@code android.os.Process} is unavailable. On failure to read the PID, {@code -1} is used.</p>
 */
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
