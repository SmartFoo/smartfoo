package com.smartfoo.android.core.content;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;

import com.smartfoo.android.core.logging.FooLog;

public class FooPreferences
{
    private static final String TAG = FooLog.TAG(FooPreferences.class);

    protected static final String FILE_NAME_APP  = "pref_file_app";
    public static final    String FILE_NAME_USER = "pref_file_user";

    // TODO:(pv) load JNI library that allows encrypted values

    protected final Context       mApplicationContext;
    private final   BackupManager mBackupManager;

    public FooPreferences(Context applicationContext)
    {
        mApplicationContext = applicationContext;
        mBackupManager = new BackupManager(applicationContext);
    }

    public Context getApplicationContext()
    {
        return mApplicationContext;
    }

    public void clearAll()
    {
        clear(FILE_NAME_APP);
        clearUser();
    }

    public void clearUser()
    {
        clear(FILE_NAME_USER);
    }

    //
    // Supporting methods...
    //

    public SharedPreferences getPrivatePreferences(String name)
    {
        return mApplicationContext.getSharedPreferences(name, Activity.MODE_PRIVATE);
    }

    @SuppressLint("ApplySharedPref")
    protected void deleteKey(String name, String key)
    {
        getPrivatePreferences(name).edit().remove(key).commit();
        mBackupManager.dataChanged();
    }

    protected void clear(String name)
    {
        clear(getPrivatePreferences(name));
    }

    @SuppressLint("ApplySharedPref")
    protected void clear(SharedPreferences preferences)
    {
        preferences.edit().clear().commit();
        mBackupManager.dataChanged();
    }

    protected String getString(String name, String key, String defaultValue)
    {
        return getPrivatePreferences(name).getString(key, defaultValue);
    }

    @SuppressLint("ApplySharedPref")
    protected void setString(String name, String key, String value)
    {
        getPrivatePreferences(name).edit().putString(key, value).commit();
        mBackupManager.dataChanged();
    }

    protected boolean getBoolean(String name, String key, boolean defaultValue)
    {
        return getPrivatePreferences(name).getBoolean(key, defaultValue);
    }

    @SuppressLint("ApplySharedPref")
    protected void setBoolean(String name, String key, boolean value)
    {
        getPrivatePreferences(name).edit().putBoolean(key, value).commit();
        mBackupManager.dataChanged();
    }

    protected int getInt(String name, String key, int defaultValue)
    {
        return getPrivatePreferences(name).getInt(key, defaultValue);
    }

    @SuppressLint("ApplySharedPref")
    protected void setInt(String name, String key, int value)
    {
        getPrivatePreferences(name).edit().putInt(key, value).commit();
        mBackupManager.dataChanged();
    }

    protected long getLong(String name, String key, int defaultValue)
    {
        return getPrivatePreferences(name).getLong(key, defaultValue);
    }

    @SuppressLint("ApplySharedPref")
    protected void setLong(String name, String key, long value)
    {
        getPrivatePreferences(name).edit().putLong(key, value).commit();
        mBackupManager.dataChanged();
    }
}
