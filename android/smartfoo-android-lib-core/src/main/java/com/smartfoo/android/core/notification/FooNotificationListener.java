package com.smartfoo.android.core.notification;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.media.RemoteController;
import android.media.RemoteController.MetadataEditor;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.R;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@TargetApi(19)
public class FooNotificationListener
        extends NotificationListenerService
        implements RemoteController.OnClientUpdateListener
{
    private static final String TAG = FooLog.TAG(FooNotificationListener.class);

    private static final int VERSION_SDK_INT = VERSION.SDK_INT;

    public static boolean supportsNotificationListenerSettings()
    {
        return VERSION_SDK_INT >= 19;
    }

    @SuppressLint("InlinedApi")
    @TargetApi(19)
    @NonNull
    public static Intent getIntentNotificationListenerSettings()
    {
        final String ACTION_NOTIFICATION_LISTENER_SETTINGS;
        if (VERSION_SDK_INT >= 22)
        {
            ACTION_NOTIFICATION_LISTENER_SETTINGS = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
        }
        else
        {
            ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
        }

        return new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
    }

    public static final String ACTION_BIND_REMOTE_CONTROLLER = "com.smartfoo.android.core.notification.FooNotificationListener.ACTION_BIND_REMOTE_CONTROLLER";

    public class RemoteControllerBinder
            extends Binder
    {
        public FooNotificationListener getService()
        {
            return FooNotificationListener.this;
        }
    }

    private IBinder mRemoteControllerBinder = new RemoteControllerBinder();

    private RemoteController mRemoteController;
    private FooTextToSpeech  mTextToSpeech;

    private final Map<String, NotificationParser> mNotificationParsers;

    public FooNotificationListener()
    {
        mNotificationParsers = new HashMap<>();
    }

    private void addNotificationParser(NotificationParser notificationParser)
    {
        mNotificationParsers.put(notificationParser.getPackageName(), notificationParser);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        FooLog.d(TAG, "onBind(intent=" + FooPlatformUtils.toString(intent) + ')');
        if (ACTION_BIND_REMOTE_CONTROLLER.equals(intent.getAction()))
        {
            return mRemoteControllerBinder;
        }
        else
        {
            return super.onBind(intent);
        }
    }

    @Override
    public void onCreate()
    {
        FooLog.d(TAG, "onCreate()");
        super.onCreate();

        Context applicationContext = getApplicationContext();

        mRemoteController = new RemoteController(applicationContext, this);
        mTextToSpeech = FooTextToSpeech.getInstance().start(applicationContext);

        addNotificationParser(new PandoraNotificationParser(applicationContext, mTextToSpeech));
        addNotificationParser(new SpotifyNotificationParser(applicationContext, mTextToSpeech));
        addNotificationParser(new GoogleHangoutsNotificationParser(applicationContext, mTextToSpeech));
        addNotificationParser(new GmailNotificationParser(applicationContext, mTextToSpeech));
        addNotificationParser(new GoogleMessengerNotificationParser(applicationContext, mTextToSpeech));
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    public RemoteController getRemoteController()
    {
        return mRemoteController;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        String packageName = NotificationParser.getPackageName(sbn);
        FooLog.d(TAG, "onNotificationPosted: packageName=" + FooString.quote(packageName));

        boolean handled;

        NotificationParser notificationParser = mNotificationParsers.get(packageName);
        if (notificationParser != null)
        {
            handled = notificationParser.onNotificationPosted(sbn);
        }
        else
        {
            handled = NotificationParser.defaultOnNotificationPosted(mTextToSpeech, sbn, null);
        }

        if (handled)
        {
            mTextToSpeech.silence(500);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        String packageName = sbn.getPackageName();
        FooLog.d(TAG, "onNotificationRemoved: packageName=" + FooString.quote(packageName));

        NotificationParser notificationParser = mNotificationParsers.get(packageName);
        if (notificationParser != null)
        {
            // TODO:(pv) Reset any cache in the parser
            notificationParser.onNotificationRemoved(sbn);
        }
    }

    @Override
    public void onClientChange(boolean clearing)
    {
    }

    @Override
    public void onClientPlaybackStateUpdate(int state)
    {
    }

    @Override
    public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed)
    {
    }

    @Override
    public void onClientTransportControlUpdate(int transportControlFlags)
    {
    }

    @Override
    public void onClientMetadataUpdate(MetadataEditor metadataEditor)
    {
    }

    public static String toVerboseString(int value)
    {
        return Integer.toString(value) + " (0x" + Integer.toHexString(value) + ')';
    }

    // TODO:(pv) Make a UI that shows all StatusBarNotification fields, especially:
    //  Notification.tickerText
    //  All ImageView Resource Ids and TextView Texts in BigContentView
    //  All ImageView Resource Ids and TextView Texts in ContentView
    //  The user could then select what to say for what images, and prefixing for suffixing texts

    public static abstract class NotificationParser
    {
        @NonNull
        public static String getPackageName(
                @NonNull
                StatusBarNotification sbn)
        {
            return sbn.getPackageName();
        }

        @NonNull
        public static Notification getNotification(
                @NonNull
                StatusBarNotification sbn)
        {
            return sbn.getNotification();
        }

        public static RemoteViews getBigContentView(
                @NonNull
                StatusBarNotification sbn)
        {
            return getNotification(sbn).bigContentView;
        }

        public static RemoteViews getContentView(
                @NonNull
                StatusBarNotification sbn)
        {
            return getNotification(sbn).contentView;
        }

        @Nullable
        public static Context createPackageContext(
                @NonNull
                Context context,
                @NonNull
                RemoteViews remoteView)
        {
            String packageName = remoteView.getPackage();

            try
            {
                return context.createPackageContext(packageName, Context.CONTEXT_RESTRICTED);
            }
            catch (NameNotFoundException e)
            {
                return null;
            }
        }

        @Nullable
        public static View mockRemoteView(
                @NonNull
                Context context,
                @NonNull
                RemoteViews remoteView)
        {
            Context otherAppContext = createPackageContext(context, remoteView);
            if (otherAppContext == null)
            {
                return null;
            }

            LayoutInflater layoutInflater = LayoutInflater.from(otherAppContext);

            return layoutInflater.inflate(remoteView.getLayoutId(), null, true);
        }

        public static int getIdOfChildWithName(
                @NonNull
                View parent,
                @NonNull
                String childName)
        {
            //PbLog.e(TAG,
            //        "getIdOfChildWithName(parent=" + parent + ", childName=" + PbStringUtils.quote(childName) + ')');

            Resources resources = parent.getResources();
            String packageName = parent.getContext().getPackageName();

            return resources.getIdentifier(childName, "id", packageName);
        }

        /*
        @Nullable
        public static View findViewByName(
                @NonNull
                View parent,
                @NonNull
                String childName)
        {
            FooLog.v(TAG, "findViewByName(parent=" + parent + ", childName=" + FooString.quote(childName) + ')');

            int id = getIdOfChildWithName(parent, childName);
            if (id == 0)
            {
                return null;
            }

            return parent.findViewById(id);
        }
        */

        public interface TagTypes
        {
            /*
            /* *
             * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L733
             * /
            int PendingIntent    = 1;
            */
            /**
             * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1057
             */
            int ReflectionAction = 2;
        }

        /**
         * From:
         * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1074
         */
        public interface ActionTypes
        {
            int BOOLEAN          = 1;
            int BYTE             = 2;
            int SHORT            = 3;
            int INT              = 4;
            int LONG             = 5;
            int FLOAT            = 6;
            int DOUBLE           = 7;
            int CHAR             = 8;
            int STRING           = 9;
            int CHAR_SEQUENCE    = 10;
            int URI              = 11;
            int BITMAP           = 12;
            int BUNDLE           = 13;
            int INTENT           = 14;
            int COLOR_STATE_LIST = 15;
            int ICON             = 16;
        }

        public interface ValueTypes
        {
            int TEXT              = 1;
            int VISIBILITY        = 2;
            int ENABLED           = 3;
            int IMAGE_RESOURCE_ID = 4;
            /*
            int PENDING_INTENT    = 5;
            int ICON              = 6;
            int BITMAP            = 7;
            */
        }

        @Nullable
        public static Object getRemoteViewValueById(
                @NonNull
                RemoteViews remoteViews, int id, int valueType)
        {
            //noinspection TryWithIdenticalCatches
            try
            {
                Field field = remoteViews.getClass().getDeclaredField("mActions");
                field.setAccessible(true);

                @SuppressWarnings("unchecked")
                ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(remoteViews);

                for (int i = 0; i < actions.size(); i++)
                {
                    Parcelable parcelable = actions.get(i);

                    Parcel parcel = Parcel.obtain();

                    try
                    {
                        parcelable.writeToParcel(parcel, 0);

                        parcel.setDataPosition(0);

                        int tag = parcel.readInt();
                        FooLog.v(TAG, "getRemoteViewValueById: tag=" + toVerboseString(tag));
                        switch (valueType)
                        {
                            /*
                            case ValueTypes.PENDING_INTENT:
                                if (tag != TagTypes.PendingIntent)
                                {
                                    continue;
                                }
                                break;
                            */
                            case ValueTypes.TEXT:
                            case ValueTypes.VISIBILITY:
                            case ValueTypes.ENABLED:
                            case ValueTypes.IMAGE_RESOURCE_ID:
                                if (tag != TagTypes.ReflectionAction)
                                {
                                    continue;
                                }
                                break;
                            default:
                                continue;
                        }

                        int viewId = parcel.readInt();
                        FooLog.v(TAG, "getRemoteViewValueById: viewId=" + toVerboseString(viewId));
                        if (viewId != id)
                        {
                            continue;
                        }

                        Object value = null;

                        switch (tag)
                        {
                            /*
                            case TagTypes.PendingIntent:
                            {
                                if (parcel.readInt() != 0)
                                {
                                    value = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
                                }
                                break;
                            }
                            */
                            case TagTypes.ReflectionAction:
                            {
                                String methodName = parcel.readString();
                                switch (valueType)
                                {
                                    case ValueTypes.TEXT:
                                        if (!"setText".equals(methodName))
                                        {
                                            continue;
                                        }
                                        break;
                                    case ValueTypes.VISIBILITY:
                                        if (!"setVisibility".equals(methodName))
                                        {
                                            continue;
                                        }
                                        break;
                                    case ValueTypes.IMAGE_RESOURCE_ID:
                                        if (!"setImageResource".equals(methodName))
                                        {
                                            continue;
                                        }
                                        break;
                                    case ValueTypes.ENABLED:
                                        if (!"setEnabled".equals(methodName))
                                        {
                                            continue;
                                        }
                                        break;
                                    default:
                                        continue;
                                }

                                int actionType = parcel.readInt();
                                // per:
                                // https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1101
                                switch (actionType)
                                {
                                    case ActionTypes.BOOLEAN:
                                        value = parcel.readInt() != 0;
                                        break;
                                    case ActionTypes.INT:
                                        value = parcel.readInt();
                                        break;
                                    case ActionTypes.CHAR_SEQUENCE:
                                        value = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel)
                                                .toString()
                                                .trim();
                                        break;
                                    /*
                                    case ActionTypes.INTENT:
                                        if (parcel.readInt() != 0)
                                        {
                                            value = Intent.CREATOR.createFromParcel(parcel);
                                        }
                                        break;
                                    case ActionTypes.ICON:
                                        if (parcel.readInt() != 0)
                                        {
                                            value = null;//Icon.CREATOR.createFromParcel(parcel);
                                        }
                                    */
                                }
                                break;
                            }
                            default:
                                continue;
                        }

                        FooLog.v(TAG, "getRemoteViewValueById: parcel.dataAvail()=" + parcel.dataAvail());

                        return value;
                    }
                    finally
                    {
                        parcel.recycle();
                    }
                }
            }
            catch (IllegalAccessException e)
            {
                FooLog.e(TAG, "getRemoteViewValueById", e);
            }
            catch (NoSuchFieldException e)
            {
                FooLog.e(TAG, "getRemoteViewValueById", e);
            }

            return null;
        }

        public static void walkView(View view)
        {
            FooLog.v(TAG, "walkView: view=" + view);

            if (view instanceof ViewGroup)
            {
                ViewGroup viewGroup = (ViewGroup) view;
                int childCount = viewGroup.getChildCount();
                for (int i = 0; i < childCount; i++)
                {
                    View childView = viewGroup.getChildAt(i);
                    walkView(childView);
                }
            }
        }

        @NonNull
        public static String unknownIfNullOrEmpty(String value)
        {
            if (FooString.isNullOrEmpty(value))
            {
                value = "Unknown";
            }
            return value;
        }

        public static boolean defaultOnNotificationPosted(
                @NonNull
                FooTextToSpeech textToSpeech,
                @NonNull
                StatusBarNotification sbn,
                String packageAppSpokenName)
        {
            Notification notification = getNotification(sbn);
            CharSequence tickerText = notification.tickerText;
            if (!FooString.isNullOrEmpty(tickerText))
            {
                String title = FooString.isNullOrEmpty(packageAppSpokenName) ? getPackageName(sbn) : packageAppSpokenName;
                textToSpeech.speak(title);
                textToSpeech.silence(500);
                textToSpeech.speak(tickerText.toString());

                return true;
            }

            return false;
        }

        protected final Context         mApplicationContext;
        protected final Resources       mResources;
        protected final FooTextToSpeech mTextToSpeech;
        protected final String          mPackageName;
        protected final String          mPackageAppSpokenName;

        protected NotificationParser(
                @NonNull
                Context applicationContext,
                @NonNull
                FooTextToSpeech textToSpeech,
                @NonNull
                String packageName,
                @NonNull
                String packageAppSpokenName)
        {
            mApplicationContext = applicationContext;
            mResources = applicationContext.getResources();
            mTextToSpeech = textToSpeech;
            mPackageName = packageName;
            mPackageAppSpokenName = packageAppSpokenName;
        }

        public String getPackageName()
        {
            return mPackageName;
        }

        public boolean onNotificationPosted(StatusBarNotification sbn)
        {
            //String groupKey = sbn.getGroupKey();
            //String key = sbn.getKey();
            //UserHandle user = sbn.getUser();
            //String packageName = sbn.getPackageName();
            //long postTime = sbn.getPostTime();

            Notification notification = sbn.getNotification();
            FooLog.v(TAG, "onNotificationPosted: notification=" + notification);

            //int id = sbn.getId();
            //String tag = sbn.getTag();

            //CharSequence tickerText = notification.tickerText;

            // TODO:(pv) Seriously, introspect and walk all StatusBarNotification fields, especially:
            //  Notification.tickerText
            //  All ImageView Resource Ids and TextView Texts in BigContentView
            //  All ImageView Resource Ids and TextView Texts in ContentView

            RemoteViews bigContentView = notification.bigContentView;
            View mockBigContentView = mockRemoteView(mApplicationContext, bigContentView);
            FooLog.v(TAG, "onNotificationPosted: bigContentView");
            walkView(mockBigContentView);
            FooLog.v(TAG, "onNotificationPosted: --------");
            RemoteViews contentView = notification.contentView;
            View mockContentView = mockRemoteView(mApplicationContext, contentView);
            FooLog.v(TAG, "onNotificationPosted: contentView");
            walkView(mockContentView);

            //RemoteViews headUpContentView = notification.headsUpContentView;

            //Notification.Action[] actions = notification.actions;

            //String category = notification.category;

            Bundle extras = notification.extras;
            FooLog.v(TAG, "onNotificationPosted: extras=" + FooPlatformUtils.toString(extras));

            return defaultOnNotificationPosted(mTextToSpeech, sbn, mPackageAppSpokenName);
        }

        public void onNotificationRemoved(StatusBarNotification sbn)
        {
        }
    }

    public static class PandoraNotificationParser
            extends NotificationParser
    {
        private String mAdvertisementTitle;
        private String mAdvertisementArtist;

        private boolean mLastIsPlaying;
        private String  mLastArtist;
        private String  mLastTitle;
        private String  mLastStation;

        public PandoraNotificationParser(Context applicationContext, FooTextToSpeech textToSpeech)
        {
            super(applicationContext, textToSpeech, "com.pandora.android", applicationContext.getString(R.string.pandora_package_app_name));

            mAdvertisementTitle = applicationContext.getString(R.string.pandora_advertisement_title);
            mAdvertisementArtist = applicationContext.getString(R.string.pandora_advertisement_artist);
        }

        @Override
        public boolean onNotificationPosted(StatusBarNotification sbn)
        {
            //super.onNotificationPosted(sbn);

            RemoteViews bigContentView = getBigContentView(sbn);

            //
            // NOTE: We intentionally recompute this every time;
            // The app can update in the background which can cause the resource ids to change.
            //
            View mockRemoteView = mockRemoteView(mApplicationContext, bigContentView);
            if (mockRemoteView == null)
            {
                return false;
            }

            int idTitle = getIdOfChildWithName(mockRemoteView, "title");
            FooLog.v(TAG, "onNotificationPosted: idTitle=" + toVerboseString(idTitle));
            if (idTitle == 0)
            {
                return false;
            }

            int idArtist = getIdOfChildWithName(mockRemoteView, "artist");
            FooLog.v(TAG, "onNotificationPosted: idArtist=" + toVerboseString(idArtist));
            if (idArtist == 0)
            {
                return false;
            }

            int idStation = getIdOfChildWithName(mockRemoteView, "station");
            FooLog.v(TAG, "onNotificationPosted: idStation=" + toVerboseString(idStation));
            if (idStation == 0)
            {
                return false;
            }

            int idPlay = getIdOfChildWithName(mockRemoteView, "play");
            FooLog.v(TAG, "onNotificationPosted: idPlay=" + toVerboseString(idPlay));
            if (idPlay == 0)
            {
                return false;
            }

            Context otherAppContext = createPackageContext(mApplicationContext, bigContentView);
            if (otherAppContext == null)
            {
                return false;
            }

            Resources resources = otherAppContext.getResources();
            String packageName = otherAppContext.getPackageName();
            int idPauseDrawable = resources.getIdentifier("notification_pause_selector", "drawable", packageName);
            FooLog.v(TAG, "onNotificationPosted: idPauseDrawable=" + toVerboseString(idPauseDrawable));

            int idPlayResourceId = (int) getRemoteViewValueById(bigContentView, idPlay, ValueTypes.IMAGE_RESOURCE_ID);
            FooLog.v(TAG, "onNotificationPosted: idPlayResourceId=" + toVerboseString(idPlayResourceId));
            if (idPlayResourceId == 0)
            {
                return false;
            }
            boolean isPlaying = idPlayResourceId == idPauseDrawable;
            FooLog.v(TAG, "onNotificationPosted: isPlaying=" + isPlaying);

            String textTitle = (String) getRemoteViewValueById(bigContentView, idTitle, ValueTypes.TEXT);
            textTitle = unknownIfNullOrEmpty(textTitle);
            FooLog.v(TAG, "onNotificationPosted: textTitle=" + FooString.quote(textTitle));

            String textArtist = (String) getRemoteViewValueById(bigContentView, idArtist, ValueTypes.TEXT);
            textArtist = unknownIfNullOrEmpty(textArtist);
            FooLog.v(TAG, "onNotificationPosted: textArtist=" + FooString.quote(textArtist));

            String textStation = (String) getRemoteViewValueById(bigContentView, idStation, ValueTypes.TEXT);
            textStation = unknownIfNullOrEmpty(textStation);
            FooLog.v(TAG, "onNotificationPosted: textStation=" + FooString.quote(textStation));

            if (mAdvertisementTitle.equalsIgnoreCase(textTitle) &&
                mAdvertisementArtist.equalsIgnoreCase(textArtist))
            {
                // It's a commercial!
                // TODO:(pv) Option to mute the volume and unmute when commercial ends...
                return false;
            }

            if (isPlaying != mLastIsPlaying ||
                !textTitle.equals(mLastTitle) ||
                !textArtist.equals(mLastArtist) ||
                !textStation.equals(mLastStation))
            {
                mLastIsPlaying = isPlaying;
                mLastTitle = textTitle;
                mLastArtist = textArtist;
                mLastStation = textStation;

                if (isPlaying)
                {
                    mTextToSpeech.speak(mPackageAppSpokenName + " playing");
                    mTextToSpeech.silence(500);
                    mTextToSpeech.speak("artist " + textArtist);
                    mTextToSpeech.silence(500);
                    mTextToSpeech.speak("title " + textTitle);
                    //mTextToSpeech.silence(500);
                    //mTextToSpeech.speak("station " + textStation);
                }
                else
                {
                    mTextToSpeech.speak(mPackageAppSpokenName + " paused");
                }

                return true;
            }

            return false;
        }
    }

    public static class SpotifyNotificationParser
            extends NotificationParser
    {
        private boolean mLastIsPlaying;
        private String  mLastArtist;
        private String  mLastTitle;
        private String  mLastAlbum;

        public SpotifyNotificationParser(Context applicationContext, FooTextToSpeech textToSpeech)
        {
            super(applicationContext, textToSpeech, "com.spotify.music", applicationContext.getString(R.string.spotify_package_app_name));
        }

        @Override
        public boolean onNotificationPosted(StatusBarNotification sbn)
        {
            //super.onNotificationPosted(sbn);

            RemoteViews contentView = getContentView(sbn);

            View mockRemoteView = mockRemoteView(mApplicationContext, contentView);
            if (mockRemoteView == null)
            {
                return false;
            }

            int idTitle = getIdOfChildWithName(mockRemoteView, "title");
            if (idTitle == 0)
            {
                return false;
            }

            int idSubtitle = getIdOfChildWithName(mockRemoteView, "subtitle");
            if (idSubtitle == 0)
            {
                return false;
            }

            int idPause = getIdOfChildWithName(mockRemoteView, "pause");
            if (idPause == 0)
            {
                return false;
            }

            int pauseVisibility = (int) getRemoteViewValueById(contentView, idPause, ValueTypes.VISIBILITY);
            FooLog.v(TAG, "onNotificationPosted: pauseVisibility=" + pauseVisibility);
            boolean isPlaying = pauseVisibility == View.VISIBLE;
            FooLog.v(TAG, "onNotificationPosted: isPlaying=" + isPlaying);

            String textTitle = (String) getRemoteViewValueById(contentView, idTitle, ValueTypes.TEXT);
            textTitle = unknownIfNullOrEmpty(textTitle);
            FooLog.v(TAG, "onNotificationPosted: textTitle=" + FooString.quote(textTitle));

            String textSubtitle = (String) getRemoteViewValueById(contentView, idSubtitle, ValueTypes.TEXT);
            FooLog.v(TAG, "onNotificationPosted: textSubtitle=" + FooString.quote(textSubtitle));
            String[] textArtistAndAlbum = (textSubtitle != null) ? textSubtitle.split("—") : null;
            String textArtist = null;
            String textAlbum = null;
            if (textArtistAndAlbum != null)
            {
                int offset = 0;
                if (offset < textArtistAndAlbum.length)
                {
                    textArtist = textArtistAndAlbum[offset++];
                }
                if (offset < textArtistAndAlbum.length)
                {
                    //noinspection UnusedAssignment
                    textAlbum = textArtistAndAlbum[offset++];
                }
            }

            FooLog.v(TAG, "onNotificationPosted: textArtist=" + FooString.quote(textArtist));
            FooLog.v(TAG, "onNotificationPosted: textAlbum=" + FooString.quote(textAlbum));

            if (textArtist == null && textAlbum == null)
            {
                // It's a commercial!
                // TODO:(pv) Option to mute the volume and unmute when commercial ends...
                return false;
            }

            textArtist = unknownIfNullOrEmpty(textArtist);
            textAlbum = unknownIfNullOrEmpty(textAlbum);

            if (isPlaying != mLastIsPlaying ||
                !textArtist.equals(mLastArtist) ||
                !textTitle.equals(mLastTitle) ||
                !textAlbum.equals(mLastAlbum))
            {
                mLastIsPlaying = isPlaying;
                mLastArtist = textArtist;
                mLastTitle = textTitle;
                mLastAlbum = textAlbum;

                if (isPlaying)
                {
                    mTextToSpeech.speak(mPackageAppSpokenName + " playing");
                    mTextToSpeech.silence(500);
                    mTextToSpeech.speak("artist " + textArtist);
                    mTextToSpeech.silence(500);
                    mTextToSpeech.speak("title " + textTitle);
                    mTextToSpeech.silence(500);
                    mTextToSpeech.speak("album " + textAlbum);
                }
                else
                {
                    mTextToSpeech.speak(mPackageAppSpokenName + " paused");
                }

                return true;
            }

            return false;
        }
    }

    public static class GoogleHangoutsNotificationParser
            extends NotificationParser
    {
        private static class TextMessage
        {
            final String mFrom;
            final String mMessage;

            public TextMessage(String from, String message)
            {
                mFrom = from;
                mMessage = message;
            }

            @Override
            public boolean equals(Object o)
            {
                if (o instanceof TextMessage)
                {
                    return equals((TextMessage) o);
                }
                return super.equals(o);
            }

            public boolean equals(TextMessage o)
            {
                return mFrom.equals(o.mFrom) && mMessage.equals(o.mMessage);
            }

            @Override
            public int hashCode()
            {
                return mFrom.hashCode() + mMessage.hashCode();
            }
        }

        private final List<TextMessage> mTextMessages;

        public GoogleHangoutsNotificationParser(Context applicationContext, FooTextToSpeech textToSpeech)
        {
            super(applicationContext, textToSpeech, "com.google.android.talk", applicationContext.getString(R.string.hangouts_package_app_name));

            mTextMessages = new LinkedList<>();
        }

        private TextMessage addTextMessage(String from, String message)
        {
            TextMessage textMessage = new TextMessage(from, message);
            if (mTextMessages.contains(textMessage))
            {
                return null;
            }

            mTextMessages.add(textMessage);

            return textMessage;
        }

        @Override
        public boolean onNotificationPosted(StatusBarNotification sbn)
        {
            //super.onNotificationPosted(sbn);

            Notification notification = sbn.getNotification();

            Bundle extras = notification.extras;
            if (extras == null)
            {
                return false;
            }

            List<TextMessage> textMessages = new LinkedList<>();

            CharSequence androidTitle = extras.getCharSequence("android.title", "Unknown User");
            CharSequence androidText = extras.getCharSequence("android.text", "Unknown Text");
            CharSequence[] androidTextLines = extras.getCharSequenceArray("android.textLines");

            if (androidTextLines != null)
            {
                for (CharSequence textLine : androidTextLines)
                {
                    String[] parts = textLine.toString().split("  ");
                    String from = parts[0];
                    String message = parts[1];
                    TextMessage textMessage = addTextMessage(from, message);
                    if (textMessage != null)
                    {
                        textMessages.add(textMessage);
                    }
                }
            }
            else
            {
                String from = androidTitle.toString();
                String message = androidText.toString();
                TextMessage textMessage = addTextMessage(from, message);
                if (textMessage != null)
                {
                    textMessages.add(textMessage);
                }
            }

            // TODO:(pv) Prevent repeats or overly verbose speaking of rolled up texts...

            int count = textMessages.size();
            if (count == 0)
            {
                return false;
            }

            String title = mResources.getQuantityString(R.plurals.X_new_messages, count, count);
            mTextToSpeech.speak(title);
            for (TextMessage textMessage : textMessages)
            {
                mTextToSpeech.silence(750);
                mTextToSpeech.speak(mApplicationContext.getString(R.string.X_says, textMessage.mFrom));
                //mTextToSpeech.speak("to " + to);
                mTextToSpeech.silence(500);
                mTextToSpeech.speak(textMessage.mMessage);
            }

            return true;
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification sbn)
        {
            mTextMessages.clear();
        }
    }

    public static class GmailNotificationParser
            extends NotificationParser
    {
        protected GmailNotificationParser(Context context, FooTextToSpeech textToSpeech)
        {
            super(context, textToSpeech, "com.google.android.gm", "G mail");
        }
    }

    public static class GoogleMessengerNotificationParser
            extends NotificationParser
    {
        protected GoogleMessengerNotificationParser(Context context, FooTextToSpeech textToSpeech)
        {
            super(context, textToSpeech, "com.google.android.apps.messaging", "Google Messenger");
        }
    }
}
