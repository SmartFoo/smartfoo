# Package com.smartfoo.android.core.permission

Individual permission query and intent helpers. `FooPermission` provides static functions to check specific runtime permissions (e.g. `READ_PHONE_STATE`) and manage battery-optimisation exemptions: `isIgnoringBatteryOptimizations`, `intentRequestIgnoreBatteryOptimizations`, and `startActivityIgnoreBatteryOptimizations` navigate users to the correct system settings screen or the direct exemption dialog.
