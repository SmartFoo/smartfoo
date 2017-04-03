package com.smartfoo.android.audiofocusthief;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;

import com.smartfoo.android.core.media.FooAudioFocusListener.FooAudioFocusConfiguration;
import com.smartfoo.android.core.media.FooAudioFocusListener.FooAudioFocusListenerCallbacks;
import com.swooby.audiofocusthief.R;

public class MainActivity
        extends AppCompatActivity
{
    private final FooAudioFocusListenerCallbacks mAudioFocusListenerCallbacks = new FooAudioFocusListenerCallbacks()
    {

        @Override
        public void onAudioFocusGain(int audioFocusStreamType, int audioFocusDurationHint)
        {
            MainActivity.this.onAudioFocusGain(audioFocusStreamType, audioFocusDurationHint);
        }

        @Override
        public FooAudioFocusConfiguration onAudioFocusLoss(int audioFocusStreamType, int audioFocusDurationHint)
        {
            return MainActivity.this.onAudioFocusLoss(audioFocusStreamType, audioFocusDurationHint);
        }
    };

    private MainApplication mMainApplication;
    private Switch          mSwitchNotification;
    private EditText        mEditHashtag;
    private Switch          mSwitchAudioFocus;
    private Switch          mSwitchAudioFocusThief;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

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
        mEditHashtag = (EditText) findViewById(R.id.editHashtag);
        mSwitchAudioFocus = (Switch) findViewById(R.id.switchAudioFocus);
        mSwitchAudioFocus.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    String hashtag = mEditHashtag.getText().toString();
                    mMainApplication.audioFocusOn(hashtag);
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
                mMainApplication.setIsAudioFocusThief(isChecked);
                updateViews();
            }
        });
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

    private void onAudioFocusGain(int audioFocusStreamType, int audioFocusDurationHint)
    {
        mSwitchAudioFocus.setChecked(true);
    }

    private FooAudioFocusConfiguration onAudioFocusLoss(int audioFocusStreamType, int audioFocusDurationHint)
    {
        mSwitchAudioFocus.setChecked(false);
        return null;
    }
}
