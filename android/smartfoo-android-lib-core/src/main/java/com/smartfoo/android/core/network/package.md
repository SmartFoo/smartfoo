# Package com.smartfoo.android.core.network

Network and cellular state monitoring. `FooDataConnectionManager` unifies `FooDataConnectionListener` (Wi-Fi/mobile data connectivity) and `FooCellularStateListener` (phone call hook state) into a single listener-based manager that blocks data usage while a voice call is active or the network is disconnected. `FooDataConnectionListener` uses `ConnectivityManager` to track network availability, and `FooCellularStateListener` uses `TelephonyManager` to track off-hook/on-hook transitions.
