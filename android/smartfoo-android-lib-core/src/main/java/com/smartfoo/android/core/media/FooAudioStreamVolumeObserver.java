package com.smartfoo.android.core.media;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.FooListenerManager;
import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.media.FooAudioStreamVolumeObserver.SystemSettingsContentObserver.OnSystemSettingsChangedCallbacks;
import com.smartfoo.android.core.platform.FooHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Observes changes to audio stream volumes and notifies registered callbacks.
 *
 * <p>Registers a {@link android.database.ContentObserver} on
 * {@link android.provider.Settings.System#CONTENT_URI} to detect volume changes for one or more
 * audio stream types. Multiple listeners may be attached for the same or different stream types;
 * the content observer is registered lazily on the first {@link #attach} call and unregistered
 * after the last {@link #detach} call.</p>
 */
public class FooAudioStreamVolumeObserver
{
    private static final String TAG = FooLog.TAG(FooAudioStreamVolumeObserver.class);

    /**
     * Callback interface for audio stream volume change events.
     */
    public interface OnAudioStreamVolumeChangedCallbacks
    {
        /**
         * Called when the volume of a monitored audio stream changes.
         *
         * @param audioStreamType the stream type (one of the {@link android.media.AudioManager}
         *                        {@code STREAM_*} constants)
         * @param volume          the new absolute volume level
         * @param volumeMax       the maximum possible volume for this stream
         * @param volumePercent   the new volume as a percentage of the maximum (0–100)
         */
        void onAudioStreamVolumeChanged(int audioStreamType, int volume, int volumeMax, int volumePercent);
    }

    static class SystemSettingsContentObserver
    {
        interface OnSystemSettingsChangedCallbacks
        {
            void onSystemSettingsChanged(boolean selfChange);
        }

        private final ContentResolver mContentResolver;
        private final FooHandler      mHandler;

        private ContentObserver mContentObserver;

        private SystemSettingsContentObserver(@NonNull Context context)
        {
            mContentResolver = context.getContentResolver();
            mHandler = new FooHandler();
        }

        private void start(@NonNull final OnSystemSettingsChangedCallbacks callbacks)
        {
            if (mContentObserver != null)
            {
                return;
            }

            mContentObserver = new ContentObserver(mHandler)
            {
                /**
                 * Invoked by the system when any row under
                 * {@link android.provider.Settings.System#CONTENT_URI} changes.
                 * Delegates to {@link OnSystemSettingsChangedCallbacks#onSystemSettingsChanged}.
                 *
                 * @param selfChange {@code true} if the change was triggered by this observer
                 * @param uri        the URI of the changed content
                 */
                @Override
                public void onChange(boolean selfChange, Uri uri)
                {
                    callbacks.onSystemSettingsChanged(selfChange);
                }
            };

            mContentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, mContentObserver);
        }

        private void stop()
        {
            if (mContentObserver == null)
            {
                return;
            }

            mContentResolver.unregisterContentObserver(mContentObserver);
        }
    }

    private final Context                                                               mContext;
    private final AudioManager                                                          mAudioManager;
    private final Map<Integer, FooListenerManager<OnAudioStreamVolumeChangedCallbacks>> mAudioStreamTypeToListenerManagers;
    private final Map<Integer, Integer>                                                 mAudioStreamTypeToLastVolume;

    private SystemSettingsContentObserver mSystemSettingsContentObserver;

    public FooAudioStreamVolumeObserver(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioStreamTypeToListenerManagers = new LinkedHashMap<>();
        mAudioStreamTypeToLastVolume = new LinkedHashMap<>();
    }

    /**
     * Attaches a callback for the given audio stream type. If this is the first callback across
     * all stream types, the system content observer is registered automatically.
     *
     * @param audioStreamType the audio stream to monitor (e.g. {@link android.media.AudioManager#STREAM_MUSIC})
     * @param callbacks       the callbacks to notify; must not be null
     */
    public void attach(int audioStreamType, @NonNull OnAudioStreamVolumeChangedCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");

        FooListenerManager<OnAudioStreamVolumeChangedCallbacks> listenerManager = mAudioStreamTypeToListenerManagers
                .get(audioStreamType);
        if (listenerManager == null)
        {
            listenerManager = new FooListenerManager<>(this);
            mAudioStreamTypeToListenerManagers.put(audioStreamType, listenerManager);
        }

        int volume = mAudioManager.getStreamVolume(audioStreamType);
        mAudioStreamTypeToLastVolume.put(audioStreamType, volume);

        listenerManager.attach(callbacks);

        if (mAudioStreamTypeToListenerManagers.size() == 1)
        {
            if (mSystemSettingsContentObserver == null)
            {
                mSystemSettingsContentObserver = new SystemSettingsContentObserver(mContext);
                mSystemSettingsContentObserver.start(new OnSystemSettingsChangedCallbacks()
                {
                    @Override
                    public void onSystemSettingsChanged(boolean selfChange)
                    {
                        //FooLog.v(TAG, "onSystemSettingsChanged(selfChange=" + selfChange + ')');
                        for (Entry<Integer, FooListenerManager<OnAudioStreamVolumeChangedCallbacks>> entry :
                                mAudioStreamTypeToListenerManagers.entrySet())
                        {
                            int audioStreamType = entry.getKey();
                            //FooLog.v(TAG, "onSystemSettingsChanged: audioStreamType == " + audioStreamType);
                            int volume = mAudioManager.getStreamVolume(audioStreamType);
                            //FooLog.v(TAG, "onSystemSettingsChanged: volume == " + volume);
                            Integer lastVolume = mAudioStreamTypeToLastVolume.get(audioStreamType);
                            //FooLog.v(TAG, "onSystemSettingsChanged: lastVolume == " + lastVolume);
                            if (volume == lastVolume)
                            {
                                continue;
                            }

                            mAudioStreamTypeToLastVolume.put(audioStreamType, volume);

                            int volumeMax = mAudioManager.getStreamMaxVolume(audioStreamType);
                            int volumePercent = Math.round(volume / (float) volumeMax * 100f);

                            FooListenerManager<OnAudioStreamVolumeChangedCallbacks> listenerManager = entry.getValue();
                            for (OnAudioStreamVolumeChangedCallbacks callbacks : listenerManager.beginTraversing())
                            {
                                callbacks.onAudioStreamVolumeChanged(audioStreamType, volume, volumeMax, volumePercent);
                            }
                            listenerManager.endTraversing();
                        }
                    }
                });
            }
        }
    }

    /**
     * Detaches a previously attached callback. If this was the last callback across all stream
     * types, the system content observer is unregistered automatically.
     *
     * @param audioStreamType the audio stream type that was passed to {@link #attach}
     * @param callbacks       the callbacks to remove; must not be null
     */
    public void detach(int audioStreamType, @NonNull OnAudioStreamVolumeChangedCallbacks callbacks)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(callbacks, "callbacks");

        FooListenerManager<OnAudioStreamVolumeChangedCallbacks> listenerManager = mAudioStreamTypeToListenerManagers
                .get(audioStreamType);
        if (listenerManager == null)
        {
            return;
        }

        listenerManager.detach(callbacks);

        if (listenerManager.size() == 0)
        {
            mAudioStreamTypeToListenerManagers.remove(audioStreamType);
        }

        if (mAudioStreamTypeToListenerManagers.size() == 0)
        {
            if (mSystemSettingsContentObserver != null)
            {
                mSystemSettingsContentObserver.stop();
                mSystemSettingsContentObserver = null;
            }
        }
    }
}
