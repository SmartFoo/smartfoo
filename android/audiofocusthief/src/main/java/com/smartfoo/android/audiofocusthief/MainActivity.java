package com.smartfoo.android.audiofocusthief;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioFocusListener;
import com.smartfoo.android.core.media.FooAudioFocusListener.FooAudioFocusListenerCallbacks;
import com.smartfoo.android.core.media.FooAudioUtils;
import com.smartfoo.android.core.platform.FooPlatformUtils;

public class MainActivity
        extends AppCompatActivity
{
    private static final String TAG = FooLog.TAG(MainActivity.class);

    private final FooAudioFocusListenerCallbacks mAudioFocusListenerCallbacks = new FooAudioFocusListenerCallbacks()
    {
        @Override
        public void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
        {
            MainActivity.this.onAudioFocusGained(audioFocusStreamType, audioFocusDurationHint);
        }

        @Override
        public boolean onAudioFocusLost(FooAudioFocusListener audioFocusListener, int audioFocusStreamType, int audioFocusDurationHint, int focusChange)
        {
            return MainActivity.this.onAudioFocusLost(audioFocusListener, audioFocusStreamType, audioFocusDurationHint, focusChange);
        }
    };

    private final OnCheckedChangeListener mOnCheckedChangeListener = new OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
            MainActivity.this.onCheckedChanged(buttonView, isChecked);
        }
    };

    private MainApplication mMainApplication;
    private Switch          mSwitchNotification;
    private Switch          mSwitchAudioFocus;
    private Switch          mSwitchAudioFocusThief;

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

        setContentView(R.layout.activity_main);

        mSwitchNotification = (Switch) findViewById(R.id.switchBackgroundSevice);
        mSwitchNotification.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mSwitchAudioFocus = (Switch) findViewById(R.id.switchAudioFocus);
        mSwitchAudioFocus.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mSwitchAudioFocusThief = (Switch) findViewById(R.id.switchAudioFocusThief);
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

    private void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        switch (buttonView.getId())
        {
            case R.id.switchBackgroundSevice:
                if (isChecked)
                {
                    mMainApplication.notificationOn();
                }
                else
                {
                    mMainApplication.notificationOff();
                }
                break;
            case R.id.switchAudioFocus:
                if (isChecked)
                {
                    mMainApplication.audioFocusOn();
                }
                else
                {
                    mMainApplication.audioFocusOff();
                }
                break;
            case R.id.switchAudioFocusThief:
                mMainApplication.setIsAudioFocusThief(isChecked);
                break;
        }
        updateViews();
    }

    private void setChecked(@NonNull Switch viewSwitch, boolean checked)
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
                      FooAudioUtils.audioFocusToString(audioFocusDurationHint) + ')');
        setChecked(mSwitchAudioFocus, true);
    }

    private boolean onAudioFocusLost(FooAudioFocusListener audioFocusListener, int audioFocusStreamType, int audioFocusDurationHint, int focusChange)
    {
        FooLog.e(TAG, getAudioFocusHashtag() +
                      " onAudioFocusLost(audioFocusStreamType=" +
                      FooAudioUtils.audioStreamTypeToString(audioFocusStreamType) +
                      ", audioFocusDurationHint=" +
                      FooAudioUtils.audioFocusToString(audioFocusDurationHint) +
                      ", focusChange=" +
                      FooAudioUtils.audioFocusToString(focusChange) + ')');
        setChecked(mSwitchAudioFocus, false);
        mMainApplication.audioFocusOff();
        return false;
    }
}
