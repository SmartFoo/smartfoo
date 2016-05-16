package com.smartfoo.android.core.logging;

import android.Manifest;
import android.content.Context;
import android.os.Environment;

import com.smartfoo.android.core.platform.FooPlatformUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The caller of this class is responsible for checking that all {@link #REQUIRED_PERMISSIONS} have been allowed.
 */
public class FooLogFilePrinter
        extends FooLogPrinter
{
    private static final String TAG = FooLog.TAG(FooLogFilePrinter.class);

    public static final String[] REQUIRED_PERMISSIONS = new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE };

    private static final boolean LOG_IOEXCEPTIONS = false;

    private static final FooLogFormatter DEFAULT_FORMATTER = new FooLogAndroidFormatter();

    private static FooLogFilePrinter sInstance;

    /**
     * NOTE: {@link #getInstance(Context)} must first be called
     *
     * @return the singleton FooLogFileWriter instance
     * @throws IllegalStateException if {@link #getInstance(Context)} has not been called
     */
    public static FooLogFilePrinter getInstance()
    {
        return getInstance(null);
    }

    public static FooLogFilePrinter getInstance(Context applicationContext)
    {
        return getInstance(applicationContext, DEFAULT_FORMATTER);
    }

    public static FooLogFilePrinter getInstance(Context applicationContext, FooLogFormatter formatter)
    {
        return getInstance(applicationContext, formatter, true);
    }

    private static FooLogFilePrinter getInstance(Context applicationContext, FooLogFormatter formatter, boolean throwIllegalStateException)
    {
        if (sInstance == null)
        {
            if (applicationContext == null)
            {
                if (throwIllegalStateException)
                {
                    throw new IllegalStateException("getInstance(Context applicationContext) must first be called with a non-null applicationContext");
                }
            }
            else
            {
                sInstance = new FooLogFilePrinter(applicationContext, formatter);
            }
        }
        return sInstance;
    }

    private final FooLogFormatter mFormatter;
    private final File            mLogFile;

    private BufferedWriter mBufferedWriter;

    private FooLogFilePrinter(Context applicationContext, FooLogFormatter formatter)
    {
        if (applicationContext == null)
        {
            throw new IllegalArgumentException("applicationContext must not be null");
        }

        if (formatter == null)
        {
            throw new IllegalArgumentException("formatter must not be null");
        }

        mFormatter = formatter;

        String logFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                             File.separatorChar + FooPlatformUtils.getPackageName(applicationContext) +
                             File.separatorChar + "debuglog.txt";

        mLogFile = new File(logFilePath);
    }

    public File getUncompressedLogFile()
    {
        return mLogFile;
    }

    /**
     * @param compress true to compress the data (may throw IOException), false to just get the path (will never throw
     *                 IOException)
     * @return
     * @throws IOException
     */
    public File getCompressedLogFile(boolean compress)
            throws IOException
    {
        File inputFile = mLogFile;
        String inputFilePath = inputFile.getAbsolutePath();
        int inputFileNameIndex = inputFilePath.lastIndexOf('/') + 1;
        String inputFileName = inputFilePath.substring(inputFileNameIndex);
        int inputFileNameExtensionIndex = inputFileName.lastIndexOf('.');
        if (inputFileNameExtensionIndex == -1)
        {
            inputFileNameExtensionIndex = inputFileName.length();
        }
        inputFilePath = inputFilePath.substring(0, inputFileNameIndex);

        String outputFileName = inputFileName.substring(0, inputFileNameExtensionIndex) + ".zip";
        String outputFilePath = inputFilePath + outputFileName;
        File outputFile = new File(outputFilePath);

        if (compress)
        {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile));

            ZipOutputStream zos = new ZipOutputStream(bos);

            zos.setLevel(Deflater.BEST_COMPRESSION);

            //noinspection TryFinallyCanBeTryWithResources
            try
            {
                try
                {
                    ZipEntry entry = new ZipEntry(inputFileName);

                    zos.putNextEntry(entry);

                    FileInputStream fis = new FileInputStream(inputFile);
                    int len;
                    byte[] buffer = new byte[1024];
                    while ((len = fis.read(buffer)) > 0)
                    {
                        zos.write(buffer, 0, len);
                    }
                    fis.close();
                }
                finally
                {
                    zos.closeEntry();
                }
            }
            catch (IOException e)
            {
                FooLog.e(TAG, "getCompressedLogFile: EXCEPTION", e);
                throw e;
            }
            finally
            {
                zos.close();
            }
        }

        return outputFile;
    }

    private boolean openBufferedWriter()
    {
        synchronized (mLogFile)
        {
            if (mBufferedWriter == null)
            {
                String state = Environment.getExternalStorageState();
                if (!Environment.MEDIA_MOUNTED.equals(state))
                {
                    if (LOG_IOEXCEPTIONS)
                    {
                        System.out.println(
                                TAG + " openBufferedWriter: Cannot write to " + mLogFile.getAbsolutePath() +
                                "; media not mounted");
                    }
                    return false;
                }

                String fileParent = mLogFile.getParent();
                if (fileParent != null)
                {
                    File fileParentDir = new File(fileParent);

                    //noinspection ResultOfMethodCallIgnored
                    fileParentDir.mkdirs();
                }

                try
                {
                    mBufferedWriter = new BufferedWriter(new FileWriter(mLogFile), 1024);
                }
                catch (IOException ioe)
                {
                    if (LOG_IOEXCEPTIONS)
                    {
                        System.out.println(TAG + " openBufferedWriter: EXCEPTION " + ioe);
                    }
                    return false;
                }
            }
        }

        return true;
    }

    private void closeBufferedWriter()
    {
        synchronized (mLogFile)
        {
            if (mBufferedWriter != null)
            {
                try
                {
                    mBufferedWriter.close();
                }
                catch (IOException e)
                {
                    if (LOG_IOEXCEPTIONS)
                    {
                        System.out.println(TAG + " closeBufferedWriter: EXCEPTION " + e);
                    }
                }

                mBufferedWriter = null;
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        synchronized (mLogFile)
        {
            super.setEnabled(enabled);

            if (!enabled)
            {
                closeBufferedWriter();
            }
        }
    }

    @Override
    protected boolean printlnInternal(String tag, int level, String msg, Throwable e)
    {
        msg = mFormatter.format(tag, level, msg, e);

        synchronized (mLogFile)
        {
            if (!openBufferedWriter())
            {
                return false;
            }

            try
            {
                mBufferedWriter.write(msg);
                mBufferedWriter.newLine();
                mBufferedWriter.flush();
            }
            catch (IOException ioe)
            {
                if (LOG_IOEXCEPTIONS)
                {
                    System.out.println(TAG + " printlnInternal: EXCEPTION " + ioe);
                }
                return false;
            }
        }

        return true;
    }

    @Override
    public void clear()
    {
        synchronized (mLogFile)
        {
            closeBufferedWriter();

            //noinspection ResultOfMethodCallIgnored
            mLogFile.delete();
        }
    }
}
