# Package com.smartfoo.android.core.notification

Notification construction and listening. `FooNotificationBuilder` wraps `NotificationCompat.Builder` with a fluent API and convenience overloads for resource IDs. `FooNotificationService` is a base `NotificationListenerService` that delegates posted/removed events through `FooNotificationListenerManager` to registered `FooNotificationListener` callbacks. `FooNotification` and `FooNotificationReceiver` provide common notification data structures and broadcast-receiver integration.
