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

    @Override
    public String toString()
    {
        return FooString.quote(mName) + '(' + String.format("0x%04X", mAssignedNumber) + ')';
    }

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

    public boolean equals(FooGattUuid o)
    {
        return o != null && equals(o.mUuid);
    }

    public boolean equals(ParcelUuid o)
    {
        return o != null && equals(o.getUuid());
    }

    public boolean equals(BluetoothGattService o)
    {
        return o != null && equals(o.getUuid());
    }

    public boolean equals(BluetoothGattCharacteristic o)
    {
        return o != null && equals(o.getUuid());
    }

    public boolean equals(UUID o)
    {
        return mUuid.equals(o);
    }

    public int getAssignedNumber()
    {
        return mAssignedNumber;
    }

    public UUID getUuid()
    {
        return mUuid;
    }

    public String getName()
    {
        return mName;
    }

    public ParcelUuid getParcelable()
    {
        return new ParcelUuid(mUuid);
    }
}
