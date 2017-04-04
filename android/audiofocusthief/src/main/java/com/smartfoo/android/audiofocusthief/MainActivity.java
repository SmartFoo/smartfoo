package com.smartfoo.android.audiofocusthief;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioFocusListener.FooAudioFocusConfiguration;
import com.smartfoo.android.core.media.FooAudioFocusListener.FooAudioFocusListenerCallbacks;
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
        public FooAudioFocusConfiguration onAudioFocusLost(int audioFocusStreamType, int audioFocusDurationHint)
        {
            return MainActivity.this.onAudioFocusLost(audioFocusStreamType, audioFocusDurationHint);
        }
    };

    private MainApplication mMainApplication;
    private Switch          mSwitchNotification;
    private Switch          mSwitchAudioFocus;
    private Switch          mSwitchAudioFocusThief;

    private String getAudioFocusHashtag()
    {
        return "#AUDIOFOCUS_" + (mSwitchAudioFocusThief.isChecked() ? "THIEF" : "NICE");
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
        mSwitchNotification.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    mMainApplication.notificationOn();
                }
                else
                {
                    mMainApplication.notificationOff();
                }
                updateViews();
            }
        });
        mSwitchAudioFocus = (Switch) findViewById(R.id.switchAudioFocus);
        mSwitchAudioFocus.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    mMainApplication.audioFocusOn(getAudioFocusHashtag());
                }
                else
                {
                    mMainApplication.audioFocusOff();
                }
                updateViews();
            }
        });
        mSwitchAudioFocusThief = (Switch) findViewById(R.id.switchAudioFocusThief);
        mSwitchAudioFocusThief.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                mMainApplication.setIsAudioFocusThief(isChecked, getAudioFocusHashtag());
                updateViews();
            }
        });
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
        super.onResume();
        mMainApplication.attach(mAudioFocusListenerCallbacks);
        updateViews();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mMainApplication.detach(mAudioFocusListenerCallbacks);
    }

    private void updateViews()
    {
        mSwitchNotification.setChecked(mMainApplication.isNotificationOn());
        mSwitchAudioFocus.setChecked(mMainApplication.isAudioFocusOn());
        mSwitchAudioFocusThief.setChecked(mMainApplication.getIsAudioFocusThief());
    }

    private void onAudioFocusGained(int audioFocusStreamType, int audioFocusDurationHint)
    {
        FooLog.e(TAG, getAudioFocusHashtag() +
                      " onAudioFocusGained(" + audioFocusStreamType + ", " + audioFocusDurationHint + ')');
        mSwitchAudioFocus.setChecked(true);
    }

    private FooAudioFocusConfiguration onAudioFocusLost(int audioFocusStreamType, int audioFocusDurationHint)
    {
        FooLog.e(TAG, getAudioFocusHashtag() +
                      " onAudioFocusGained(" + audioFocusStreamType + ", " + audioFocusDurationHint + ')');
        mSwitchAudioFocus.setChecked(false);
        return null;
    }
}
