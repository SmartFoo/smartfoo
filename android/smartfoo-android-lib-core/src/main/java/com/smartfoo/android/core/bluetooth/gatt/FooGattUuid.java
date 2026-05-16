package com.smartfoo.android.core.bluetooth.gatt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;

import com.smartfoo.android.core.FooString;

import java.util.UUID;

/**
 * Represents a named Bluetooth GATT UUID with its Bluetooth SIG assigned number.
 *
 * <p>Provides equality comparisons against {@link UUID}, {@link android.os.ParcelUuid},
 * {@link android.bluetooth.BluetoothGattService}, and
 * {@link android.bluetooth.BluetoothGattCharacteristic} for convenient use in switch/lookup
 * scenarios. Instances are created by {@link FooGattUuids} and should not be constructed
 * directly outside the SDK.</p>
 */
public class FooGattUuid
{
    private final int    mAssignedNumber;
    private final String mName;
    private final UUID   mUuid;

    /**
     * Creates a {@code FooGattUuid} from a Bluetooth SIG assigned number.
     * The 128-bit UUID is derived via {@link FooGattUuids#assignedNumberToUUID(int)}.
     *
     * @param assignedNumber the 16-bit Bluetooth SIG assigned number
     * @param name           a human-readable name; must not be null or empty
     */
    public FooGattUuid(int assignedNumber, String name)
    {
        this(FooGattUuids.assignedNumberToUUID(assignedNumber), name);
    }

    FooGattUuid(String uuid, String name)
    {
        this(UUID.fromString(uuid), name);
    }

    FooGattUuid(UUID uuid, String name)
    {
        if (uuid == null)
        {
            throw new IllegalArgumentException("uuid must not be null");
        }
        if (FooString.isNullOrEmpty(name))
        {
            throw new IllegalArgumentException("name must not be null or empty");
        }

        mUuid = uuid;
        mName = name;
        mAssignedNumber = FooGattUuids.getAssignedNumber(mUuid);
    }

    /**
     * Returns a string of the form {@code "\"Name\"(0xXXXX)"} suitable for logging.
     *
     * @return a human-readable representation of this UUID, never null
     */
    @Override
    public String toString()
    {
        return FooString.quote(mName) + '(' + String.format("0x%04X", mAssignedNumber) + ')';
    }

    /**
     * Compares this UUID for equality with another object. Supports comparison against
     * {@link FooGattUuid}, {@link java.util.UUID}, {@link android.os.ParcelUuid},
     * {@link android.bluetooth.BluetoothGattService}, and
     * {@link android.bluetooth.BluetoothGattCharacteristic} by extracting their underlying UUID.
     *
     * @param o the object to compare against
     * @return true if the underlying 128-bit UUIDs are equal
     */
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof FooGattUuid)
        {
            return equals((FooGattUuid) o);
        }
        else if (o instanceof UUID)
        {
            return equals((UUID) o);
        }
        else if (o instanceof ParcelUuid)
        {
            return equals((ParcelUuid) o);
        }
        else if (o instanceof BluetoothGattService)
        {
            return equals((BluetoothGattService) o);
        }
        else if (o instanceof BluetoothGattCharacteristic)
        {
            return equals((BluetoothGattCharacteristic) o);
        }
        else
        {
            return super.equals(o);
        }
    }

    /**
     * Returns true if the underlying UUID of {@code o} equals this UUID.
     *
     * @param o another {@link FooGattUuid} to compare; returns false if null
     * @return true if equal
     */
    public boolean equals(FooGattUuid o)
    {
        return o != null && equals(o.mUuid);
    }

    /**
     * Returns true if the UUID wrapped by {@code o} equals this UUID.
     *
     * @param o a {@link android.os.ParcelUuid} to compare; returns false if null
     * @return true if equal
     */
    public boolean equals(ParcelUuid o)
    {
        return o != null && equals(o.getUuid());
    }

    /**
     * Returns true if the UUID of the given service equals this UUID.
     *
     * @param o a {@link android.bluetooth.BluetoothGattService} to compare; returns false if null
     * @return true if equal
     */
    public boolean equals(BluetoothGattService o)
    {
        return o != null && equals(o.getUuid());
    }

    /**
     * Returns true if the UUID of the given characteristic equals this UUID.
     *
     * @param o a {@link android.bluetooth.BluetoothGattCharacteristic} to compare; returns false if null
     * @return true if equal
     */
    public boolean equals(BluetoothGattCharacteristic o)
    {
        return o != null && equals(o.getUuid());
    }

    /**
     * Returns true if {@code o} equals the underlying 128-bit UUID.
     *
     * @param o a {@link java.util.UUID} to compare
     * @return true if equal
     */
    public boolean equals(UUID o)
    {
        return mUuid.equals(o);
    }

    /**
     * Returns the 16-bit Bluetooth SIG assigned number extracted from the 128-bit UUID.
     *
     * @return the assigned number, e.g. {@code 0x180F} for Battery Service
     */
    public int getAssignedNumber()
    {
        return mAssignedNumber;
    }

    /**
     * Returns the full 128-bit {@link java.util.UUID}.
     *
     * @return the UUID, never null
     */
    public UUID getUuid()
    {
        return mUuid;
    }

    /**
     * Returns the human-readable name for this UUID.
     *
     * @return the name, never null or empty
     */
    public String getName()
    {
        return mName;
    }

    /**
     * Returns this UUID wrapped in a {@link android.os.ParcelUuid} for use with Android APIs.
     *
     * @return a new {@link android.os.ParcelUuid}, never null
     */
    public ParcelUuid getParcelable()
    {
        return new ParcelUuid(mUuid);
    }
}
