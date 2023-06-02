package com.smartfoo.android.core.platform;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.smartfoo.android.core.FooString;

public class FooHandler
        extends Handler
{
    private final FooIncrementingIntegerValue mMessageCodes;

    public int getNextMessageCode()
    {
        return mMessageCodes.getNextMessageCode();
    }

    /**
     * Default constructor associates this handler with the {@link Looper} for the
     * main thread.
     */
    public FooHandler()
    {
        this((Looper) null);
    }

    /**
     * Constructor associates this handler with the {@link Looper} for the
     * main thread and takes a callback interface in which you can handle
     * messages.
     *
     * @param callback The callback interface in which to handle messages, or null.
     */
    public FooHandler(Callback callback)
    {
        this(null, null, callback);
    }

    /**
     * Use the provided {@link Looper} instead of the default one.
     *
     * @param looper The looper, must not be null.
     */
    public FooHandler(Looper looper)
    {
        this(null, looper, null);
    }

    /**
     * Use the provided {@link Looper} instead of the default one and take a callback
     * interface in which to handle messages.
     *
     * @param looper   The looper, must not be null.
     * @param callback The callback interface in which to handle messages, or null.
     */
    public FooHandler(Looper looper, Callback callback)
    {
        this(null, looper, callback);
    }

    public FooHandler(FooIncrementingIntegerValue messageCodes, Callback callback)
    {
        this(messageCodes, null, callback);
    }

    public FooHandler(FooIncrementingIntegerValue messageCodes, Looper looper, Callback callback)
    {
        super(looper != null ? looper : Looper.getMainLooper(), callback);

        if (messageCodes == null)
        {
            messageCodes = new FooIncrementingIntegerValue();
        }

        mMessageCodes = messageCodes;
    }

    @Override
    public String toString()
    {
        String infoKey = "getLooper()";
        String infoValue = null;

        Looper looper = getLooper();
        if (looper != null)
        {
            infoKey += ".getThread().getName()";
            infoValue = looper.getThread().getName();
        }
        return getClass().getSimpleName() + '@' + Integer.toHexString(hashCode()) +
               " { " +
               infoKey + '=' + FooString.quote(infoValue) +
               " }";
    }

    public Message obtainAndSendMessage(int what, Object obj)
    {
        return obtainAndSendMessage(what, 0, 0, obj);
    }

    public Message obtainAndSendMessage(int what, int arg1, Object obj)
    {
        return obtainAndSendMessage(what, arg1, 0, obj);
    }

    public Message obtainAndSendMessage(int what, int arg1, int arg2)
    {
        return obtainAndSendMessage(what, arg1, arg2, null);
    }

    public Message obtainAndSendMessage(int what, int arg1, int arg2, Object obj)
    {
        return obtainAndSendMessageDelayed(what, arg1, arg2, obj, 0);
    }

    public Message obtainAndSendMessageDelayed(int what, Object obj, long delayMillis)
    {
        return obtainAndSendMessageDelayed(what, 0, 0, obj, delayMillis);
    }

    public Message obtainAndSendMessageDelayed(int what, int arg1, Object obj, long delayMillis)
    {
        return obtainAndSendMessageDelayed(what, arg1, 0, obj, delayMillis);
    }

    public Message obtainAndSendMessageDelayed(int what, int arg1, int arg2, long delayMillis)
    {
        return obtainAndSendMessageDelayed(what, arg1, arg2, null, delayMillis);
    }

    public Message obtainAndSendMessageDelayed(int what, int arg1, int arg2, Object obj, long delayMillis)
    {
        Message message = obtainMessage(what, arg1, arg2, obj);
        sendMessageDelayed(message, delayMillis);
        return message;
    }

    public boolean post(Runnable r, Object token)
    {
        return postDelayed(r, token, 0);
    }

    /*
    public boolean postDelayed(Runnable r, Object token, long delayMillis)
    {
        return sendMessageDelayed(getPostMessage(r, token), delayMillis);
    }
    */

    private Message getPostMessage(Runnable r, Object token)
    {
        Message m = Message.obtain(this, r);
        m.obj = token;
        return m;
    }
}