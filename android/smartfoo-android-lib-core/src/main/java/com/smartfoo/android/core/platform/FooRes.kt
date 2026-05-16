package com.smartfoo.android.core.platform

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.annotation.RawRes
import androidx.core.content.res.ResourcesCompat
import com.smartfoo.android.core.FooReflection
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale

/**
 * Convenience utilities for accessing Android resources and display metrics.
 *
 * Covers colour and drawable retrieval, dp→px conversion, locale and orientation
 * queries, and reading raw resource files into a [ByteArray]. All methods require
 * a [android.content.Context] or [android.content.res.Resources] obtained from one.
 */
@Suppress("unused")
object FooRes {
    @JvmStatic
    fun getResources(context: Context): Resources = context.resources

    @JvmStatic
    fun getConfiguration(context: Context): Configuration = getResources(context).configuration

    @JvmStatic
    fun getDisplayMetrics(context: Context): DisplayMetrics = getResources(context).displayMetrics

    @JvmStatic
    fun getString(context: Context, resId: Int, vararg formatArgs: Any?) = context.getString(resId, *formatArgs)

    //@SuppressLint("NewApi", "ObsoleteSdkInt")
    @JvmStatic
    fun getColor(res: Resources, resId: Int) = res.getColor(resId, null)

    //@SuppressLint("NewApi", "ObsoleteSdkInt")
    @JvmStatic
    fun getDrawable(res: Resources, resId: Int) = ResourcesCompat.getDrawable(res, resId, null)

    @JvmStatic
    val systemResourcesDisplayMetricsHeightPixels: Int
        get() = Resources.getSystem().displayMetrics.heightPixels

    /**
     * Converts a density-independent pixel value to physical pixels.
     *
     * @param context any valid context (used to read the display density)
     * @param dpValue the value in dp to convert
     * @return the equivalent value in physical pixels, rounded to the nearest integer
     */
    @JvmStatic
    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = getDisplayMetrics(context).density
        return (dpValue * scale + 0.5f).toInt()
    }

    //@SuppressLint("NewApi")
    @JvmStatic
    fun getLocale(context: Context): Locale {
        val configuration = getConfiguration(context)
        return configuration.getLocales().get(0)
    }

    @JvmStatic
    fun getOrientation(context: Context) = getConfiguration(context).orientation

    private val orientationMap by lazy {
        FooReflection.mapConstants(Configuration::class, "ORIENTATION_")
    }

    @JvmStatic
    fun orientationToString(orientation: Int) = FooReflection.toString(orientationMap, orientation)

    /**
     * Reads a raw resource file into a [ByteArray].
     *
     * @param context any valid context
     * @param resId the raw resource ID to open
     * @return the file contents, or null if the resource does not exist or an I/O error occurs
     */
    @JvmStatic
    fun openRawResource(context: Context, @RawRes resId: Int): ByteArray? {
        val inputStream: InputStream?
        try {
            inputStream = getResources(context).openRawResource(resId)
        } catch (e: Resources.NotFoundException) {
            return null
        }

        val outputStream = ByteArrayOutputStream()

        var data: ByteArray?

        try {
            try {
                val bufferSize = 1024
                val buffer = ByteArray(bufferSize)
                var readSize: Int
                while ((inputStream.read(buffer, 0, bufferSize).also { readSize = it }) > 0) {
                    outputStream.write(buffer, 0, readSize)
                }
            } catch (e: IOException) {
                return null
            }

            data = outputStream.toByteArray()
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                // ignore
            }

            try {
                outputStream.close()
            } catch (e: IOException) {
                // ignore
            }
        }

        return data
    }
}

/*
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
*/
