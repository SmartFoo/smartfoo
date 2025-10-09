package com.smartfoo.android.core.platform

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.service.quicksettings.TileService
import android.telephony.TelephonyManager
import android.text.SpannableString
import android.text.Spanned
import android.view.Gravity
import android.view.TouchDelegate
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.net.toUri
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.R
import com.smartfoo.android.core.annotations.NonNullNonEmpty
import com.smartfoo.android.core.logging.FooLog
import com.smartfoo.android.core.logging.FooLog.d
import com.smartfoo.android.core.permissions.FooPermissionsChecker
import java.util.Locale

@Suppress("unused")
object FooPlatformUtils {
    private val TAG = FooLog.TAG(FooPlatformUtils::class.java)

    /**
     * @param context context
     * @param resId   resource id of the string to toast, or -1 for none
     * @param gravity one of Gravity.*
     */
    /**
     * @param context context
     * @param resId   resource id of the string to toast, or -1 for none
     */
    @JvmOverloads
    @JvmStatic
    fun toastLong(context: Context?, resId: Int, gravity: Int = Gravity.CENTER) {
        toast(context, resId, Toast.LENGTH_LONG, gravity)
    }

    /**
     * @param context context
     * @param text    string to toast, or null/empty for none
     * @param gravity one of Gravity.*
     */
    /**
     * @param context context
     * @param text    string to toast, or null/empty for none
     */
    @JvmOverloads
    @JvmStatic
    fun toastLong(context: Context?, text: String?, gravity: Int = Gravity.CENTER) {
        toast(context, text, Toast.LENGTH_LONG, gravity)
    }

    /**
     * @param context context
     * @param resId   resource id of the string to toast, or -1 for none
     * @param gravity one of Gravity.*
     */
    /**
     * @param context context
     * @param resId   resource id of the string to toast, or -1 for none
     */
    @JvmOverloads
    @JvmStatic
    fun toastShort(context: Context?, resId: Int, gravity: Int = Gravity.CENTER) {
        toast(context, resId, Toast.LENGTH_SHORT, gravity)
    }

    /**
     * @param context context
     * @param text    string to toast, or null/empty for none
     * @param gravity one of Gravity.*
     */
    /**
     * @param context context
     * @param text    string to toast, or null/empty for none
     */
    @JvmOverloads
    @JvmStatic
    fun toastShort(context: Context?, text: String?, gravity: Int = Gravity.CENTER) {
        toast(context, text, Toast.LENGTH_SHORT, gravity)
    }

    /**
     * @param context  context
     * @param resId    resource id of the string to toast, or -1 for none
     * @param duration One of Toast.LENGTH_*
     * @param gravity  gravity
     */
    @JvmStatic
    fun toast(context: Context?, resId: Int, duration: Int, gravity: Int) {
        if (context == null) {
            return
        }

        if (resId == -1) {
            return
        }

        toast(context, context.getString(resId), duration, gravity)
    }

    /**
     * @param context  context
     * @param text     string to toast, or null/empty for none
     * @param duration One of Toast.LENGTH_*
     * @param gravity  gravity
     */
    @JvmStatic
    fun toast(context: Context?, text: String?, duration: Int, gravity: Int) {
        if (context == null) {
            return
        }

        if (FooString.isNullOrEmpty(text)) {
            return
        }

        val toast = Toast.makeText(context, text, duration)

        val v = toast.getView()
        if (v != null) {
            val tv = v.findViewById<TextView?>(android.R.id.message)
            tv?.setGravity(gravity)
        }

        toast.show()
    }

    @JvmStatic
    fun getPackageManager(context: Context): PackageManager {
        return context.packageManager
    }

    @JvmStatic
    fun getPackageName(context: Context): String {
        return context.packageName
    }

    @JvmStatic
    fun getApplicationName(context: Context): String? {
        return getApplicationName(context, getPackageName(context))
    }

    @JvmStatic
    fun getApplicationName(context: Context, packageName: String): String? {
        val ai = getApplicationInfo(context, packageName)
        if (ai == null) {
            return null
        }
        return getPackageManager(context).getApplicationLabel(ai).toString()
    }

    /**
     * As of Android 11 (API 30) requires the following in AndroidManifest.xml:
     * <pre>
     * &lt;uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
     * tools:ignore="QueryAllPackagesPermission" /&gt;
    </pre> *
     * See:
     *
     *  *
     * [Package visibility in Android 11](https://medium.com/androiddevelopers/package-visibility-in-android-11-cc857f221cd9)
     * "In rare cases, your app might need to query or interact with all installed apps on a device, independent of the components they contain. To allow your app to see all other installed apps, Android 11 introduces the QUERY_ALL_PACKAGES permission. In an upcoming Google Play policy update, look for guidelines for apps that need the QUERY_ALL_PACKAGES permission."
     *
     *  * [Package visibility filtering on Android](https://developer.android.com/training/package-visibility)
     *
     */
    @JvmStatic
    fun getApplicationInfo(context: Context, packageName: String): ApplicationInfo? {
        try {
            return getPackageManager(context).getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
    }

    /**
     * @param context context
     * @return PackageInfo of the context's package name, or null if one does not exist (should never happen)
     */
    @JvmStatic
    fun getPackageInfo(context: Context): PackageInfo? {
        return getPackageInfo(context, getPackageName(context))
    }

    @JvmStatic
    fun getPackageInfo(context: Context, packageName: String): PackageInfo? {
        try {
            return getPackageManager(context).getPackageInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
    }

    /**
     * @param context      context
     * @param defaultValue defaultValue
     * @return the context's package's versionName, or defaultValue if one does not exist
     */
    @JvmStatic
    fun getVersionName(context: Context, defaultValue: String?): String? {
        val packageInfo = getPackageInfo(context)
        if (packageInfo != null) {
            return packageInfo.versionName
        } else {
            return defaultValue
        }
    }

    /**
     * @param context      context
     * @param defaultValue defaultValue
     * @return the context's package's versionCode, or defaultValue if one does not exist
     */
    @JvmStatic
    fun getVersionCode(context: Context, defaultValue: Int): Int {
        val packageInfo = getPackageInfo(context)
        if (packageInfo != null) {
            return packageInfo.versionCode
        } else {
            return defaultValue
        }
    }

    @JvmStatic
    val deviceName: String
        get() {
            val manufacturer = FooString.capitalize(Build.MANUFACTURER)
            val deviceModel = FooString.capitalize(Build.MODEL)

            val deviceName: String
            if (deviceModel.startsWith(manufacturer)) {
                deviceName = deviceModel
            } else {
                deviceName = "$manufacturer - $deviceModel"
            }

            return deviceName
        }

    /**
     * @param permissionsChecker permissionsChecker
     * @return An ID that is unique for every device
     */
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    @SuppressLint("HardwareIds")
    @JvmStatic
    fun getDeviceId(permissionsChecker: FooPermissionsChecker): String? {
        var deviceId: String? = null

        val permissionsDenied =
            permissionsChecker.checkPermission(Manifest.permission.READ_PHONE_STATE)
        if (!permissionsDenied.contains(Manifest.permission.READ_PHONE_STATE)) {
            val context = permissionsChecker.getContext()
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            deviceId = telephonyManager.getDeviceId()
        }

        val repeatingPattern = "((.)\\2+)"

        if (deviceId == null || deviceId.matches(repeatingPattern.toRegex())) {
            deviceId = Build.SERIAL
        }

        return deviceId
    }

    @JvmStatic
    val osVersion: String
        get() {
            val builder = StringBuilder()

            builder.append("Android ").append(Build.VERSION.RELEASE)

            val fields =
                VERSION_CODES::class.java.getFields()
            for (field in fields) {
                val fieldName = field.getName()

                var fieldValue = -1
                try {
                    fieldValue = field.getInt(Any())
                } catch (e: IllegalArgumentException) {
                    // ignore
                } catch (e: IllegalAccessException) {
                } catch (e: NullPointerException) {
                }

                if (fieldValue == Build.VERSION.SDK_INT) {
                    if (!FooString.isNullOrEmpty(fieldName)) {
                        builder.append(' ').append(fieldName)
                    }
                    builder.append(" (API level ").append(fieldValue).append(')')
                    break
                }
            }

            return builder.toString()
        }

    @JvmStatic
    fun hasSystemFeature(context: Context, name: String): Boolean {
        return getPackageManager(context).hasSystemFeature(name)
    }

    @JvmStatic
    fun hasSystemFeatureAutomotive(context: Context): Boolean {
        return hasSystemFeature(context, PackageManager.FEATURE_AUTOMOTIVE)
    }

    @JvmStatic
    fun hasSystemFeatureTelephony(context: Context): Boolean {
        return hasSystemFeature(context, PackageManager.FEATURE_TELEPHONY)
    }

    @JvmStatic
    fun hasSystemFeatureTelevision(context: Context): Boolean {
        return hasSystemFeature(context, PackageManager.FEATURE_LEANBACK)
    }

    @JvmStatic
    fun hasSystemFeatureWatch(context: Context): Boolean {
        return hasSystemFeature(context, PackageManager.FEATURE_WATCH)
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
    @JvmStatic
    fun getMetaData(context: Context): Bundle? {
        var metaDataBundle: Bundle? = null

        val packageName = getPackageName(context)
        val packageManager = context.getPackageManager()

        var applicationInfo: ApplicationInfo?
        try {
            applicationInfo =
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            applicationInfo = null
        }

        if (applicationInfo != null) {
            metaDataBundle = applicationInfo.metaData
        }

        return metaDataBundle
    }

    @JvmStatic
    fun getMetaDataString(context: Context, key: String?, defaultValue: String?): String? {
        var value: String? = null
        val metaDataBundle = getMetaData(context)
        if (metaDataBundle != null) {
            value = metaDataBundle.getString(key, defaultValue)
        }
        return value
    }

    @JvmStatic
    fun getMetaDataInt(context: Context, key: String?, defaultValue: Int): Int {
        var value = defaultValue
        val metaDataBundle = getMetaData(context)
        if (metaDataBundle != null) {
            value = metaDataBundle.getInt(key, defaultValue)
        }
        return value
    }

    @JvmStatic
    fun getMetaDataBoolean(context: Context, key: String?, defaultValue: Boolean): Boolean {
        var value = defaultValue
        val metaDataBundle = getMetaData(context)
        if (metaDataBundle != null) {
            value = metaDataBundle.getBoolean(key, defaultValue)
        }
        return value
    }

    @JvmStatic
    fun toString(intent: Intent?): String {
        if (intent == null) return "null"
        val sb = StringBuilder()
        sb.append(intent) // only prints "(has extras)" for extras
        sb.append(", extras=").append(toString(intent.extras)) // show extras
        return sb.toString()
    }

    /**
     * May be unnecessary; [android.os.Bundle]`.toString` output seems almost acceptable nowadays.
     */
    @JvmStatic
    fun toString(bundle: Bundle?): String {
        if (bundle == null) return "null"

        val sb = StringBuilder()

        val keys = bundle.keySet()
        val it = keys.iterator()

        sb.append('{')
        while (it.hasNext()) {
            val key = it.next()
            var value = try {
                /**
                 * [android.os.BaseBundle.get] calls hidden method [android.os.BaseBundle.getValue].
                 * `android.os.BaseBundle#getValue(java.lang.String)` says:
                 * "Deprecated: Use `getValue(String, Class, Class[])`. This method should only be used in other deprecated APIs."
                 * That first sentence does not help this method that dynamically enumerates the Bundle entries without awareness/concern of any types.
                 * That second sentence tells me they probably won't be getting rid of android.os.BaseBundle#get(java.lang.String) any time soon.
                 * So marking deprecated `android.os.BaseBundle#get(java.lang.String)` as safe to call... for awhile.
                 */
                @Suppress("DEPRECATION")
                bundle.get(key)
            } catch (e: RuntimeException) {
                // Known issue if a Bundle (Parcelable) incorrectly implements writeToParcel
                "[Error retrieving \"$key\" value: ${e.message}]"
            }

            sb.append(FooString.quote(key)).append('=')

            if (key.lowercase(Locale.getDefault()).contains("password")) {
                value = "*REDACTED*"
            }

            if (value is Bundle) {
                sb.append(toString(value))
            } else if (value is Intent) {
                sb.append(toString(value))
            } else {
                sb.append(FooString.quote(value))
            }

            if (it.hasNext()) {
                sb.append(", ")
            }
        }
        sb.append('}')

        return sb.toString()
    }

    @JvmOverloads
    @JvmStatic
    fun startActivity(context: Context, activityClass: Class<*>, bundle: Bundle? = null) {
        startActivity(context, Intent(context, activityClass), bundle)
    }

    @JvmOverloads
    @JvmStatic
    fun startActivity(context: Context, intent: Intent, bundle: Bundle? = null) {
        /*
        if (context is Application) {
            // TODO Use Application.ActivityLifecycleCallbacks (like in AlfredAI) to actually test for background or not
            /*
            // Background startActivity requires FLAG_ACTIVITY_NEW_TASK
            if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) == 0)
            {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            */
        }
        */
        context.startActivity(intent, bundle)
    }

    /**
     *
     *
     * I originally just wanted to be able to change the System Development Debug Layout property.
     * I thought that I could duplicate what com.android.settings.DevelopmentSettings does:
     * https://cs.android.com/android/platform/superproject/+/android-7.1.2_r39:packages/apps/Settings/src/com/android/settings/DevelopmentSettings.java
     * Currently (2025/03) that code has moved to under:
     * https://cs.android.com/android/platform/superproject/main/+/main:packages/apps/Settings/src/com/android/settings/development/
     *
     * ie: Use Reflection to set the SystemProperty and then pokeSystemProperties
     *
     *
     *
     * After several hours of work I learned that the SystemProperties are ACL protected to only allow the Google
     * Signed Settings app to change them.
     * http://stackoverflow.com/a/11136242 -&gt; http://stackoverflow.com/a/11123609/252308
     *
     *
     *
     * Rather than continue to try to get this to work (if it is even possible),
     * I have chosen to just launch the SettingsActivity DevelopmentSettings fragment.
     *
     *
     *
     * Other references for my wasted efforts:
     * https://github.com/android/platform_packages_apps_settings/blob/master/src/com/android/settings/DevelopmentSettings.java#L1588
     * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/os/SystemProperties.java#L122
     * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/view/View.java#L706
     * https://github.com/Androguide/CMDProcessorLibrary/blob/master/CMDProcessorLibrary/src/com/androguide/cmdprocessor/SystemPropertiesReflection.java
     *
     *
     * @param context context
     */
    @JvmStatic
    fun showDevelopmentSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        // DevelopmentSettings appears to not have any arguments. :(
        // https://github.com/android/platform_packages_apps_settings/blob/master/src/com/android/settings/DevelopmentSettings.java
        startActivity(context, intent)
    }

    /**
     * This has a sometimes annoying side-effect of Back not being able to exit the Activity.
     * Even Android's built in Notification Tile "Wireless debug settings" exhibits the same problem.
     * When this happens just do what you always do to close an Activity:
     *   swipe up from the bottom and then swipe the Activity up to close it.
     *
     * https://stackoverflow.com/a/74859391/252308
     *
     * https://cs.android.com/android/platform/superproject/main/+/main:packages/apps/Settings/src/com/android/settings/development/AdbWirelessDialog.java
     * https://cs.android.com/android/platform/superproject/+/android-14.0.0_r61:packages/apps/Settings/src/com/android/settings/development/AdbWirelessDialog.java
     * https://cs.android.com/android/platform/superproject/+/android-14.0.0_r61:packages/apps/Settings/src/com/android/settings/development/AdbWirelessDialogController.java
     *
     * @param context
     */
    @JvmStatic
    fun showAdbWirelessSettings(context: Context) {
        /*

        Hints of how Android Settings -> Development Settings does something similar:
        https://cs.android.com/android/platform/superproject/+/android-15.0.0_r23:packages/apps/Settings/AndroidManifest.xml?q=DevelopmentSettingsActivity

        ```
        <activity
            android:name="Settings$DevelopmentSettingsActivity"
            android:label="@string/development_settings_title"
            android:icon="@drawable/ic_settings_development"
            android:exported="true">
            <intent-filter android:priority="1">
                <action android:name="android.settings.APPLICATION_DEVELOPMENT_SETTINGS" />
                <action android:name="com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS" />
                <action android:name="android.service.quicksettings.action.QS_TILE_PREFERENCES"/>
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
            <meta-data android:name="com.android.settings.FRAGMENT_CLASS"
                       android:value="com.android.settings.development.DevelopmentSettingsDashboardFragment" />
            <meta-data android:name="com.android.settings.HIGHLIGHT_MENU_KEY"
                       android:value="@string/menu_key_system"/>
            <meta-data android:name="com.android.settings.PRIMARY_PROFILE_CONTROLLED"
                       android:value="true" />
        </activity>
        ```

        `Settings$DevelopmentSettingsActivity` is a stub activity that
        https://cs.android.com/android/platform/superproject/+/android-15.0.0_r23:packages/apps/Settings/src/com/android/settings/Settings.java
        reads the meta-data to redirect to DevelopmentSettingsDashboardFragment:
        https://cs.android.com/android/platform/superproject/+/android-15.0.0_r23:packages/apps/Settings/src/com/android/settings/development/DevelopmentSettingsDashboardFragment.java

        */

        val pkg = "com.android.settings"
        val cls = "com.android.settings.development.qstile.DevelopmentTiles\$WirelessDebugging"
        val componentName = ComponentName(pkg, cls)

        // "android.service.quicksettings.action.QS_TILE_PREFERENCES"
        val intent = Intent(TileService.ACTION_QS_TILE_PREFERENCES)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME, componentName)
        startActivity(context, intent)
    }

    @JvmStatic
    fun showGooglePlay(context: Context, packageName: String?) {
        try {
            val uri = "market://details?id=$packageName".toUri()
            startActivity(context, Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            val uri = "https://play.google.com/store/apps/details?id=$packageName".toUri()
            startActivity(context, Intent(Intent.ACTION_VIEW, uri))
        }
    }

    @JvmOverloads
    @JvmStatic
    fun showAppSettings(context: Context, packageName: String? = context.packageName) {
        val uri = "package:$packageName".toUri()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
        startActivity(context, intent)
    }

    @JvmStatic
    fun showBatterySettings(context: Context) {
        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        if (resolveInfo != null) {
            startActivity(context, intent)
        }
    }

    private fun intentAppNotificationSettings(ctx: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        }

    @JvmStatic
    fun showAppNotificationSettings(ctx: Context) {
        startActivity(ctx, intentAppNotificationSettings(ctx))
    }

    /*
    /**
     * Reference: http://stackoverflow.com/a/11438245/252308
     *
     * @param context context
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
    */

    @JvmStatic
    fun showSoftInput(view: View, show: Boolean) {
        requireNotNull(view) { "view must not be null" }

        val context = view.getContext()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if (show) {
            imm.showSoftInput(view, 0)
        } else {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0)
        }
    }

    /**
     * @param view  view
     * @param start start
     * @param kind  kind
     * @param whats whats
     * @param <T>   type of span
     * @return -1 if there are no more spans
    </T> */
    @JvmStatic
    fun <T> setNextSpan(view: TextView, start: Int, kind: Class<T?>?, whats: Array<Any?>): Int {
        var start = start
        val charSequence = view.getText()

        val length = charSequence.length

        val spannable =
            if (charSequence is SpannableString) charSequence else SpannableString.valueOf(
                charSequence
            )

        // For some reason "spannable.nextSpanTransition(...)" doesn't seem to work correctly if the string *starts* w/ the given span type
        val spans = spannable.getSpans<T?>(start, length, kind)
        if (spans == null || spans.size == 0) {
            return -1
        }

        start = spannable.getSpanStart(spans[0])
        val end = spannable.getSpanEnd(spans[0])

        for (what in whats) {
            spannable.setSpan(what, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        view.setText(spannable)

        //MovementMethod m = view.getMovementMethod();
        //if ((m == null) || !(m instanceof LinkMovementMethod))
        //{
        //    view.setMovementMethod(LinkMovementMethod.getInstance());
        //}
        return end
    }

    /**
     * @param delegate delegate
     * @param dx       dx
     * @param dy       dy
     */
    @JvmStatic
    fun enlargeHitRect(delegate: View, dx: Int, dy: Int) {
        val parent = delegate.getParent() as View
        parent.post(object : Runnable {
            override fun run() {
                val r = Rect()
                delegate.getHitRect(r)
                r.inset(-dx, -dy)
                parent.setTouchDelegate(TouchDelegate(r, delegate))
            }
        })
    }

    /**
     * @param show     if true, shows the progress view and hides the main view;
     * if false, shows the main view and hides the progress view
     * @param main     main
     * @param progress progress
     */
    @TargetApi(VERSION_CODES.HONEYCOMB_MR2)
    @JvmStatic
    fun showProgress(show: Boolean, main: View, progress: View) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime =
                main.getResources().getInteger(android.R.integer.config_shortAnimTime)

            main.visibility = if (show) View.GONE else View.VISIBLE
            main.animate()
                .setDuration(shortAnimTime.toLong())
                .alpha((if (show) 0 else 1).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        main.setVisibility(if (show) View.GONE else View.VISIBLE)
                    }
                })

            progress.visibility = if (show) View.VISIBLE else View.GONE
            progress.animate()
                .setDuration(shortAnimTime.toLong())
                .alpha((if (show) 1 else 0).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        progress.visibility = if (show) View.VISIBLE else View.GONE
                    }
                })
        } else {
            // The ViewPropertyAnimator APIs are not available,
            // so simply show and hide the relevant UI components.
            main.visibility = if (show) View.VISIBLE else View.GONE
            progress.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    @JvmStatic
    fun supportsViewElevation(): Boolean {
        return (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP)
    }

    @JvmStatic
    fun viewVisibilityToString(visibility: Int): String {
        val name = when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> "UNKNOWN"
        }
        return "$name($visibility)"
    }

    //
    //
    //
    @JvmStatic
    fun getPlatformInfoString(context: Context, extras: LinkedHashMap<String, String?>?): String {
        val platformInfo = getPlatformInfo(context, extras)

        var widest = 0
        for (key in platformInfo.keys) {
            val width = key.length
            if (width > widest) {
                widest = width
            }
        }

        val sb = StringBuilder()

        for (entry in platformInfo.entries) {
            val key = entry.key
            val value = entry.value

            // right justify
            val label = StringBuilder()
            for (i in 0..<widest - key.length) {
                label.append(' ')
            }
            label.append(key)

            sb.append(' ').append(label).append(": ").append(value).append(FooString.LINEFEED)
        }

        return sb.toString()
    }

    @JvmStatic
    fun getPlatformInfo(
        context: Context,
        extras: Map<String, String?>?
    ): MutableMap<String, String?> {
        var extras = extras
        if (extras == null) {
            extras = mapOf()
        }

        // TODO:(pv) More product specific text, Log Limit, Tablet, Dimensions/DPI, etc…

        //Resources res = context.getResources();
        //Configuration configuration = res.getConfiguration();
        val packageName = FooString.quote(getPackageName(context)) // ex: "com.pebblebee.app.hive"
        val appName = FooString.quote(context.getString(R.string.app_name)) // ex: "Pebblebee Hive"
        val appVersion = FooString.quote(getVersionName(context, "0.0.0.1")) // ex: "1.0"
        val appBuild = FooString.quote(getVersionCode(context, 1).toString()) // ex: "1"
        val osVersion = FooString.quote(osVersion) // ex: "Android 5.1 LOLLIPOP_MR1 (API level 22)"
        val deviceName = FooString.quote(deviceName) // ex: "LGE - Nexus 5"
        val locale = FooString.quote(FooRes.getLocale(context))

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
        val buildId = Build.ID // ex: "N2G47E"
        d(TAG, "getPlatformInfo:       buildId=${FooString.quote(buildId)}")
        val buildDisplay = Build.DISPLAY // ex: "N2G47E"
        d(TAG, "getPlatformInfo:  buildDisplay=${FooString.quote(buildDisplay)}")
        val buildProduct = Build.PRODUCT // ex: "marlin"
        d(TAG, "getPlatformInfo:  buildProduct=${FooString.quote(buildProduct)}")
        val buildDevice = Build.DEVICE // ex: "marlin"
        d(TAG, "getPlatformInfo:   buildDevice=${FooString.quote(buildDevice)}")
        val buildBoard = Build.BOARD // ex: "marlin"
        d(TAG, "getPlatformInfo:    buildBoard=${FooString.quote(buildBoard)}")
        val buildBrand = Build.BRAND // ex: "google"
        d(TAG, "getPlatformInfo:    buildBrand=${FooString.quote(buildBrand)}")
        val buildHardware = Build.HARDWARE // ex: "marlin"
        d(TAG, "getPlatformInfo: buildHardware=${FooString.quote(buildHardware)}")
        val buildTags = Build.TAGS // ex: "release-keys"
        d(TAG, "getPlatformInfo:     buildTags=${FooString.quote(buildTags)}")
        val buildType = Build.TYPE // ex: "user"
        d(TAG, "getPlatformInfo:     buildType=${FooString.quote(buildType)}")
        val localeDefault = Locale.getDefault().toString() // ex: "en_US"
        d(TAG, "getPlatformInfo: localeDefault=${FooString.quote(localeDefault)}")
        val orientation = FooRes.orientationToString(FooRes.getOrientation(context)) // ex: ORIENTATION_PORTRAIT(1)
        d(TAG, "getPlatformInfo:   orientation=${FooString.quote(orientation)}")
        d(TAG, "getPlatformInfo: hasAutomotive=${hasSystemFeatureAutomotive(context)}")
        d(TAG, "getPlatformInfo:  hasTelephony=${hasSystemFeatureTelephony(context)}")
        d(TAG, "getPlatformInfo: hasTelevision=${hasSystemFeatureTelevision(context)}")
        d(TAG, "getPlatformInfo:      hasWatch=${hasSystemFeatureWatch(context)}")

        val platformInfo = mutableMapOf<String, String?>()
        platformInfo.put("Package", packageName)
        platformInfo.put("Name", appName)
        platformInfo.put("Version", "$appVersion (Build $appBuild)")
        platformInfo.put("OS", osVersion)
        platformInfo.put("Device", deviceName)
        platformInfo.put("Locale", locale)
        //platformInfo.put("DeviceId", deviceId + " (Serial " + serial + ')');
        //platformInfo.put("AdId", adId);
        //platformInfo.put("InstallId", installationId);
        platformInfo.putAll(extras)

        return platformInfo
    }
}
