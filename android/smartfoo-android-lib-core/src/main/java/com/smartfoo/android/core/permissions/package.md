# Package com.smartfoo.android.core.permissions

Runtime permission request flow orchestration. `FooPermissionsChecker` drives the full `ActivityCompat.requestPermissions` flow: it queries which of the required permissions are already granted, requests the missing ones, and dispatches results via `FooPermissionsHandler` callbacks for granted, denied, and rationale cases. Works from both `Activity` and `Fragment` hosts.
