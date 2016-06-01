package com.smartfoo.android.core.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;

public class GenericPromptSingleButtonDialogFragment
        extends CallbackDialogFragment<GenericPromptSingleButtonDialogFragmentCallbacks>
{
    public enum Result
    {
        Canceled,
        Accepted,
    }

    public static GenericPromptSingleButtonDialogFragment newInstance(Context context,
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
        Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        arguments.putString(ARG_MESSAGE, message);
        arguments.putString(ARG_BUTTON_TEXT, textButton);

        GenericPromptSingleButtonDialogFragment fragment = new GenericPromptSingleButtonDialogFragment();
        fragment.setArguments(arguments);

        return fragment;
    }

    private static final String ARG_TITLE       = "ARG_TITLE";
    private static final String ARG_MESSAGE     = "ARG_MESSAGE";
    private static final String ARG_BUTTON_TEXT = "ARG_BUTTON_TEXT";

    private String mMessage;
    private Result mResult;

    public GenericPromptSingleButtonDialogFragment()
    {
        super(new GenericPromptSingleButtonDialogFragmentCallbacks()
        {
            @Override
            public boolean onGenericPromptSingleButtonDialogFragmentResult(GenericPromptSingleButtonDialogFragment dialogFragment, String fragmentTagName)
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
        Bundle arguments = getArguments();
        String title = arguments.getString(ARG_TITLE);
        mMessage = arguments.getString(ARG_MESSAGE);
        String textButton = arguments.getString(ARG_BUTTON_TEXT);
        if (textButton == null)
        {
            textButton = getString(android.R.string.ok);
        }

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
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
        mResult = result;

        GenericPromptSingleButtonDialogFragment dialogFragment = GenericPromptSingleButtonDialogFragment.this;
        String tag = dialogFragment.getTag();

        mCallback.onGenericPromptSingleButtonDialogFragmentResult(dialogFragment, tag);

        dialog.dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog)
    {
        super.onCancel(dialog);
        onResult(dialog, Result.Canceled);
    }
}
