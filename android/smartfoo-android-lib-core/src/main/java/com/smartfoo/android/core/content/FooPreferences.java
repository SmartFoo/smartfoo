package com.smartfoo.android.core.content;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;

import com.smartfoo.android.core.logging.FooLog;

/**
 * Base class for managing Android {@link SharedPreferences} with automatic
 * {@link BackupManager} notification on every write.
 *
 * <p>Subclass this to define typed getters/setters backed by a private preference file.
 * Two named preference files are provided: {@link #FILE_NAME_APP} for application-wide
 * state and {@link #FILE_NAME_USER} for per-user state. All write helpers call
 * {@link BackupManager#dataChanged()} so that the backup service is notified without
 * any extra bookkeeping in the caller.</p>
 */
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

    /**
     * Returns the application context supplied at construction time.
     *
     * @return the application context, never null
     */
    public Context getApplicationContext()
    {
        return mApplicationContext;
    }

    /**
     * Clears all preferences in both the application preference file ({@link #FILE_NAME_APP})
     * and the user preference file ({@link #FILE_NAME_USER}), and notifies the
     * {@link BackupManager}.
     */
    public void clearAll()
    {
        clear(FILE_NAME_APP);
        clearUser();
    }

    /**
     * Clears all preferences in the user preference file ({@link #FILE_NAME_USER}) and notifies
     * the {@link BackupManager}.
     */
    public void clearUser()
    {
        clear(FILE_NAME_USER);
    }

    //
    // Supporting methods…
    //

    /**
     * Returns a private {@link SharedPreferences} instance for the given preference file name.
     *
     * @param name the preference file name (e.g. {@link #FILE_NAME_APP} or {@link #FILE_NAME_USER})
     * @return the preferences object, never null
     */
    public SharedPreferences getPrivatePreferences(String name)
    {
        return mApplicationContext.getSharedPreferences(name, Activity.MODE_PRIVATE);
    }

    /**
     * Removes a single key from the named preference file and notifies the
     * {@link BackupManager}.
     *
     * @param name the preference file name
     * @param key  the key to remove
     */
    @SuppressLint("ApplySharedPref")
    protected void deleteKey(String name, String key)
    {
        getPrivatePreferences(name).edit().remove(key).commit();
        mBackupManager.dataChanged();
    }

    /**
     * Clears all entries in the named preference file and notifies the {@link BackupManager}.
     *
     * @param name the preference file name
     */
    protected void clear(String name)
    {
        clear(getPrivatePreferences(name));
    }

    /**
     * Clears all entries in the given {@link SharedPreferences} and notifies the
     * {@link BackupManager}.
     *
     * @param preferences the preferences to clear; must not be null
     */
    @SuppressLint("ApplySharedPref")
    protected void clear(SharedPreferences preferences)
    {
        preferences.edit().clear().commit();
        mBackupManager.dataChanged();
    }

    /**
     * Reads a String preference value.
     *
     * @param name         the preference file name
     * @param key          the preference key
     * @param defaultValue value to return if the key is not present
     * @return the stored value, or {@code defaultValue} if absent
     */
    protected String getString(String name, String key, String defaultValue)
    {
        return getPrivatePreferences(name).getString(key, defaultValue);
    }

    /**
     * Writes a String preference value and notifies the {@link BackupManager}.
     *
     * @param name  the preference file name
     * @param key   the preference key
     * @param value the value to store
     */
    @SuppressLint("ApplySharedPref")
    protected void setString(String name, String key, String value)
    {
        getPrivatePreferences(name).edit().putString(key, value).commit();
        mBackupManager.dataChanged();
    }

    /**
     * Reads a boolean preference value.
     *
     * @param name         the preference file name
     * @param key          the preference key
     * @param defaultValue value to return if the key is not present
     * @return the stored value, or {@code defaultValue} if absent
     */
    protected boolean getBoolean(String name, String key, boolean defaultValue)
    {
        return getPrivatePreferences(name).getBoolean(key, defaultValue);
    }

    /**
     * Writes a boolean preference value and notifies the {@link BackupManager}.
     *
     * @param name  the preference file name
     * @param key   the preference key
     * @param value the value to store
     */
    @SuppressLint("ApplySharedPref")
    protected void setBoolean(String name, String key, boolean value)
    {
        getPrivatePreferences(name).edit().putBoolean(key, value).commit();
        mBackupManager.dataChanged();
    }

    /**
     * Reads an int preference value.
     *
     * @param name         the preference file name
     * @param key          the preference key
     * @param defaultValue value to return if the key is not present
     * @return the stored value, or {@code defaultValue} if absent
     */
    protected int getInt(String name, String key, int defaultValue)
    {
        return getPrivatePreferences(name).getInt(key, defaultValue);
    }

    /**
     * Writes an int preference value and notifies the {@link BackupManager}.
     *
     * @param name  the preference file name
     * @param key   the preference key
     * @param value the value to store
     */
    @SuppressLint("ApplySharedPref")
    protected void setInt(String name, String key, int value)
    {
        getPrivatePreferences(name).edit().putInt(key, value).commit();
        mBackupManager.dataChanged();
    }

    /**
     * Reads a long preference value.
     *
     * @param name         the preference file name
     * @param key          the preference key
     * @param defaultValue value to return if the key is not present
     * @return the stored value, or {@code defaultValue} if absent
     */
    protected long getLong(String name, String key, int defaultValue)
    {
        return getPrivatePreferences(name).getLong(key, defaultValue);
    }

    /**
     * Writes a long preference value and notifies the {@link BackupManager}.
     *
     * @param name  the preference file name
     * @param key   the preference key
     * @param value the value to store
     */
    @SuppressLint("ApplySharedPref")
    protected void setLong(String name, String key, long value)
    {
        getPrivatePreferences(name).edit().putLong(key, value).commit();
        mBackupManager.dataChanged();
    }
}
