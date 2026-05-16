# Package com.smartfoo.android.core.bluetooth.gatt

BLE GATT connection lifecycle management. `FooGattManager` acts as a registry that allocates and tracks `FooGattHandler` instances, one per remote device address, all sharing a common `Looper`. `FooGattHandler` encapsulates the connect/discover-services/read/write/notify state machine for a single peripheral. Supporting classes include `FooGattUtils` for common GATT result-code conversions, `FooGattUuid`/`FooGattUuids` for standard service and characteristic UUID constants, and `BluetoothGattCompat` for cross-API compatibility shims.
