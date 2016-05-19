package com.smartfoo.android.core.platform;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.Gravity;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.notification.FooPermissionsChecker;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class FooPlatformUtils
{
    private FooPlatformUtils()
    {
    }

    /**
     * @param context
     * @param resId   resource id of the string to toast, or -1 for none
     */
    public static void toastLong(Context context, int resId)
    {
        toastLong(context, resId, Gravity.CENTER);
    }

    /**
     * @param context
     * @param resId   resource id of the string to toast, or -1 for none
     * @param gravity one of Gravity.*
     */
    public static void toastLong(Context context, int resId, int gravity)
    {
        toast(context, resId, Toast.LENGTH_LONG, gravity);
    }

    /**
     * @param context
     * @param text    string to toast, or null/empty for none
     */
    public static void toastLong(Context context, String text)
    {
        toastLong(context, text, Gravity.CENTER);
    }

    /**
     * @param context
     * @param text    string to toast, or null/empty for none
     * @param gravity one of Gravity.*
     */
    public static void toastLong(Context context, String text, int gravity)
    {
        toast(context, text, Toast.LENGTH_LONG, gravity);
    }

    /**
     * @param context
     * @param resId   resource id of the string to toast, or -1 for none
     */
    public static void toastShort(Context context, int resId)
    {
        toastShort(context, resId, Gravity.CENTER);
    }

    /**
     * @param context
     * @param resId   resource id of the string to toast, or -1 for none
     * @param gravity one of Gravity.*
     */
    public static void toastShort(Context context, int resId, int gravity)
    {
        toast(context, resId, Toast.LENGTH_SHORT, gravity);
    }

    /**
     * @param context
     * @param text    string to toast, or null/empty for none
     */
    public static void toastShort(Context context, String text)
    {
        toastShort(context, text, Gravity.CENTER);
    }

    /**
     * @param context
     * @param text    string to toast, or null/empty for none
     * @param gravity one of Gravity.*
     */
    public static void toastShort(Context context, String text, int gravity)
    {
        toast(context, text, Toast.LENGTH_SHORT, gravity);
    }

    /**
     * @param context
     * @param resId    resource id of the string to toast, or -1 for none
     * @param duration One of Toast.LENGTH_*
     */
    public static void toast(Context context, int resId, int duration, int gravity)
    {
        if (context == null)
        {
            return;
        }

        if (resId == -1)
        {
            return;
        }

        toast(context, context.getString(resId), duration, gravity);
    }

    /**
     * @param context
     * @param text     string to toast, or null/empty for none
     * @param duration One of Toast.LENGTH_*
     */
    public static void toast(Context context, String text, int duration, int gravity)
    {
        if (context == null)
        {
            return;
        }

        if (FooString.isNullOrEmpty(text))
        {
            return;
        }

        Toast toast = Toast.makeText(context, text, duration);
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        if (v != null)
        {
            v.setGravity(gravity);
        }
        toast.show();
    }

    /**
     * @param context
     * @return never null
     */
    public static String getPackageName(Context context)
    {
        return context.getPackageName();
    }

    public static String getApplicationName(Context context)
    {
        return getApplicationName(context, getPackageName(context));
    }

    public static String getApplicationName(Context context, String packageName)
    {
        PackageManager pm = context.getPackageManager();
        try
        {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            if (ai != null)
            {
                CharSequence applicationLabel = pm.getApplicationLabel(ai);
                if (applicationLabel != null)
                {
                    return applicationLabel.toString();
                }
            }
        }
        catch (NameNotFoundException e)
        {
            // ignore
        }
        return null;
    }

    /**
     * @param context
     * @return PackageInfo of the context's package name, or null if one does not exist (should never happen)
     */
    public static PackageInfo getPackageInfo(Context context)
    {
        String packageName = context.getPackageName();
        try
        {
            return context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
        }
        catch (NameNotFoundException e)
        {
            // ignore
            return null;
        }
    }

    /**
     * @param context
     * @param defaultValue
     * @return the context's package's versionName, or defaultValue if one does not exist
     */
    public static String getVersionName(Context context, String defaultValue)
    {
        PackageInfo packageInfo = getPackageInfo(context);
        if (packageInfo != null)
        {
            return packageInfo.versionName;
        }
        else
        {
            return defaultValue;
        }
    }

    /**
     * @param context
     * @param defaultValue
     * @return the context's package's versionCode, or defaultValue if one does not exist
     */
    public static int getVersionCode(Context context, int defaultValue)
    {
        PackageInfo packageInfo = getPackageInfo(context);
        if (packageInfo != null)
        {
            return packageInfo.versionCode;
        }
        else
        {
            return defaultValue;
        }
    }

    public static String getDeviceName()
    {
        String manufacturer = FooString.capitalize(Build.MANUFACTURER);
        String deviceModel = FooString.capitalize(Build.MODEL);

        String deviceName;
        if (deviceModel.startsWith(manufacturer))
        {
            deviceName = deviceModel;
        }
        else
        {
            deviceName = manufacturer + " - " + deviceModel;
        }

        return deviceName;
    }

    /**
     * @return An ID that is unique for every device
     */
    public static String getDeviceId(FooPermissionsChecker permissionsChecker)
    {
        String deviceId = null;

        List<String> permissionsDenied = permissionsChecker.checkPermission(Manifest.permission.READ_PHONE_STATE);
        if (!permissionsDenied.contains(Manifest.permission.READ_PHONE_STATE))
        {
            Context context = permissionsChecker.getContext();
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            deviceId = telephonyManager.getDeviceId();
        }

        String repeatingPattern = "((.)\\2+)";

        if (deviceId == null || deviceId.matches(repeatingPattern))
        {
            deviceId = Build.SERIAL;
        }

        return deviceId;
    }

    public static String getOsVersion()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("Android ").append(Build.VERSION.RELEASE);

        Field[] fields = Build.VERSION_CODES.class.getFields();
        for (Field field : fields)
        {
            String fieldName = field.getName();

            int fieldValue = -1;
            try
            {
                fieldValue = field.getInt(new Object());
            }
            catch (IllegalArgumentException | IllegalAccessException | NullPointerException e)
            {
                // ignore
            }

            if (fieldValue == Build.VERSION.SDK_INT)
            {
                if (!FooString.isNullOrEmpty(fieldName))
                {
                    builder.append(' ').append(fieldName);
                }
                builder.append(" (API level ").append(fieldValue).append(')');
                break;
            }
        }

        return builder.toString();
    }

    /*
    public static String getVersionFriendly(Context context)
    {
        FooVersionString version = getVersion(context);
        String versionCode = getBuildRevision(context);

        // TODO: move this to resources
        return String.format("Build: %s Version: %s", versionCode, version.toString());
    }
    */

    /**
     * @param context
     * @return null if the package does not exist or has no meta-data
     */
    public static Bundle getMetaData(Context context)
    {
        Bundle metaDataBundle = null;

        String packageName = getPackageName(context);
        PackageManager packageManager = context.getPackageManager();

        ApplicationInfo applicationInfo;
        try
        {
            applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            applicationInfo = null;
        }

        if (applicationInfo != null)
        {
            metaDataBundle = applicationInfo.metaData;
        }

        return metaDataBundle;
    }

    public static String getMetaDataString(Context context, String key, String defaultValue)
    {
        String value = null;
        Bundle metaDataBundle = getMetaData(context);
        if (metaDataBundle != null)
        {
            value = metaDataBundle.getString(key, defaultValue);
        }
        return value;
    }

    public static int getMetaDataInt(Context context, String key, int defaultValue)
    {
        int value = defaultValue;
        Bundle metaDataBundle = getMetaData(context);
        if (metaDataBundle != null)
        {
            value = metaDataBundle.getInt(key, defaultValue);
        }
        return value;
    }

    public static boolean getMetaDataBoolean(Context context, String key, boolean defaultValue)
    {
        boolean value = defaultValue;
        Bundle metaDataBundle = getMetaData(context);
        if (metaDataBundle != null)
        {
            value = metaDataBundle.getBoolean(key, defaultValue);
        }
        return value;
    }

    public static String toString(Intent intent)
    {
        if (intent == null)
        {
            return "null";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(intent.toString());

        Bundle bundle = intent.getExtras();
        sb.append(", extras=").append(toString(bundle));

        return sb.toString();
    }

    public static String toString(Bundle bundle)
    {
        if (bundle == null)
        {
            return "null";
        }

        StringBuilder sb = new StringBuilder();

        Set<String> keys = bundle.keySet();
        Iterator<String> it = keys.iterator();

        sb.append('{');
        while (it.hasNext())
        {
            String key = it.next();
            Object value = bundle.get(key);

            sb.append(FooString.quote(key)).append('=');

            if (key.toLowerCase().contains("password"))
            {
                value = "*CENSORED*";
            }

            if (value instanceof Bundle)
            {
                sb.append(toString((Bundle) value));
            }
            else if (value instanceof Intent)
            {
                sb.append(toString((Intent) value));
            }
            else
            {
                sb.append(FooString.quote(value));
            }

            if (it.hasNext())
            {
                sb.append(", ");
            }
        }
        sb.append('}');

        return sb.toString();
    }

    /**
     * I originally just wanted to be able to change the System Development Debug Layout property.
     * I thought that I could duplicate what com.android.settings.DevelopmentSettings does:
     * https://github.com/android/platform_packages_apps_settings/blob/master/src/com/android/settings/DevelopmentSettings.java#L941
     * ie: Use Reflection to set the SystemProperty and then pokeSystemProperties
     * <p/>
     * After several hours of work I learned that the SystemProperties are ACL protected to only allow the Google
     * Signed Settings app to change them.
     * http://stackoverflow.com/a/11136242 -> http://stackoverflow.com/a/11123609/252308
     * <p/>
     * Rather than continue to try to get this to work (if it is even possible),
     * I have chosen to just launch the SettingsActivity DevelopmentSettings fragment.
     * <p/>
     * Other references for my wasted efforts:
     * https://github.com/android/platform_packages_apps_settings/blob/master/src/com/android/settings/DevelopmentSettings.java#L1588
     * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/os/SystemProperties.java#L122
     * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/view/View.java#L706
     * https://github.com/Androguide/CMDProcessorLibrary/blob/master/CMDProcessorLibrary/src/com/androguide/cmdprocessor/SystemPropertiesReflection.java
     */
    public static void showDevelopmentSettings(Context context)
    {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        // DevelopmentSettings appears to not have any arguments. :(
        // https://github.com/android/platform_packages_apps_settings/blob/master/src/com/android/settings/DevelopmentSettings.java
        context.startActivity(intent);
    }

    public static void showGooglePlay(Context context, String packageName)
    {
        try
        {
            Uri uri = Uri.parse("market://details?id=" + packageName);
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
        catch (ActivityNotFoundException e)
        {
            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName);
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }

    public static void showAppSettings(Context context, String packageName)
    {
        Uri uri = Uri.parse("package:" + packageName);
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
        context.startActivity(intent);
    }

    public static void showAppSettings(Context context)
    {
        showAppSettings(context, context.getPackageName());
    }

    public static void showBatterySettings(Context context)
    {
        Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, 0);
        if (resolveInfo != null)
        {
            context.startActivity(intent);
        }
    }

    /**
     * Reference: http://stackoverflow.com/a/11438245/252308
     *
     * @param context
     */
    public static void enableDeviceMenuButtonToShowOverflowMenu(Context context)
    {
        try
        {
            ViewConfiguration config = ViewConfiguration.get(context);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null)
            {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        }
        catch (Exception e)
        {
            // Ignore but consider logging; worst case: Phone w/ dedicated menu button won't see menu in ActionBar
            //FooLog.e(TAG, "EXCEPTION setting ActionBar sHasPermanentMenuKey=" + false, e);
        }
    }

    public static void showSoftInput(View view, boolean show)
    {
        if (view == null)
        {
            throw new IllegalArgumentException("view must not be null");
        }

        Context context = view.getContext();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (show)
        {
            imm.showSoftInput(view, 0);
        }
        else
        {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * @param view
     * @param start
     * @param kind
     * @param whats
     * @param <T>
     * @return
     */
    public static <T> int setNextSpan(TextView view, int start, Class<T> kind, Object[] whats)
    {
        CharSequence charSequence = view.getText();

        int length = charSequence.length();

        SpannableString spannable =
                (charSequence instanceof SpannableString) ? (SpannableString) charSequence : SpannableString.valueOf(charSequence);

        // For some reason "spannable.nextSpanTransition(...)" doesn't seem to work correctly if the string *starts* w/ the given span type
        T[] spans = spannable.getSpans(start, length, kind);
        if (spans == null || spans.length == 0)
        {
            return -1;
        }

        start = spannable.getSpanStart(spans[0]);
        int end = spannable.getSpanEnd(spans[0]);

        for (Object what : whats)
        {
            spannable.setSpan(what, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        view.setText(spannable);

        //MovementMethod m = view.getMovementMethod();
        //if ((m == null) || !(m instanceof LinkMovementMethod))
        //{
        //    view.setMovementMethod(LinkMovementMethod.getInstance());
        //}

        return end;
    }

    /**
     * @param delegate
     * @param dx
     * @param dy
     */
    public static void enlargeHitRect(final View delegate, final int dx, final int dy)
    {
        final View parent = (View) delegate.getParent();
        parent.post(new Runnable()
        {
            @Override
            public void run()
            {
                Rect r = new Rect();
                delegate.getHitRect(r);
                r.inset(-dx, -dy);
                parent.setTouchDelegate(new TouchDelegate(r, delegate));
            }
        });
    }

    public static int dip2px(Context context, float dpValue)
    {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * @param show if true, shows the progress view and hides the main view;
     *             if false, shows the main view and hides the progress view
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public static void showProgress(final boolean show, final View main, final View progress)
    {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)
        {
            int shortAnimTime = main.getResources().getInteger(android.R.integer.config_shortAnimTime);

            main.setVisibility(show ? View.GONE : View.VISIBLE);
            main.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            main.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });

            progress.setVisibility(show ? View.VISIBLE : View.GONE);
            progress.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            progress.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });
        }
        else
        {
            // The ViewPropertyAnimator APIs are not available,
            // so simply show and hide the relevant UI components.
            main.setVisibility(show ? View.VISIBLE : View.GONE);
            progress.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public static boolean supportsViewElevation()
    {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
    }

    @SuppressLint("NewApi")
    public static Drawable getDrawable(Resources res, int resId)
    {
        //noinspection deprecation
        return (Build.VERSION.SDK_INT > 21) ? res.getDrawable(resId, null) : res.getDrawable(resId);
    }
}
