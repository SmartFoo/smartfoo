package com.smartfoo.android.core.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.app.GenericPromptSingleButtonDialogFragment.GenericPromptSingleButtonDialogFragmentCallbacks;
import com.smartfoo.android.core.logging.FooLog;

/**
 * A reusable {@link androidx.fragment.app.DialogFragment} that shows a prompt with a single
 * acknowledgement button.
 *
 * <p>Create instances via one of the {@code newInstance} factory methods. The dialog result is
 * reported through {@link GenericPromptSingleButtonDialogFragmentCallbacks}.
 * The outcome ({@link Result#Accepted} or {@link Result#Canceled}) is available via
 * {@link #getResult()} after the dialog is dismissed.</p>
 */
public class GenericPromptSingleButtonDialogFragment
        extends CallbackDialogFragment<GenericPromptSingleButtonDialogFragmentCallbacks>
{
    private static final String TAG = FooLog.TAG(GenericPromptSingleButtonDialogFragment.class);

    /**
     * Callback interface delivered when the user dismisses the dialog.
     */
    public interface GenericPromptSingleButtonDialogFragmentCallbacks
    {
        /**
         * Called when the dialog has been dismissed with any result (accepted or cancelled).
         *
         * @param dialogFragment the dialog that was dismissed; call {@link #getResult()} to read
         *                       the outcome
         * @return {@code true} if the event was handled, {@code false} otherwise
         */
        boolean onGenericPromptSingleButtonDialogFragmentResult(@NonNull GenericPromptSingleButtonDialogFragment dialogFragment);
    }

    public enum Result
    {
        Canceled,
        Accepted,
    }

    /**
     * Creates a new instance using Android string-resource IDs.
     *
     * @param context    a context used to resolve the resource IDs; must not be null
     * @param title      string resource ID for the dialog title
     * @param message    string resource ID for the dialog message
     * @param textButton string resource ID for the single acknowledgement button label
     * @return a configured dialog fragment
     */
    public static GenericPromptSingleButtonDialogFragment newInstance(
            @NonNull Context context,
            int title,
            int message,
            int textButton)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        return newInstance(context.getString(title),
                context.getString(message),
                context.getString(textButton));
    }

    /**
     * Creates a new instance with string arguments.
     *
     * @param title      dialog title text
     * @param message    dialog message text
     * @param textButton acknowledgement button label, or {@code null} for the system default
     * @return a configured dialog fragment
     */
    public static GenericPromptSingleButtonDialogFragment newInstance(String title,
                                                                      String message,
                                                                      String textButton)
    {
        GenericPromptSingleButtonDialogFragment fragment = new GenericPromptSingleButtonDialogFragment();

        Bundle arguments = fragment.makeArguments(title, message, textButton);

        fragment.setArguments(arguments);

        return fragment;
    }

    private static final String ARG_TITLE       = "ARG_TITLE";
    private static final String ARG_MESSAGE     = "ARG_MESSAGE";
    private static final String ARG_BUTTON_TEXT = "ARG_BUTTON_TEXT";

    /**
     * Builds a {@link Bundle} of fragment arguments.
     *
     * @param title      dialog title text
     * @param message    dialog message text
     * @param textButton acknowledgement button label, or {@code null} for the system default
     * @return a bundle suitable for passing to {@link #setArguments}
     */
    protected Bundle makeArguments(String title,
                                   String message,
                                   String textButton)
    {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        arguments.putString(ARG_MESSAGE, message);
        arguments.putString(ARG_BUTTON_TEXT, textButton);
        return arguments;
    }

    protected String mMessage;
    protected Result mResult;

    public GenericPromptSingleButtonDialogFragment()
    {
        super(new GenericPromptSingleButtonDialogFragmentCallbacks()
        {
            @Override
            public boolean onGenericPromptSingleButtonDialogFragmentResult(GenericPromptSingleButtonDialogFragment dialogFragment)
            {
                return false;
            }
        });
    }

    /**
     * Returns the dialog message that was passed to the factory method.
     *
     * @return the message string, or {@code null} if the dialog has not yet been created
     */
    public String getMessage()
    {
        return mMessage;
    }

    /**
     * Returns the outcome of the last user interaction with this dialog.
     *
     * @return {@link Result#Accepted} if the button was tapped, {@link Result#Canceled} if the
     *         dialog was dismissed without tapping the button; {@code null} if the dialog has not
     *         been dismissed yet
     */
    public Result getResult()
    {
        return mResult;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        //FooLog.e(TAG, "onCreateDialog: savedInstanceState=" + savedInstanceState);

        Bundle arguments = getArguments();
        String title = arguments.getString(ARG_TITLE);
        mMessage = arguments.getString(ARG_MESSAGE);
        String textButton = arguments.getString(ARG_BUTTON_TEXT);
        if (textButton == null)
        {
            textButton = getString(android.R.string.ok);
        }

        Context context = getActivity();

        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(mMessage)
                .setPositiveButton(textButton, new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        onResult(dialog, Result.Accepted);
                    }
                })
                .create();

        alertDialog.setCanceledOnTouchOutside(false);

        return alertDialog;
    }

    /**
     * Finalises the dialog result, notifies the callback, and dismisses the dialog.
     * Subsequent calls are silently ignored so that {@code onCancel}/{@code onDismiss} cannot
     * overwrite a result already set by a button tap.
     *
     * @param dialog the dialog interface to dismiss; may be {@code null} when called from
     *               {@code onDismiss} where the dialog is already going away
     * @param result the raw result from the button or cancel event
     */
    protected void onResult(DialogInterface dialog, Result result)
    {
        //FooLog.e(TAG, "onResult(dialog=" + dialog + ", result=" + result + ')');
        if (mResult != null)
        {
            //FooLog.e(TAG, "onResult: mResult(" + mResult + ") already set; ignoring");
            return;
        }

        mResult = result;

        mCallback.onGenericPromptSingleButtonDialogFragmentResult(this);

        if (dialog != null)
        {
            dialog.dismiss();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog)
    {
        //FooLog.e(TAG, "onCancel");
        super.onCancel(dialog);
        onResult(dialog, Result.Canceled);
    }

    @Override
    public void onDismiss(DialogInterface dialog)
    {
        //FooLog.e(TAG, "onDismiss");
        super.onDismiss(dialog);
        onResult(null, Result.Canceled);
    }
}
