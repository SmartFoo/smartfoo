package com.smartfoo.android.core.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;

public class GenericPromptPositiveNegativeDialogFragment
        extends CallbackDialogFragment<GenericPromptPositiveNegativeDialogFragmentCallbacks>
{
    public enum Result
    {
        Canceled,
        Positive,
        Negative,
    }

    public static GenericPromptPositiveNegativeDialogFragment newInstance(Context context,
                                                                          int title,
                                                                          int message,
                                                                          int textPositiveButton,
                                                                          int textNegativeButton)
    {
        return newInstance(context.getString(title),
                context.getString(message),
                context.getString(textPositiveButton),
                context.getString(textNegativeButton));
    }

    public static GenericPromptPositiveNegativeDialogFragment newInstance(String title,
                                                                          String message,
                                                                          String textPositiveButton,
                                                                          String textNegativeButton)
    {
        GenericPromptPositiveNegativeDialogFragment fragment = new GenericPromptPositiveNegativeDialogFragment();

        Bundle arguments = fragment.makeArguments(title, message, textPositiveButton, textNegativeButton);

        fragment.setArguments(arguments);

        return fragment;
    }

    protected static final String ARG_TITLE                = "ARG_TITLE";
    protected static final String ARG_MESSAGE              = "ARG_MESSAGE";
    protected static final String ARG_POSITIVE_BUTTON_TEXT = "ARG_POSITIVE_BUTTON_TEXT";
    protected static final String ARG_NEGATIVE_BUTTON_TEXT = "ARG_NEGATIVE_BUTTON_TEXT";

    protected Bundle makeArguments(String title,
                                   String message,
                                   String textPositiveButton,
                                   String textNegativeButton)
    {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        arguments.putString(ARG_MESSAGE, message);
        arguments.putString(ARG_POSITIVE_BUTTON_TEXT, textPositiveButton);
        arguments.putString(ARG_NEGATIVE_BUTTON_TEXT, textNegativeButton);
        return arguments;
    }

    private Result mResult;

    public GenericPromptPositiveNegativeDialogFragment()
    {
        super(new GenericPromptPositiveNegativeDialogFragmentCallbacks()
        {
            @Override
            public boolean onGenericPromptPositiveNegativeDialogFragmentResult(GenericPromptPositiveNegativeDialogFragment dialogFragment, String fragmentTagName)
            {
                return false;
            }
        });
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
        String message = arguments.getString(ARG_MESSAGE);
        String textPositiveButton = arguments.getString(ARG_POSITIVE_BUTTON_TEXT);
        if (textPositiveButton == null)
        {
            textPositiveButton = getString(android.R.string.ok);
        }
        String textNegativeButton = arguments.getString(ARG_NEGATIVE_BUTTON_TEXT);
        if (textNegativeButton == null)
        {
            textNegativeButton = getString(android.R.string.cancel);
        }

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(textPositiveButton, new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        onResult(dialog, Result.Positive);
                    }
                })
                .setNegativeButton(textNegativeButton, new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        onResult(dialog, Result.Negative);
                    }
                })
                .create();
        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    protected void onResult(DialogInterface dialog, Result result)
    {
        mResult = result;

        GenericPromptPositiveNegativeDialogFragment dialogFragment = GenericPromptPositiveNegativeDialogFragment.this;
        String tag = dialogFragment.getTag();

        mCallback.onGenericPromptPositiveNegativeDialogFragmentResult(dialogFragment, tag);

        dialog.dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog)
    {
        super.onCancel(dialog);
        onResult(dialog, Result.Canceled);
    }
}
