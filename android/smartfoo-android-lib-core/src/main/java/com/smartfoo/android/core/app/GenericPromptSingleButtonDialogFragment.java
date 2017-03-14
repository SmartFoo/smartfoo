package com.smartfoo.android.core.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.app.GenericPromptSingleButtonDialogFragment.GenericPromptSingleButtonDialogFragmentCallbacks;
import com.smartfoo.android.core.logging.FooLog;

public class GenericPromptSingleButtonDialogFragment
        extends CallbackDialogFragment<GenericPromptSingleButtonDialogFragmentCallbacks>
{
    private static final String TAG = FooLog.TAG(GenericPromptSingleButtonDialogFragment.class);

    public interface GenericPromptSingleButtonDialogFragmentCallbacks
    {
        /**
         * @param dialogFragment dialogFragment
         * @return true if handled, false if not handled
         */
        boolean onGenericPromptSingleButtonDialogFragmentResult(
                @NonNull
                        GenericPromptSingleButtonDialogFragment dialogFragment);
    }

    public enum Result
    {
        Canceled,
        Accepted,
    }

    public static GenericPromptSingleButtonDialogFragment newInstance(
            @NonNull
                    Context context,
            int title,
            int message,
            int textButton)
    {
        return newInstance(context.getString(title),
                context.getString(message),
                context.getString(textButton));
    }

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

    public String getMessage()
    {
        return mMessage;
    }

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
