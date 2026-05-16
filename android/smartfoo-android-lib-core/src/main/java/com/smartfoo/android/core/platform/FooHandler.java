package com.smartfoo.android.core.platform;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.smartfoo.android.core.FooString;

public class FooHandler
        extends Handler
{
    private final FooIncrementingIntegerValue mMessageCodes;

    /**
     * Returns the next unique message code from the associated {@link FooIncrementingIntegerValue},
     * then increments the counter.
     *
     * @return the message code before incrementing
     */
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

    /**
     * Associates this handler with the given message-code counter and a callback, using the
     * main looper.
     *
     * @param messageCodes the counter used to generate unique message codes; a new default instance
     *                     is created if null
     * @param callback     the callback interface in which to handle messages, or null
     */
    public FooHandler(FooIncrementingIntegerValue messageCodes, Callback callback)
    {
        this(messageCodes, null, callback);
    }

    /**
     * Full constructor. Associates this handler with the given looper, message-code counter, and
     * callback.
     *
     * @param messageCodes the counter used to generate unique message codes; a new default instance
     *                     is created if null
     * @param looper       the looper to use; falls back to {@link Looper#getMainLooper()} if null
     * @param callback     the callback interface in which to handle messages, or null
     */
    public FooHandler(FooIncrementingIntegerValue messageCodes, Looper looper, Callback callback)
    {
        super(looper != null ? looper : Looper.getMainLooper(), callback);

        if (messageCodes == null)
        {
            messageCodes = new FooIncrementingIntegerValue();
        }

        mMessageCodes = messageCodes;
    }

    /**
     * Returns a human-readable description of this handler that includes the associated looper's
     * thread name, useful for logging and diagnostics.
     *
     * @return a string of the form {@code "FooHandler@XXXXXXXX { getLooper().getThread().getName()="name" }"}
     */
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

    /**
     * Obtains a {@link Message} from the pool and immediately sends it to this handler's queue.
     *
     * @param what the message identifier
     * @param obj  the optional arbitrary object to attach
     * @return the sent message
     */
    public Message obtainAndSendMessage(int what, Object obj)
    {
        return obtainAndSendMessage(what, 0, 0, obj);
    }

    /**
     * Obtains a {@link Message} from the pool and immediately sends it to this handler's queue.
     *
     * @param what the message identifier
     * @param arg1 first integer argument
     * @param obj  the optional arbitrary object to attach
     * @return the sent message
     */
    public Message obtainAndSendMessage(int what, int arg1, Object obj)
    {
        return obtainAndSendMessage(what, arg1, 0, obj);
    }

    /**
     * Obtains a {@link Message} from the pool and immediately sends it to this handler's queue.
     *
     * @param what the message identifier
     * @param arg1 first integer argument
     * @param arg2 second integer argument
     * @return the sent message
     */
    public Message obtainAndSendMessage(int what, int arg1, int arg2)
    {
        return obtainAndSendMessage(what, arg1, arg2, null);
    }

    /**
     * Obtains a {@link Message} from the pool and immediately sends it to this handler's queue.
     *
     * @param what the message identifier
     * @param arg1 first integer argument
     * @param arg2 second integer argument
     * @param obj  the optional arbitrary object to attach
     * @return the sent message
     */
    public Message obtainAndSendMessage(int what, int arg1, int arg2, Object obj)
    {
        return obtainAndSendMessageDelayed(what, arg1, arg2, obj, 0);
    }

    /**
     * Obtains a {@link Message} and posts it to this handler's queue with a delay.
     *
     * @param what         the message identifier
     * @param obj          the optional arbitrary object to attach
     * @param delayMillis  delay in milliseconds before delivery
     * @return the sent message
     */
    public Message obtainAndSendMessageDelayed(int what, Object obj, long delayMillis)
    {
        return obtainAndSendMessageDelayed(what, 0, 0, obj, delayMillis);
    }

    /**
     * Obtains a {@link Message} and posts it to this handler's queue with a delay.
     *
     * @param what         the message identifier
     * @param arg1         first integer argument
     * @param obj          the optional arbitrary object to attach
     * @param delayMillis  delay in milliseconds before delivery
     * @return the sent message
     */
    public Message obtainAndSendMessageDelayed(int what, int arg1, Object obj, long delayMillis)
    {
        return obtainAndSendMessageDelayed(what, arg1, 0, obj, delayMillis);
    }

    /**
     * Obtains a {@link Message} and posts it to this handler's queue with a delay.
     *
     * @param what         the message identifier
     * @param arg1         first integer argument
     * @param arg2         second integer argument
     * @param delayMillis  delay in milliseconds before delivery
     * @return the sent message
     */
    public Message obtainAndSendMessageDelayed(int what, int arg1, int arg2, long delayMillis)
    {
        return obtainAndSendMessageDelayed(what, arg1, arg2, null, delayMillis);
    }

    /**
     * Obtains a {@link Message} and posts it to this handler's queue with a delay.
     *
     * @param what         the message identifier
     * @param arg1         first integer argument
     * @param arg2         second integer argument
     * @param obj          the optional arbitrary object to attach
     * @param delayMillis  delay in milliseconds before delivery
     * @return the sent message
     */
    public Message obtainAndSendMessageDelayed(int what, int arg1, int arg2, Object obj, long delayMillis)
    {
        Message message = obtainMessage(what, arg1, arg2, obj);
        sendMessageDelayed(message, delayMillis);
        return message;
    }

    /**
     * Posts a runnable to this handler's queue with an associated token object, with no delay.
     *
     * @param r     the runnable to post
     * @param token an object that can be used to cancel the posted runnable via
     *              {@link android.os.Handler#removeCallbacksAndMessages(Object)}
     * @return true if the message was successfully placed in the queue
     */
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