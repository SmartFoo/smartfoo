package com.smartfoo.android.core.platform;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.util.DisplayMetrics;

import com.smartfoo.android.core.FooRun;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class FooRes
{
    private FooRes()
    {
    }

    @NonNull
    public static Context getContext(@NonNull Context context)
    {
        return FooRun.getContext(context);
    }

    @NonNull
    public static Resources getResources(@NonNull Context context)
    {
        return getContext(context).getResources();
    }

    @NonNull
    public static Configuration getConfiguration(@NonNull Context context)
    {
        return getResources(context).getConfiguration();
    }

    @NonNull
    public static DisplayMetrics getDisplayMetrics(@NonNull Context context)
    {
        return getResources(context).getDisplayMetrics();
    }

    @NonNull
    public static String getString(@NonNull Context context, int resId, Object... formatArgs)
    {
        return getContext(context).getString(resId, formatArgs);
    }

    @SuppressLint({ "NewApi", "ObsoleteSdkInt" })
    public static int getColor(@NonNull Resources res, int resId)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(res, "res");
        //noinspection deprecation
        return (Build.VERSION.SDK_INT > 23) ? res.getColor(resId, null) : res.getColor(resId);
    }

    @NonNull
    @SuppressLint({ "NewApi", "ObsoleteSdkInt" })
    public static Drawable getDrawable(@NonNull Resources res, int resId)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(res, "res");
        //noinspection deprecation
        return (Build.VERSION.SDK_INT > 21) ? res.getDrawable(resId, null) : res.getDrawable(resId);
    }

    public static int getSystemResourcesDisplayMetricsHeightPixels()
    {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public static int dip2px(@NonNull Context context, float dpValue)
    {
        final float scale = getDisplayMetrics(context).density;
        return (int) (dpValue * scale + 0.5f);
    }

    @NonNull
    @SuppressLint("NewApi")
    public static Locale getLocale(@NonNull Context context)
    {
        Configuration configuration = getConfiguration(context);
        //noinspection deprecation
        return (VERSION.SDK_INT >= 24) ? configuration.getLocales().get(0) : configuration.locale;
    }

    public static int getOrientation(@NonNull Context context)
    {
        return getConfiguration(context).orientation;
    }

    @NonNull
    public static String orientationToString(int orientation)
    {
        String s;
        switch (orientation)
        {
            case Configuration.ORIENTATION_LANDSCAPE:
                s = "ORIENTATION_LANDSCAPE";
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                s = "ORIENTATION_PORTRAIT";
                break;
            case Configuration.ORIENTATION_UNDEFINED:
                s = "ORIENTATION_UNDEFINED";
                break;
            case Configuration.ORIENTATION_SQUARE:
                s = "ORIENTATION_SQUARE";
                break;
            default:
                s = "UNKNOWN";
                break;
        }
        return s + '(' + orientation + ')';
    }

    public static byte[] openRawResource(@NonNull Context context, @RawRes int resId)
    {

        InputStream inputStream;
        try
        {
            inputStream = getResources(context).openRawResource(resId);
        }
        catch (NotFoundException e)
        {
            return null;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] data;

        try
        {
            try
            {
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int readSize;
                while ((readSize = inputStream.read(buffer, 0, bufferSize)) > 0)
                {
                    outputStream.write(buffer, 0, readSize);
                }
            }
            catch (IOException e)
            {
                return null;
            }

            data = outputStream.toByteArray();
        }
        finally
        {
            try
            {
                inputStream.close();
            }
            catch (IOException e)
            {
                // ignore
            }

            try
            {
                outputStream.close();
            }
            catch (IOException e)
            {
                // ignore
            }
        }

        return data;
    }
}
