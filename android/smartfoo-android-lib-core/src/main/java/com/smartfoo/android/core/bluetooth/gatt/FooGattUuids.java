package com.smartfoo.android.core.bluetooth.gatt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.UUID;

/**
 * From:
 * https://developer.bluetooth.org/gatt/services/Pages/ServicesHome.aspx
 * https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicsHome.aspx
 * <p/>
 * Other References:
 * https://github.com/movisens/SmartGattLib/blob/master/src/main/java/com/movisens/smartgattlib/Service.java
 * https://github.com/movisens/SmartGattLib/blob/master/src/main/java/com/movisens/smartgattlib/Characteristic.java
 */
public class FooGattUuids
{
    public static final FooGattUuid ALERT_NOTIFICATION_SERVICE                 = new FooGattUuid(0x1811, "Alert Notification Service");
    public static final FooGattUuid ALERT_CATEGORY_ID                          = new FooGattUuid(0x2a43, "Alert Category ID");
    public static final FooGattUuid ALERT_CATEGORY_ID_BIT_MASK                 = new FooGattUuid(0x2a42, "Alert Category ID Bit Mask");
    // https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.alert_level.xml
    public static final FooGattUuid ALERT_LEVEL                                = new FooGattUuid(0x2a06, "Alert Level");
    public static final int         ALERT_LEVEL_NONE                           = 0x00;
    public static final int         ALERT_LEVEL_MILD                           = 0x01;
    public static final int         ALERT_LEVEL_HIGH                           = 0x02;
    public static final FooGattUuid ALERT_NOTIFICATION_CONTROL_POINT           = new FooGattUuid(0x2a44, "Alert Notification Control Point");
    public static final FooGattUuid ALERT_STATUS                               = new FooGattUuid(0x2a3f, "Alert Status");
    public static final FooGattUuid NEW_ALERT                                  = new FooGattUuid(0x2a46, "New Alert");
    //
    public static final FooGattUuid BATTERY_SERVICE                            = new FooGattUuid(0x180f, "Battery Service");
    public static final FooGattUuid BATTERY_LEVEL                              = new FooGattUuid(0x2a19, "Battery Level");
    //
    public static final FooGattUuid BLOOD_PRESSURE_SERVICE                     = new FooGattUuid(0x1810, "Blood Pressure Service");
    public static final FooGattUuid BLOOD_PRESSURE_MEASUREMENT                 = new FooGattUuid(0x2a35, "Blood Pressure Measurement");
    //
    public static final FooGattUuid CYCLING_SPEED_AND_CADENCE_SERVICE          = new FooGattUuid(0x1816, "Cycling Speed and Cadence Service");
    public static final FooGattUuid CYCLING_SPEED_AND_CADENCE_MEASUREMENT      = new FooGattUuid(0x2a5b, "Cycling Speed and Cadence Measurement");
    public static final FooGattUuid CYCLING_SPEED_AND_CADENCE_FEATURE          = new FooGattUuid(0x2a5c, "Cycling Speed and Cadence Feature");
    public static final FooGattUuid CYCLING_SPEED_AND_CADENCE_CONTROL_POINT    = new FooGattUuid(0x2a55, "Speed and Cadence Control Point");
    public static final FooGattUuid SENSOR_LOCATION                            = new FooGattUuid(0x2a5d, "Sensor Location");
    //
    public static final FooGattUuid DEVICE_INFORMATION_SERVICE                 = new FooGattUuid(0x180A, "Device Information Service");
    public static final FooGattUuid MANUFACTURER_NAME                          = new FooGattUuid(0x2A29, "Manufacturer Name String");
    public static final FooGattUuid MODEL_NUMBER                               = new FooGattUuid(0x2a24, "Model Number String");
    public static final FooGattUuid SERIAL_NUMBER                              = new FooGattUuid(0x2a25, "Serial Number String");
    public static final FooGattUuid HARDWARE_REVISION                          = new FooGattUuid(0x2a27, "Hardware Revision String");
    public static final FooGattUuid FIRMWARE_REVISION                          = new FooGattUuid(0x2a26, "Firmware Revision String");
    public static final FooGattUuid SOFTWARE_REVISION                          = new FooGattUuid(0x2a28, "Software Revision String");
    public static final FooGattUuid PNP_ID                                     = new FooGattUuid(0x2a50, "PnP ID");
    //
    public static final FooGattUuid ENVIROMENTAL_SENSING_SERVICE               = new FooGattUuid(0x181A, "Environmental Sensing Service");
    public static final FooGattUuid TEMPERATURE_CHARACTERISTIC                 = new FooGattUuid(0x2A6E, "Temperature");
    //
    //
    // https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-attribute-profile
    //
    public static final FooGattUuid GENERIC_ACCESS_SERVICE                     = new FooGattUuid(0x1800, "Generic Access Service");
    public static final FooGattUuid DEVICE_NAME                                = new FooGattUuid(0x2A00, "Device Name");
    public static final FooGattUuid APPEARANCE                                 = new FooGattUuid(0x2A01, "Appearance");
    public static final FooGattUuid PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = new FooGattUuid(0x2A04, "Peripheral Preferred Connection Parameters");
    public static final FooGattUuid SERVICE_CHANGED                            = new FooGattUuid(0x2A05, "Service Changed");
    public static final FooGattUuid GENERIC_ATTRIBUTE_SERVICE                  = new FooGattUuid(0x1801, "Generic Attribute Service");
    //
    public static final FooGattUuid HEART_RATE_SERVICE                         = new FooGattUuid(0x180d, "Heart Rate Service");
    public static final FooGattUuid HEART_RATE_MEASUREMENT                     = new FooGattUuid(0x2a37, "Heart Rate Measurement");
    public static final FooGattUuid BODY_SENSOR_LOCATION                       = new FooGattUuid(0x2a38, "Body Sensor Location");
    public static final FooGattUuid CLIENT_CHARACTERISTIC_CONFIG               = new FooGattUuid(0x2902, "Client Characteristic Config");
    //
    public static final FooGattUuid IMMEDIATE_ALERT_SERVICE                    = new FooGattUuid(0x1802, "Immediate Alert Service");
    //
    public static final FooGattUuid LINK_LOSS_SERVICE                          = new FooGattUuid(0x1803, "Link Loss Service");
    //
    public static final FooGattUuid RUNNING_SPEED_AND_CADENCE_SERVICE          = new FooGattUuid(0x1814, "Running Speed and Cadence Service");
    public static final FooGattUuid RUNNING_SPEED_AND_CADENCE_MEASUREMENT      = new FooGattUuid(0x2a53, "Running Speed and Cadence Measurement");
    //
    public static final FooGattUuid TX_POWER_SERVICE                           = new FooGattUuid(0x1804, "Tx Power Service");
    public static final FooGattUuid TX_POWER_LEVEL                             = new FooGattUuid(0x2a07, "Tx Power Level");

    private FooGattUuids()
    {
    }

    public static int getAssignedNumber(UUID uuid)
    {
        return (int) ((uuid.getMostSignificantBits() & 0x0000FFFF00000000L) >> 32);
    }

    private static final long GATT_LEAST_SIGNIFICANT_BITS = 0x800000805f9b34fbL;

    public static UUID assignedNumberToUUID(int assignedNumber)
    {
        return new UUID(((long) assignedNumber << 32) | 0x1000, GATT_LEAST_SIGNIFICANT_BITS);
    }

    // Lookup table to allow reverse lookup.
    private static final HashMap<UUID, FooGattUuid> lookup = new HashMap<>();

    /**
     * Reverse look up UUID -> GattUuid
     *
     * @param uuid The UUID to get a look up a GattUuid value for.
     * @return GattUuid that matches the given UUID, or null if not found
     */
    public static FooGattUuid get(UUID uuid)
    {
        if (lookup.size() == 0)
        {
            // Populate the lookup table upon first lookup
            for (Field field : FooGattUuids.class.getDeclaredFields())
            {
                int modifiers = field.getModifiers();
                if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) &&
                        field.getType() == FooGattUuid.class)
                {
                    try
                    {
                        FooGattUuid value = (FooGattUuid) field.get(null);
                        lookup.put(value.getUuid(), value);
                    }
                    catch (IllegalAccessException e)
                    {
                        // unreachable
                    }
                }
            }
        }
        return lookup.get(uuid);
    }

    public static String toString(BluetoothGattService service)
    {
        return (service == null) ? null : toString(service.getUuid());
    }

    public static String toString(BluetoothGattCharacteristic characteristic)
    {
        return (characteristic == null) ? null : toString(characteristic.getUuid());
    }

    public static String toString(BluetoothGattDescriptor descriptor)
    {
        return (descriptor == null) ? null : toString(descriptor.getUuid());
    }

    public static String toString(UUID uuid)
    {
        String s = null;
        FooGattUuid gattUuid = get(uuid);
        if (gattUuid != null)
        {
            s = gattUuid.toString();
        }
        return s;
    }
}