package com.smartfoo.android.core.notification;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

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

        public int getPermissionsRequestCode()
        {
            return mPermissionsRequestCodeCallbacks.getPermissionsRequestCode();
        }

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

    public Context getContext()
    {
        return mCallbacks.getContext();
    }

    public int getPermissionsRequestCode()
    {
        return mCallbacks.getPermissionsRequestCode();
    }

    /**
     * @param permissionRequested One of {@link android.Manifest.permission}.*
     * @return never null
     */
    public List<String> checkPermission(String permissionRequested)
    {
        return checkPermissions(new String[] { permissionRequested });
    }

    /**
     * @param permissionsRequested One of {@link android.Manifest.permission}.*; ignored if null/empty
     * @return never null
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
                if (ContextCompat.checkSelfPermission(context, permissionRequested) == PackageManager.PERMISSION_DENIED)
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
