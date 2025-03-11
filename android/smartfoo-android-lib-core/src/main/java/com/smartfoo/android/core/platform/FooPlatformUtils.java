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
import android.graphics.Rect;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.R;
import com.smartfoo.android.core.annotations.NonNullNonEmpty;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.permissions.FooPermissionsChecker;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class FooPlatformUtils
{
    private static final String TAG = FooLog.TAG(FooPlatformUtils.class);

    private FooPlatformUtils()
    {
    }

    /**
     * @param context context
     * @param resId   resource id of the string to toast, or -1 for none
     */
    public static void toastLong(Context context, int resId)
    {
        toastLong(context, resId, Gravity.CENTER);
    }

    /**
     * @param context context
     * @param resId   resource id of the string to toast, or -1 for none
     * @param gravity one of Gravity.*
     */
    public static void toastLong(Context context, int resId, int gravity)
    {
        toast(context, resId, Toast.LENGTH_LONG, gravity);
    }

    /**
     * @param context context
     * @param text    string to toast, or null/empty for none
     */
    public static void toastLong(Context context, String text)
    {
        toastLong(context, text, Gravity.CENTER);
    }

    /**
     * @param context context
     * @param text    string to toast, or null/empty for none
     * @param gravity one of Gravity.*
     */
    public static void toastLong(Context context, String text, int gravity)
    {
        toast(context, text, Toast.LENGTH_LONG, gravity);
    }

    /**
     * @param context context
     * @param resId   resource id of the string to toast, or -1 for none
     */
    public static void toastShort(Context context, int resId)
    {
        toastShort(context, resId, Gravity.CENTER);
    }

    /**
     * @param context context
     * @param resId   resource id of the string to toast, or -1 for none
     * @param gravity one of Gravity.*
     */
    public static void toastShort(Context context, int resId, int gravity)
    {
        toast(context, resId, Toast.LENGTH_SHORT, gravity);
    }

    /**
     * @param context context
     * @param text    string to toast, or null/empty for none
     */
    public static void toastShort(Context context, String text)
    {
        toastShort(context, text, Gravity.CENTER);
    }

    /**
     * @param context context
     * @param text    string to toast, or null/empty for none
     * @param gravity one of Gravity.*
     */
    public static void toastShort(Context context, String text, int gravity)
    {
        toast(context, text, Toast.LENGTH_SHORT, gravity);
    }

    /**
     * @param context  context
     * @param resId    resource id of the string to toast, or -1 for none
     * @param duration One of Toast.LENGTH_*
     * @param gravity  gravity
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
     * @param context  context
     * @param text     string to toast, or null/empty for none
     * @param duration One of Toast.LENGTH_*
     * @param gravity  gravity
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

        View v = toast.getView();
        if (v != null)
        {
            TextView tv = v.findViewById(android.R.id.message);
            if (tv != null)
            {
                tv.setGravity(gravity);
            }
        }

        toast.show();
    }

    @NonNull
    public static Context getContext(@NonNull Context context)
    {
        return FooRun.getContext(context);
    }

    @NonNull
    public static PackageManager getPackageManager(@NonNull Context context)
    {
        return getContext(context).getPackageManager();
    }

    @NonNull
    public static String getPackageName(@NonNull Context context)
    {
        return getContext(context).getPackageName();
    }

    public static String getApplicationName(@NonNull Context context)
    {
        return getApplicationName(context, getPackageName(context));
    }

    public static String getApplicationName(@NonNull Context context, String packageName)
    {
        ApplicationInfo ai = getApplicationInfo(context, packageName);
        if (ai == null)
        {
            return null;
        }

        CharSequence applicationLabel = getPackageManager(context).getApplicationLabel(ai);
        if (applicationLabel == null)
        {
            return null;
        }

        return applicationLabel.toString();
    }

    public static ApplicationInfo getApplicationInfo(@NonNull Context context, String packageName)
    {
        try
        {
            return getPackageManager(context).getApplicationInfo(packageName, 0);
        }
        catch (NameNotFoundException e)
        {
            return null;
        }
    }

    /**
     * @param context context
     * @return PackageInfo of the context's package name, or null if one does not exist (should never happen)
     */
    public static PackageInfo getPackageInfo(@NonNull Context context)
    {
        return getPackageInfo(context, getPackageName(context));
    }

    public static PackageInfo getPackageInfo(@NonNull Context context, @NonNullNonEmpty String packageName)
    {
        try
        {
            return getPackageManager(context)
                    .getPackageInfo(FooRun.toNonNullNonEmpty(packageName, "packageName"), PackageManager.GET_META_DATA);
        }
        catch (NameNotFoundException e)
        {
            return null;
        }
    }

    /**
     * @param context      context
     * @param defaultValue defaultValue
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
     * @param context      context
     * @param defaultValue defaultValue
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
     * @param permissionsChecker permissionsChecker
     * @return An ID that is unique for every device
     */
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    @SuppressLint("HardwareIds")
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

    public static boolean hasSystemFeature(@NonNull Context context, @NonNullNonEmpty String name)
    {
        return FooRun.toNonNull(context, "context")
                .getPackageManager()
                .hasSystemFeature(FooRun.toNonNullNonEmpty(name, "name"));
    }

    public static boolean hasSystemFeatureAutomotive(@NonNull Context context)
    {
        return hasSystemFeature(context, PackageManager.FEATURE_AUTOMOTIVE);
    }

    public static boolean hasSystemFeatureTelephony(@NonNull Context context)
    {
        return hasSystemFeature(context, PackageManager.FEATURE_TELEPHONY);
    }

    public static boolean hasSystemFeatureTelevision(@NonNull Context context)
    {
        return hasSystemFeature(context, PackageManager.FEATURE_LEANBACK);
    }

    public static boolean hasSystemFeatureWatch(@NonNull Context context)
    {
        return hasSystemFeature(context, PackageManager.FEATURE_WATCH);
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
     * @param context context
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
            Object value;
            try {
                /**
                 * {@link android.os.BaseBundle#get(java.lang.String)} calls hidden method {@link android.os.BaseBundle#getValue(java.lang.String)}.
                 * `android.os.BaseBundle#getValue(java.lang.String)` says:
                 * "Deprecated: Use `getValue(String, Class, Class[])`. This method should only be used in other deprecated APIs."
                 * That first sentence does not help this method that dynamically enumerates the Bundle entries without awareness/concern of any types.
                 * That second sentence tells me they probably won't be getting rid of android.os.BaseBundle#get(java.lang.String) any time soon.
                 * So marking deprecated `android.os.BaseBundle#get(java.lang.String)` as safe to call... for awhile.
                 */
                //noinspection deprecation
                value = bundle.get(key);
            } catch (RuntimeException e) {
                // Known issue if a Bundle (Parcelable) incorrectly implements writeToParcel
                value = "[Error retrieving \"" + key + "\" value: " + e.getMessage() + "]";
            }

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
     * <p>
     * I originally just wanted to be able to change the System Development Debug Layout property.
     * I thought that I could duplicate what com.android.settings.DevelopmentSettings does:
     * https://github.com/android/platform_packages_apps_settings/blob/master/src/com/android/settings/DevelopmentSettings.java#L941
     * ie: Use Reflection to set the SystemProperty and then pokeSystemProperties
     * </p>
     * <p>
     * After several hours of work I learned that the SystemProperties are ACL protected to only allow the Google
     * Signed Settings app to change them.
     * http://stackoverflow.com/a/11136242 -&gt; http://stackoverflow.com/a/11123609/252308
     * </p>
     * <p>
     * Rather than continue to try to get this to work (if it is even possible),
     * I have chosen to just launch the SettingsActivity DevelopmentSettings fragment.
     * </p>
     * <p>
     * Other references for my wasted efforts:
     * https://github.com/android/platform_packages_apps_settings/blob/master/src/com/android/settings/DevelopmentSettings.java#L1588
     * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/os/SystemProperties.java#L122
     * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/view/View.java#L706
     * https://github.com/Androguide/CMDProcessorLibrary/blob/master/CMDProcessorLibrary/src/com/androguide/cmdprocessor/SystemPropertiesReflection.java
     * </p>
     *
     * @param context context
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

    /*
    /**
     * Reference: http://stackoverflow.com/a/11438245/252308
     *
     * @param context context
     * /
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
    */

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
     * @param view  view
     * @param start start
     * @param kind  kind
     * @param whats whats
     * @param <T>   type of span
     * @return -1 if there are no more spans
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
     * @param delegate delegate
     * @param dx       dx
     * @param dy       dy
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

    /**
     * @param show     if true, shows the progress view and hides the main view;
     *                 if false, shows the main view and hides the progress view
     * @param main     main
     * @param progress progress
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

    public static String viewVisibilityToString(int visibility)
    {
        String name;
        switch (visibility)
        {
            case View.VISIBLE:
                name = "VISIBLE";
                break;
            case View.INVISIBLE:
                name = "INVISIBLE";
                break;
            case View.GONE:
                name = "GONE";
                break;
            default:
                name = "UNKNOWN";
                break;
        }
        return name + '(' + visibility + ')';
    }

    //
    //
    //

    public static String getPlatformInfoString(Context context, LinkedHashMap<String, String> extras)
    {
        Map<String, String> platformInfo = getPlatformInfo(context, extras);

        int widest = 0;
        for (String key : platformInfo.keySet())
        {
            int width = key.length();
            if (width > widest)
            {
                widest = width;
            }
        }

        StringBuilder sb = new StringBuilder();

        for (Entry<String, String> entry : platformInfo.entrySet())
        {
            String key = entry.getKey();
            String value = entry.getValue();

            // right justify
            StringBuilder label = new StringBuilder();
            for (int i = 0; i < widest - key.length(); i++)
            {
                label.append(' ');
            }
            label.append(key);

            sb.append(' ').append(label).append(": ").append(value).append(FooString.LINEFEED);
        }

        return sb.toString();
    }

    public static Map<String, String> getPlatformInfo(
            @NonNull Context context,
            LinkedHashMap<String, String> extras)
    {
        if (extras == null)
        {
            extras = new LinkedHashMap<>();
        }

        // TODO:(pv) More product specific text, Log Limit, Tablet, Dimensions/DPI, etc…

        //Resources res = context.getResources();
        //Configuration configuration = res.getConfiguration();

        String packageName = FooString.quote(getPackageName(context)); // ex: "com.pebblebee.app.hive"
        String appName = FooString.quote(context.getString(R.string.app_name)); // ex: "Pebblebee Hive"
        String appVersion = FooString.quote(getVersionName(context, "0.0.0.1")); // ex: "1.0"
        String appBuild = FooString.quote(Integer.toString(getVersionCode(context, 1))); // ex: "1"
        String osVersion = FooString.quote(getOsVersion()); // ex: "Android 5.1 LOLLIPOP_MR1 (API level 22)"
        String deviceName = FooString.quote(getDeviceName()); // ex: "LGE - Nexus 5"
        String locale = FooString.quote(FooRes.getLocale(context));
        //String deviceId = FooString.quote(getDeviceId(context)); // ex: "35823905966360"
        //String serial = FooString.quote(Build.SERIAL); // ex: "03acaec0003c1cba"
        //String adId = FooString.quote(PbPlatformUtils.getAdvertisingId(mApplicationContext)); // ex: "96bd03b6-defc-4203-83d3-dc1c730801f7"
        //String installationId = FooString.quote(Installation.id(context)); // ex: "eda27c6d-384d-4588-b665-69f3a0ec9fb5"

        // TODO:(pv) Pass these in via Intent Bundle?
        //String personality = Preferences.getSignInPersonalityClass(this).getSimpleName();
        //String username = FooString.quote(mPreferences.getUsername());
        //String server = FooString.quote(mPreferences.getServer());

        //
        // Logging other info to see if it may still be useful if sent inside the log
        //
        String buildId = Build.ID; // ex: "N2G47E"
        FooLog.d(TAG, "getPlatformInfo:       buildId=" + FooString.quote(buildId));
        String buildDisplay = Build.DISPLAY; // ex: "N2G47E"
        FooLog.d(TAG, "getPlatformInfo:  buildDisplay=" + FooString.quote(buildDisplay));
        String buildProduct = Build.PRODUCT; // ex: "marlin"
        FooLog.d(TAG, "getPlatformInfo:  buildProduct=" + FooString.quote(buildProduct));
        String buildDevice = Build.DEVICE; // ex: "marlin"
        FooLog.d(TAG, "getPlatformInfo:   buildDevice=" + FooString.quote(buildDevice));
        String buildBoard = Build.BOARD; // ex: "marlin"
        FooLog.d(TAG, "getPlatformInfo:    buildBoard=" + FooString.quote(buildBoard));
        String buildBrand = Build.BRAND; // ex: "google"
        FooLog.d(TAG, "getPlatformInfo:    buildBrand=" + FooString.quote(buildBrand));
        String buildHardware = Build.HARDWARE; // ex: "marlin"
        FooLog.d(TAG, "getPlatformInfo: buildHardware=" + FooString.quote(buildHardware));
        String buildTags = Build.TAGS; // ex: "release-keys"
        FooLog.d(TAG, "getPlatformInfo:     buildTags=" + FooString.quote(buildTags));
        String buildType = Build.TYPE; // ex: "user"
        FooLog.d(TAG, "getPlatformInfo:     buildType=" + FooString.quote(buildType));
        String localeDefault = Locale.getDefault().toString(); // ex: "en_US"
        FooLog.d(TAG, "getPlatformInfo: localeDefault=" + FooString.quote(localeDefault));
        String orientation = FooRes.orientationToString(FooRes.getOrientation(context)); // ex: ORIENTATION_PORTRAIT(1)
        FooLog.d(TAG, "getPlatformInfo:   orientation=" + FooString.quote(orientation));
        FooLog.d(TAG, "getPlatformInfo: hasAutomotive=" + hasSystemFeatureAutomotive(context));
        FooLog.d(TAG, "getPlatformInfo:  hasTelephony=" + hasSystemFeatureTelephony(context));
        FooLog.d(TAG, "getPlatformInfo: hasTelevision=" + hasSystemFeatureTelevision(context));
        FooLog.d(TAG, "getPlatformInfo:      hasWatch=" + hasSystemFeatureWatch(context));

        Map<String, String> platformInfo = new LinkedHashMap<>();
        platformInfo.put("Package", packageName);
        platformInfo.put("Name", appName);
        platformInfo.put("Version", appVersion + " (Build " + appBuild + ')');
        platformInfo.put("OS", osVersion);
        platformInfo.put("Device", deviceName);
        platformInfo.put("Locale", locale);
        //platformInfo.put("DeviceId", deviceId + " (Serial " + serial + ')');
        //platformInfo.put("AdId", adId);
        //platformInfo.put("InstallId", installationId);
        platformInfo.putAll(extras);

        return platformInfo;
    }
}
