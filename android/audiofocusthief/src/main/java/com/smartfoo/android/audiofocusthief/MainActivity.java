package com.smartfoo.android.audiofocusthief;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import com.smartfoo.android.audiofocusthief.databinding.ActivityMainBinding;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioFocusController;
import com.smartfoo.android.core.media.FooAudioFocusController.FooAudioFocusControllerCallbacks;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.platform.FooPlatformUtils;

public class MainActivity
        extends AppCompatActivity
{
    private static final String TAG = FooLog.TAG(MainActivity.class);

    private static final int REQUEST_PERMISSION_POST_NOTIFICATIONS = 100;

    private final FooAudioFocusControllerCallbacks mAudioFocusListenerCallbacks = new FooAudioFocusControllerCallbacks()
    {
        @Override
        public void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
        {
            MainActivity.this.onAudioFocusGained(audioFocusStreamType, audioFocusDurationHint);
        }

        @Override
        public boolean onAudioFocusLost(int audioFocusStreamType, int audioFocusDurationHint, int focusChange)
        {
            return MainActivity.this.onAudioFocusLost(audioFocusStreamType, audioFocusDurationHint, focusChange);
        }
    };

    private final OnCheckedChangeListener mOnCheckedChangeListener = MainActivity.this::onCheckedChanged;

    private MainApplication mMainApplication;

    private SwitchCompat mSwitchNotification;
    private SwitchCompat mSwitchAudioFocus;
    private SwitchCompat mSwitchAudioFocusThief;

    private String getAudioFocusHashtag()
    {
        return mMainApplication.getAudioFocusHashtag();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        FooLog.e(TAG, "onCreate(savedInstanceState=" + FooPlatformUtils.toString(savedInstanceState) + ')');
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        FooLog.e(TAG, "onCreate: intent=" + FooPlatformUtils.toString(intent));

        mMainApplication = (MainApplication) getApplication();

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        mSwitchNotification = binding.appBarMain.activityMainContent.switchBackgroundService;
        mSwitchNotification.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mSwitchAudioFocus = binding.appBarMain.activityMainContent.switchAudioFocus;
        mSwitchAudioFocus.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mSwitchAudioFocusThief = binding.appBarMain.activityMainContent.switchAudioFocusThief;
        mSwitchAudioFocusThief.setOnCheckedChangeListener(mOnCheckedChangeListener);
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        FooLog.e(TAG, "onNewIntent(intent=" + FooPlatformUtils.toString(intent) + ')');
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume()
    {
        FooLog.e(TAG, "+onResume()");
        super.onResume();
        mMainApplication.attach(mAudioFocusListenerCallbacks);
        updateViews();
        FooLog.e(TAG, "-onResume()");
    }

    @Override
    protected void onPause()
    {
        FooLog.e(TAG, "+onPause()");
        super.onPause();
        mMainApplication.detach(mAudioFocusListenerCallbacks);
        FooLog.e(TAG, "-onPause()");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                backgroundServiceNotificationOn();
            } else {
                Toast.makeText(this, "Permission is required to enable notifications", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void backgroundServiceNotificationOn() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQUEST_PERMISSION_POST_NOTIFICATIONS);
            return;
        }
        mMainApplication.notificationOn();
    }

    private void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        int id = buttonView.getId();
        if (id == R.id.switchBackgroundService) {
            if (isChecked) {
                backgroundServiceNotificationOn();
            } else {
                mMainApplication.notificationOff();
            }
        } else if (id == R.id.switchAudioFocus) {
            if (isChecked) {
                mMainApplication.audioFocusOn();
            } else {
                mMainApplication.audioFocusOff();
            }
        } else if (id == R.id.switchAudioFocusThief) {
            mMainApplication.setIsAudioFocusThief(isChecked);
        }
        updateViews();
    }

    private void setChecked(@NonNull SwitchCompat viewSwitch, boolean checked)
    {
        viewSwitch.setOnCheckedChangeListener(null);
        viewSwitch.setChecked(checked);
        viewSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
    }

    private void updateViews()
    {
        FooLog.e(TAG, "+updateViews()");
        setChecked(mSwitchNotification, mMainApplication.isNotificationOn());
        setChecked(mSwitchAudioFocus, mMainApplication.isAudioFocusGained());
        setChecked(mSwitchAudioFocusThief, mMainApplication.getIsAudioFocusThief());
        FooLog.e(TAG, "-updateViews()");
    }

    private void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
    {
        FooLog.e(TAG, getAudioFocusHashtag() +
                      " onAudioFocusGained(audioFocusStreamType=" +
                      FooAudioUtils.audioStreamTypeToString(audioFocusStreamType) +
                      ", audioFocusDurationHint=" +
                      FooAudioUtils.audioFocusGainLossToString(audioFocusDurationHint) + ')');
        setChecked(mSwitchAudioFocus, true);
    }

    /** @noinspection unused*/
    private boolean onAudioFocusLost(int audioFocusStreamType, int audioFocusDurationHint, int focusChange)
    {
        FooLog.e(TAG, getAudioFocusHashtag() +
                      " onAudioFocusLost(audioFocusStreamType=" +
                      FooAudioUtils.audioStreamTypeToString(audioFocusStreamType) +
                      ", audioFocusDurationHint=" +
                      FooAudioUtils.audioFocusGainLossToString(audioFocusDurationHint) +
                      ", focusChange=" +
                      FooAudioUtils.audioFocusGainLossToString(focusChange) + ')');
        setChecked(mSwitchAudioFocus, false);
        mMainApplication.audioFocusOff();
        return false;
    }
}
