# Package com.smartfoo.android.core.bluetooth

High-level Bluetooth management for Android. `FooBluetoothManager` wraps the platform `BluetoothManager` and `BluetoothAdapter` and exposes whether classic Bluetooth and Bluetooth Low Energy are supported on the device. `FooBluetoothAdapterStateListener` delivers adapter on/off state changes via a callback interface, and `FooBluetoothAudioConnectionListener` tracks Bluetooth audio (A2DP/SCO) connection events. Utility methods for common adapter checks live in `FooBluetoothUtils`.
