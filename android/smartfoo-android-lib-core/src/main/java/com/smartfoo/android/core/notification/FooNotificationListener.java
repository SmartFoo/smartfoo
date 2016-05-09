package com.smartfoo.android.core.notification;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.LayerDrawable;
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
import android.widget.ImageView;
import android.widget.RemoteViews;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.texttospeech.FooTextToSpeech;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
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
        super.onCreate();

        mRemoteController = new RemoteController(this, this);
        mTextToSpeech = FooTextToSpeech.getInstance().start(this);

        addNotificationParser(new PandoraNotificationParser(mTextToSpeech));
        addNotificationParser(new SpotifyNotificationParser(mTextToSpeech));
        addNotificationParser(new GoogleHangoutsNotificationParser(mTextToSpeech));
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
        String packageName = sbn.getPackageName();
        NotificationParser notificationParser = mNotificationParsers.get(packageName);
        if (notificationParser != null)
        {
            notificationParser.parse(this, sbn);
        }
        else
        {
            // TODO:(pv) Try to just speak the ticker text
            Notification notification = sbn.getNotification();
            CharSequence tickerText = notification.tickerText;
            if (!FooString.isNullOrEmpty(tickerText))
            {
                mTextToSpeech.speak(tickerText.toString());
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        /*
        String packageName = sbn.getPackageName();
        NotificationParser notificationParser = mNotificationParsers.get(packageName);
        if (notificationParser != null)
        {
            // TODO:(pv) Reset any cache in the parser
            notificationParser.reset(this, sbn);
        }
        */
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
        return Integer.toString(value) + " (" + Integer.toHexString(value) + ')';
    }

    // TODO:(pv) Make a UI that shows all StatusBarNotification fields, especially:
    //  Notification.tickerText
    //  All ImageView Resource Ids and TextView Texts in BigContentView
    //  All ImageView Resource Ids and TextView Texts in ContentView
    //  The user could then select what to say for what images, and prefixing for suffixing texts

    public static abstract class NotificationParser
    {
        protected FooTextToSpeech mTextToSpeech;

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

        @Nullable
        public static View mockContentView(
                @NonNull
                Context context,
                @NonNull
                StatusBarNotification sbn)
        {
            return mockRemoteView(context, getContentView(sbn));
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

        @Nullable
        public static View findViewByName(
                @NonNull
                View parent,
                @NonNull
                String childName)
        {
            FooLog.e(TAG, "findViewByName(parent=" + parent + ", childName=" + FooString.quote(childName) + ')');

            int id = getIdOfChildWithName(parent, childName);
            if (id == 0)
            {
                return null;
            }

            return parent.findViewById(id);
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
            int IMAGE_RESOURCE_ID = 3;
            //int BITMAP     = 4;
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

                // Find the setText() and setTime() reflection actions
                for (int i = 0; i < actions.size(); i++)
                {
                    Parcelable parcelable = actions.get(i);

                    Parcel parcel = Parcel.obtain();

                    try
                    {
                        parcelable.writeToParcel(parcel, 0);

                        parcel.setDataPosition(0);

                        //
                        // 2 == ReflectionAction, per:
                        // https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1072
                        //
                        int tag = parcel.readInt();
                        if (tag != 2)
                        {
                            continue;
                        }

                        int viewId = parcel.readInt();
                        if (viewId != id)
                        {
                            continue;
                        }

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
                            default:
                                continue;
                        }

                        int actionType = parcel.readInt();
                        /*
                        if (actionType != valueType)
                        {
                            continue;
                        }
                        */

                        // per:
                        // https://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/RemoteViews.java#L1116
                        switch (actionType)
                        {
                            //case ValueTypes.ICON:
                            //    int foo = parcel.readInt();
                            //    return foo;//Icon.CREATOR.createFromParcel(parcel);
                            case ActionTypes.CHAR_SEQUENCE:
                                return TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                            case ActionTypes.INT:
                                return parcel.readInt();
                            default:
                                throw new IllegalArgumentException("unhandled actionType " + actionType);
                        }

                        //Icon.CREATOR.createFromParcel(parcel);
                    }
                    finally
                    {
                        parcel.recycle();
                    }
                }
            }
            catch (IllegalAccessException e)
            {
                FooLog.e(TAG, "getRemoteViewTextById", e);
            }
            catch (NoSuchFieldException e)
            {
                FooLog.e(TAG, "getRemoteViewTextById", e);
            }

            return null;
        }

        public static void walkView(View view)
        {
            FooLog.e(TAG, "walkView: view=" + view);

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

        protected NotificationParser(FooTextToSpeech textToSpeech)
        {
            mTextToSpeech = textToSpeech;
        }

        public abstract String getPackageName();

        public void parse(Context context, StatusBarNotification sbn)
        {
            //String groupKey = sbn.getGroupKey();
            //String key = sbn.getKey();
            //UserHandle user = sbn.getUser();
            String packageName = sbn.getPackageName();
            long postTime = sbn.getPostTime();

            Notification notification = sbn.getNotification();
            FooLog.e(TAG, "parse: notification=" + notification);

            int id = sbn.getId();
            String tag = sbn.getTag();

            CharSequence tickerText = notification.tickerText;

            // TODO:(pv) Seriously, introspect and walk all StatusBarNotification fields, especially:
            //  Notification.tickerText
            //  All ImageView Resource Ids and TextView Texts in BigContentView
            //  All ImageView Resource Ids and TextView Texts in ContentView

            RemoteViews bigContentView = notification.bigContentView;
            View mockBigContentView = mockRemoteView(context, bigContentView);
            FooLog.e(TAG, "parse: bigContentView");
            walkView(mockBigContentView);
            FooLog.e(TAG, "parse: --------");
            RemoteViews contentView = notification.contentView;
            View mockContentView = mockRemoteView(context, contentView);
            FooLog.e(TAG, "parse: contentView");
            walkView(mockContentView);

            //RemoteViews headUpContentView = notification.headsUpContentView;

            Notification.Action[] actions = notification.actions;

            //String category = notification.category;

            Bundle extras = notification.extras;
            FooLog.e(TAG, "parse: extras=" + FooPlatformUtils.toString(extras));
        }
    }

    public static class PandoraNotificationParser
            extends NotificationParser
    {
        private boolean mLastIsPlaying;
        private String  mLastArtist;
        private String  mLastTitle;
        private String  mLastStation;

        public PandoraNotificationParser(FooTextToSpeech textToSpeech)
        {
            super(textToSpeech);
        }

        @Override
        public String getPackageName()
        {
            return "com.pandora.android";
        }

        @Override
        public void parse(Context context, StatusBarNotification sbn)
        {
            super.parse(context, sbn);

            // TODO:(pv) We may need to work out of BOTH bigContentView and contentView...

            RemoteViews bigContentView = getBigContentView(sbn);
            //RemoteViews remoteViews = getContentView(sbn);

            View mockRemoteView = mockRemoteView(context, bigContentView);
            if (mockRemoteView == null)
            {
                return;
            }

            ImageView imageViewPlay = (ImageView) findViewByName(mockRemoteView, "play");
            LayerDrawable foo = (LayerDrawable) imageViewPlay.getDrawable();
            //int[] state = foo.getState();
            //Drawable bar = foo.getDrawable(state[0]);
            //Notification notification = getNotification(sbn);
            //Bundle extras = notification.extras;

            Context otherAppContext = createPackageContext(context, bigContentView);
            Resources resources = otherAppContext.getResources();
            String packageName = otherAppContext.getPackageName();
            int ic_mini_controller_play = resources.getIdentifier("ic_mini_controller_play", "drawable", packageName);
            FooLog.e(TAG, "parse: ic_mini_controller_play=" + toVerboseString(ic_mini_controller_play));
            //int idDrawablePlay2 = resources.getIdentifier("ic_play_arrow_grey600_48dp", "drawable", packageName);
            int ic_mini_controller_pause = resources.getIdentifier("ic_mini_controller_pause", "drawable", packageName);
            FooLog.e(TAG, "parse: ic_mini_controller_pause=" + toVerboseString(ic_mini_controller_pause));

            //resources.getResourceName()

            //resources.getDrawable()

            int idTitle = getIdOfChildWithName(mockRemoteView, "title");
            FooLog.e(TAG, "parse: idTitle=" + toVerboseString(idTitle));
            if (idTitle == 0)
            {
                return;
            }

            int idArtist = getIdOfChildWithName(mockRemoteView, "artist");
            FooLog.e(TAG, "parse: idArtist=" + toVerboseString(idArtist));
            if (idArtist == 0)
            {
                return;
            }

            int idStation = getIdOfChildWithName(mockRemoteView, "station");
            FooLog.e(TAG, "parse: idStation=" + toVerboseString(idStation));
            if (idStation == 0)
            {
                return;
            }

            /*
            int idText2 = getIdOfChildWithName(mockRemoteView, "text2");
            FooLog.e(TAG, "parse: idText2=" + toVerboseString(idText2));
            if (idText2 == 0)
            {
                return;
            }

            int idText3 = getIdOfChildWithName(mockRemoteView, "text3");
            FooLog.e(TAG, "parse: idText3=" + toVerboseString(idText3));
            if (idText3 == 0)
            {
                return;
            }
            */

            int idPlay = getIdOfChildWithName(mockRemoteView, "play");
            FooLog.e(TAG, "parse: idPlay=" + toVerboseString(idPlay));
            if (idPlay == 0)
            {
                return;
            }

            // TODO:(pv) Can we better determine play/pause based on SetOnClickPendingIntent?
            Integer playImageResourceId = (Integer) getRemoteViewValueById(bigContentView, idPlay, ValueTypes.IMAGE_RESOURCE_ID);
            // pause showing (ie: playing) == 2130838231 (0x7F0202D7)
            // play showing (ie: paused) == 2130838230 (0x0x7F0202D6)
            FooLog.e(TAG, "parse: playImageResourceId=" + toVerboseString(playImageResourceId));
            boolean isPlaying = playImageResourceId != null && playImageResourceId == 0x7F0202D6;
            FooLog.e(TAG, "parse: isPlaying=" + isPlaying);

            String textTitle = (String) getRemoteViewValueById(bigContentView, idTitle, ValueTypes.TEXT);
            textTitle = unknownIfNullOrEmpty(textTitle);
            FooLog.e(TAG, "parse: textTitle=" + FooString.quote(textTitle));
            // "Advertisement"

            String textArtist = (String) getRemoteViewValueById(bigContentView, idArtist, ValueTypes.TEXT);
            textArtist = unknownIfNullOrEmpty(textArtist);
            FooLog.e(TAG, "parse: textArtist=" + FooString.quote(textArtist));
            //

            String textStation = (String) getRemoteViewValueById(bigContentView, idStation, ValueTypes.TEXT);
            textStation = unknownIfNullOrEmpty(textStation);
            FooLog.e(TAG, "parse: textStation=" + FooString.quote(textStation));

            if ("Advertisement".equalsIgnoreCase(textTitle) &&
                "Your station will be right back…".equalsIgnoreCase(textArtist))
            {
                // It's a commercial!
                return;
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
                    mTextToSpeech.speak("Pandora playing");
                    mTextToSpeech.silence(500);
                    mTextToSpeech.speak("artist " + textArtist);
                    mTextToSpeech.silence(500);
                    mTextToSpeech.speak("title " + textTitle);
                    //mTextToSpeech.silence(500);
                    //mTextToSpeech.speak("station " + text3);
                }
                else
                {
                    mTextToSpeech.speak("Pandora paused");
                }

                mTextToSpeech.silence(500);
            }
        }
    }

    public static class SpotifyNotificationParser
            extends NotificationParser
    {
        private boolean mLastIsPlaying;
        private String  mLastArtist;
        private String  mLastTitle;
        private String  mLastAlbum;

        public SpotifyNotificationParser(FooTextToSpeech textToSpeech)
        {
            super(textToSpeech);
        }

        @Override
        public String getPackageName()
        {
            return "com.spotify.music";
        }

        @Override
        public void parse(Context context, StatusBarNotification sbn)
        {
            //super.parse(context, sbn);

            RemoteViews contentView = getContentView(sbn);

            View mockRemoteView = mockRemoteView(context, contentView);
            if (mockRemoteView == null)
            {
                return;
            }

            int idTitle = getIdOfChildWithName(mockRemoteView, "title");
            if (idTitle == 0)
            {
                return;
            }

            int idSubtitle = getIdOfChildWithName(mockRemoteView, "subtitle");
            if (idSubtitle == 0)
            {
                return;
            }

            int idPause = getIdOfChildWithName(mockRemoteView, "pause");
            if (idPause == 0)
            {
                return;
            }

            int pauseVisibility = (int) getRemoteViewValueById(contentView, idPause, ValueTypes.VISIBILITY);
            FooLog.e(TAG, "parse: pauseVisibility=" + pauseVisibility);
            boolean isPlaying = pauseVisibility == View.VISIBLE;
            FooLog.e(TAG, "parse: isPlaying=" + isPlaying);

            String textTitle = (String) getRemoteViewValueById(contentView, idTitle, ValueTypes.TEXT);
            textTitle = unknownIfNullOrEmpty(textTitle);
            FooLog.e(TAG, "parse: textTitle=" + FooString.quote(textTitle));

            String textSubtitle = (String) getRemoteViewValueById(contentView, idSubtitle, ValueTypes.TEXT);
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

            FooLog.e(TAG, "parse: textArtist=" + FooString.quote(textArtist));
            FooLog.e(TAG, "parse: textAlbum=" + FooString.quote(textAlbum));

            if (textArtist == null && textAlbum == null)
            {
                // It's a commercial!
                return;
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
                    mTextToSpeech.speak("Spotify playing");
                    mTextToSpeech.silence(500);
                    mTextToSpeech.speak("artist " + textArtist);
                    mTextToSpeech.silence(500);
                    mTextToSpeech.speak("title " + textTitle);
                    mTextToSpeech.silence(500);
                    mTextToSpeech.speak("album " + textAlbum);
                }
                else
                {
                    mTextToSpeech.speak("Spotify paused");
                }

                mTextToSpeech.silence(500);
            }
        }
    }

    public static class GoogleHangoutsNotificationParser
            extends NotificationParser
    {
        public GoogleHangoutsNotificationParser(FooTextToSpeech textToSpeech)
        {
            super(textToSpeech);
        }

        @Override
        public String getPackageName()
        {
            return "com.google.android.talk";
        }

        @Override
        public void parse(Context context, StatusBarNotification sbn)
        {
            super.parse(context, sbn);

            Notification notification = sbn.getNotification();

            String androidTitle = null;
            CharSequence androidText = null;
            String androidSummaryText = null;

            Bundle extras = notification.extras;
            if (extras != null)
            {
                androidTitle = extras.getString("android.title");
                androidText = extras.getCharSequence("android.text");
                androidSummaryText = extras.getString("android.summaryText");
            }

            String from = FooString.isNullOrEmpty(androidTitle) ? "Unknown" : androidTitle;
            String to = FooString.isNullOrEmpty(androidSummaryText) ? "Unknown" : androidSummaryText;
            String message = FooString.isNullOrEmpty(androidText) ? "nothing zip zilch zero nada silence" : androidText
                    .toString();

            FooLog.e(TAG, "parse: from=" + FooString.quote(from));
            FooLog.e(TAG, "parse: to=" + FooString.quote(to));
            FooLog.e(TAG, "parse: message=" + FooString.quote(message));

            // TODO:(pv) Prevent repeats or overly verbose speaking of rolled up texts...

            mTextToSpeech.speak(from + " says ");
            //mTextToSpeech.speak("to " + to);
            mTextToSpeech.silence(500);
            mTextToSpeech.speak(message);
            mTextToSpeech.silence(500);
        }
    }
}
