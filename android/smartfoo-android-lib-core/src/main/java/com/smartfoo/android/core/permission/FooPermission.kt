package com.smartfoo.android.core.permission

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * Static helpers for querying and requesting Android permissions and system settings.
 *
 * Covers runtime-permission checks ([isCallStatePermissionGranted]), battery optimisation
 * exemption status ([isIgnoringBatteryOptimizations]), and convenience methods for launching
 * the relevant system settings screens. Methods that launch activities add
 * [android.content.Intent.FLAG_ACTIVITY_NEW_TASK] automatically when starting from a
 * non-Activity context.
 */
object FooPermission {
    /**
     * Returns true if [android.Manifest.permission.READ_PHONE_STATE] has been granted.
     *
     * @param context any valid context
     * @return true if the permission is granted
     */
    fun isCallStatePermissionGranted(context: Context) =
        ContextCompat
            .checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Returns true if the app is currently exempt from battery optimisations.
     *
     * @param context any valid context
     * @return true if the system is ignoring battery optimisations for this app
     */
    fun isIgnoringBatteryOptimizations(context: Context) =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName)

    @SuppressLint("BatteryLife")
    fun intentRequestIgnoreBatteryOptimizations(context: Context) =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData("package:${context.packageName}".toUri())

    @SuppressLint("BatteryLife")
    /**
     * Navigates to the most accessible battery-optimisation screen for this app.
     *
     * If already exempt, opens the app info page (easier to locate). Otherwise,
     * opens the direct battery-optimisation request dialog.
     *
     * @param context any valid context
     */
    fun startActivityIgnoreBatteryOptimizations(context: Context) {
        if (isIgnoringBatteryOptimizations(context)) {
            //startActivityIgnoreBatteryOptimizationSettings(context) // this is the recommended way, but it is harder to access
            startActivityAppInfo(context) // this is not the recommended way, but it is easier to access
        } else {
            context.startActivity(
                intentRequestIgnoreBatteryOptimizations(context)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun intentIgnoreBatteryOptimizationSettings() =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)


    fun startActivityIgnoreBatteryOptimizationSettings(context: Context) =
        context.startActivity(
            intentIgnoreBatteryOptimizationSettings()
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    fun intentAppInfo(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData("package:${context.packageName}".toUri())

    fun startActivityAppInfo(context: Context) =
        context.startActivity(
            intentAppInfo(context)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    /**
     * Returns true if the accessibility service identified by [serviceComponent] is enabled.
     *
     * Reads [android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES] and performs a
     * case-insensitive match against the flattened component name.
     *
     * @param context any valid context
     * @param serviceComponent the component name of the accessibility service to check
     * @return true if the service is listed as enabled
     */
    fun isAccessibilityServiceEnabled(
        context: Context,
        serviceComponent: ComponentName,
    ): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = serviceComponent.flattenToString()
        return enabled.split(':').any { it.equals(me, ignoreCase = true) }
    }

    fun intentOpenAccessibilitySettings(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}