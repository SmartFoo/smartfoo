package com.smartfoo.android.core.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FooPermissionsChecker
{
    private static final String TAG = FooLog.TAG(FooPermissionsChecker.class);

    public interface FooPermissionsHandler
    {
        String[] getRequiredPermissions();

        void onAllPermissionsGranted();

        String getPermissionRequiredToText(int requestCode, String permissionDenied);

        void onRequestPermissionsDenied(int requestCode, String[] permissionsDenied);

        void onRequestPermissionsGranted(int requestCode, String[] permissionsGranted);
    }

    public interface FooPermissionsRequestCodeCallbacks
    {
        int getPermissionsRequestCode();
    }

    public static abstract class FooPermissionsCheckerCallbacks
    {
        private final Context                            mContext;
        private final FooPermissionsRequestCodeCallbacks mPermissionsRequestCodeCallbacks;

        public FooPermissionsCheckerCallbacks(Context context, FooPermissionsRequestCodeCallbacks permissionsRequestCodeCallbacks)
        {
            if (context == null)
            {
                throw new IllegalArgumentException("context must not be null");
            }

            if (permissionsRequestCodeCallbacks == null)
            {
                throw new IllegalArgumentException("permissionsRequestCodeCallbacks must not be null");
            }

            mContext = context;
            mPermissionsRequestCodeCallbacks = permissionsRequestCodeCallbacks;
        }

        public Context getContext()
        {
            return mContext;
        }

        /**
         * Returns the request code that will be passed to
         * {@link ActivityCompat#requestPermissions} and echoed back in
         * {@link Activity#onRequestPermissionsResult}.
         *
         * @return the permissions request code
         */
        public int getPermissionsRequestCode()
        {
            return mPermissionsRequestCodeCallbacks.getPermissionsRequestCode();
        }

        /**
         * Called when one or more requested permissions have been denied by the OS. Subclasses
         * should show appropriate UI (e.g. a rationale dialog) and return whether the checker
         * should continue.
         *
         * @param requestCode       the request code from {@link #getPermissionsRequestCode()}
         * @param permissionsDenied the permissions that were denied; never null
         * @return true to halt further processing, false to allow the checker to proceed
         */
        protected abstract boolean onPermissionsDenied(int requestCode, String[] permissionsDenied);
    }

    private final FooPermissionsCheckerCallbacks mCallbacks;

    public FooPermissionsChecker(FooPermissionsCheckerCallbacks callbacks)
    {
        if (callbacks == null)
        {
            throw new IllegalArgumentException("callbacks must not be null");
        }

        mCallbacks = callbacks;
    }

    /**
     * Returns the {@link android.content.Context} provided by the callbacks object.
     *
     * @return the context, never null
     */
    public Context getContext()
    {
        return mCallbacks.getContext();
    }

    /**
     * Returns the request code used when calling
     * {@link ActivityCompat#requestPermissions} so the Activity can route the result back to
     * this checker.
     *
     * @return the permissions request code
     */
    public int getPermissionsRequestCode()
    {
        return mCallbacks.getPermissionsRequestCode();
    }

    /**
     * Checks whether a single permission is granted and, if not, invokes the denied callback.
     *
     * @param permissionRequested one of the {@link android.Manifest.permission} constants
     * @return an unmodifiable list of denied permissions; empty if the permission is granted
     */
    public List<String> checkPermission(String permissionRequested)
    {
        return checkPermissions(new String[] { permissionRequested });
    }

    /**
     * Checks whether the given permission is currently granted in the given context.
     *
     * @param context    the context used to perform the check; must not be null
     * @param permission one of the {@link android.Manifest.permission} constants; must not be null
     * @return true if the permission is granted
     */
    public static boolean isPermissionGranted(@NonNull Context context, @NonNull String permission)
    {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks each of the requested permissions. For any that are not yet granted, collects them
     * into a list and calls {@link FooPermissionsCheckerCallbacks#onPermissionsDenied} so the
     * host can show rationale UI and forward the runtime-permission request.
     *
     * @param permissionsRequested an array of {@link android.Manifest.permission} constants to
     *                             check; ignored if null or empty
     * @return an unmodifiable list of denied permissions (may be empty); never null
     */
    public List<String> checkPermissions(String[] permissionsRequested)
    {
        FooLog.i(TAG, "checkPermissions(permissionsRequested=" + FooString.toString(permissionsRequested) + ')');

        List<String> permissionsDenied = new LinkedList<>();

        if (permissionsRequested != null)
        {
            Context context = mCallbacks.getContext();

            for (String permissionRequested : permissionsRequested)
            {
                if (!isPermissionGranted(context, permissionRequested))
                {
                    permissionsDenied.add(permissionRequested);
                }
            }
        }

        if (permissionsDenied.size() > 0)
        {
            int requestCode = getPermissionsRequestCode();
            FooLog.i(TAG, "checkPermissions: requestCode=" + requestCode);

            mCallbacks.onPermissionsDenied(requestCode, permissionsDenied.toArray(new String[permissionsDenied.size()]));
        }

        return Collections.unmodifiableList(permissionsDenied);
    }

    /**
     * {@link Activity#onRequestPermissionsResult(int, String[], int[])} will get the result of the request.
     *
     * @param activity    activity
     * @param permission  One of {@link android.Manifest.permission}.*
     * @param requestCode requestCode
     */
    @SuppressWarnings("unused")
    public static void requestPermission(Activity activity, String permission, int requestCode)
    {
        requestPermissions(activity,
                new String[] { permission },
                requestCode);
    }

    /**
     * {@link Activity#onRequestPermissionsResult(int, String[], int[])} will get the result of the request.
     *
     * @param activity    activity
     * @param permissions One of {@link android.Manifest.permission}.*
     * @param requestCode requestCode
     */
    @SuppressWarnings("unused")
    public static void requestPermissions(Activity activity, List<String> permissions, int requestCode)
    {
        requestPermissions(activity,
                permissions == null ? new String[] {} : permissions.toArray(new String[permissions.size()]),
                requestCode);
    }

    /**
     * {@link Activity#onRequestPermissionsResult(int, String[], int[])} will get the result of the request.
     *
     * @param activity    activity
     * @param permissions One of {@link android.Manifest.permission}.*
     * @param requestCode requestCode
     */
    public static void requestPermissions(Activity activity, String[] permissions, int requestCode)
    {
        FooLog.i(TAG,
                "requestPermissions(activity=" + activity +
                ", permissions=" + FooString.toString(permissions) +
                ", requestCode=" + requestCode + ')');
        if (!(activity instanceof OnRequestPermissionsResultCallback))
        {
            throw new IllegalArgumentException("activity must implement android.support.v4.app.OnRequestPermissionsResultCallback");
        }
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    /**
     * {@link Fragment#onRequestPermissionsResult(int, String[], int[])} will get the result of the request.
     *
     * @param fragment    fragment
     * @param permission  One of {@link android.Manifest.permission}.*
     * @param requestCode requestCode
     */
    @SuppressWarnings("unused")
    public static void requestPermission(Fragment fragment, String permission, int requestCode)
    {
        requestPermissions(fragment,
                new String[] { permission },
                requestCode);
    }

    /**
     * {@link Fragment#onRequestPermissionsResult(int, String[], int[])} will get the result of the request.
     *
     * @param fragment    fragment
     * @param permissions One of {@link android.Manifest.permission}.*
     * @param requestCode requestCode
     */
    @SuppressWarnings("unused")
    public static void requestPermissions(Fragment fragment, List<String> permissions, int requestCode)
    {
        requestPermissions(fragment,
                permissions == null ? new String[] {} : permissions.toArray(new String[permissions.size()]),
                requestCode);
    }

    /**
     * {@link Fragment#onRequestPermissionsResult(int, String[], int[])} will get the result of the request.
     *
     * @param fragment    fragment
     * @param permissions One of {@link android.Manifest.permission}.*
     * @param requestCode requestCode
     */
    public static void requestPermissions(Fragment fragment, String[] permissions, int requestCode)
    {
        fragment.requestPermissions(permissions, requestCode);
    }
}
