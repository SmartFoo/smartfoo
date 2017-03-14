package com.smartfoo.android.core.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatCheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;

import com.smartfoo.android.core.annotations.NonNullNonEmpty;
import com.smartfoo.android.core.app.GenericPromptPositiveNegativeDialogFragment.GenericPromptPositiveNegativeDialogFragmentCallbacks;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooPlatformUtils;

public class GenericPromptPositiveNegativeDialogFragment
        extends CallbackDialogFragment<GenericPromptPositiveNegativeDialogFragmentCallbacks>
{
    private static final String TAG = FooLog.TAG(GenericPromptPositiveNegativeDialogFragment.class);

    public interface GenericPromptPositiveNegativeDialogFragmentCallbacks
    {
        /**
         * @param dialogFragment dialogFragment
         * @return true if handled, false if not handled
         */
        boolean onGenericPromptPositiveNegativeDialogFragmentResult(
                @NonNull
                        GenericPromptPositiveNegativeDialogFragment dialogFragment);
    }

    public enum Result
    {
        Canceled,
        Positive,
        PositiveChecked,
        Negative,
    }

    private static boolean isValidResourceId(int resId)
    {
        return resId != -1 && resId != 0;
    }

    @NonNull
    public static GenericPromptPositiveNegativeDialogFragment newInstance(
            @NonNull
                    Context context,
            int title,
            int message,
            int textPositiveButton,
            int textNegativeButton)
    {
        return newInstance(context, title, message, 0, false, textPositiveButton, textNegativeButton);
    }

    @NonNull
    public static GenericPromptPositiveNegativeDialogFragment newInstance(
            @NonNull
                    Context context,
            int title,
            int message,
            int checkboxMessage,
            boolean checkboxChecked,
            int textPositiveButton,
            int textNegativeButton)
    {
        boolean showCheckboxMessage = isValidResourceId(checkboxMessage);
        return newInstance(context.getString(title),
                context.getString(message),
                showCheckboxMessage ? context.getString(checkboxMessage) : null,
                showCheckboxMessage && checkboxChecked,
                context.getString(textPositiveButton),
                context.getString(textNegativeButton));
    }

    @NonNull
    public static GenericPromptPositiveNegativeDialogFragment newInstance(String title,
                                                                          String message,
                                                                          String textPositiveButton,
                                                                          String textNegativeButton)
    {
        return newInstance(title, message, null, false, textPositiveButton, textNegativeButton);
    }

    @NonNull
    public static GenericPromptPositiveNegativeDialogFragment newInstance(String title,
                                                                          String message,
                                                                          String checkboxMessage,
                                                                          boolean checkboxChecked,
                                                                          String textPositiveButton,
                                                                          String textNegativeButton)
    {
        GenericPromptPositiveNegativeDialogFragment fragment = new GenericPromptPositiveNegativeDialogFragment();

        Bundle arguments = fragment.makeArguments(title, message, checkboxMessage, checkboxChecked, textPositiveButton, textNegativeButton);

        fragment.setArguments(arguments);

        return fragment;
    }

    protected static final String ARG_TITLE                = "ARG_TITLE";
    protected static final String ARG_MESSAGE              = "ARG_MESSAGE";
    protected static final String ARG_CHECKBOX_MESSAGE     = "ARG_CHECKBOX_MESSAGE";
    protected static final String ARG_CHECKBOX_CHECKED     = "ARG_CHECKBOX_CHECKED";
    protected static final String ARG_POSITIVE_BUTTON_TEXT = "ARG_POSITIVE_BUTTON_TEXT";
    protected static final String ARG_NEGATIVE_BUTTON_TEXT = "ARG_NEGATIVE_BUTTON_TEXT";

    @NonNull
    protected Bundle makeArguments(
            @NonNullNonEmpty
                    String title,
            @NonNullNonEmpty
                    String message,
            @NonNullNonEmpty
                    String textPositiveButton,
            @NonNullNonEmpty
                    String textNegativeButton)
    {
        return makeArguments(title, message, null, false, textPositiveButton, textNegativeButton);
    }

    @NonNull
    protected Bundle makeArguments(
            @NonNullNonEmpty
                    String title,
            @NonNullNonEmpty
                    String message,
            String checkboxMessage,
            boolean checkboxChecked,
            @NonNullNonEmpty
                    String textPositiveButton,
            @NonNullNonEmpty
                    String textNegativeButton)
    {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        arguments.putString(ARG_MESSAGE, message);
        if (checkboxMessage != null)
        {
            arguments.putString(ARG_CHECKBOX_MESSAGE, checkboxMessage);
            arguments.putBoolean(ARG_CHECKBOX_CHECKED, checkboxChecked);
        }
        arguments.putString(ARG_POSITIVE_BUTTON_TEXT, textPositiveButton);
        arguments.putString(ARG_NEGATIVE_BUTTON_TEXT, textNegativeButton);
        return arguments;
    }

    protected String  mMessage;
    protected boolean mShowCheckbox;
    protected boolean mIsChecked;
    protected Result  mResult;

    public GenericPromptPositiveNegativeDialogFragment()
    {
        super(new GenericPromptPositiveNegativeDialogFragmentCallbacks()
        {
            @Override
            public boolean onGenericPromptPositiveNegativeDialogFragmentResult(
                    @NonNull
                            GenericPromptPositiveNegativeDialogFragment dialogFragment)
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

        String checkboxMessage = arguments.getString(ARG_CHECKBOX_MESSAGE);
        mIsChecked = arguments.getBoolean(ARG_CHECKBOX_CHECKED);

        mShowCheckbox = checkboxMessage != null;

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

        Context context = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(mMessage);

        if (checkboxMessage != null)
        {
            LinearLayout root = new LinearLayout(context);

            AppCompatCheckBox checkBox = new AppCompatCheckBox(context);
            checkBox.setText(checkboxMessage);
            checkBox.setChecked(mIsChecked);
            checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
                {
                    mIsChecked = isChecked;
                }
            });
            root.addView(checkBox);

            int paddingPx = FooPlatformUtils.dip2px(context, 16.0f);
            root.setPadding(paddingPx, paddingPx, paddingPx, 0);

            builder.setView(root);
        }

        AlertDialog alertDialog = builder
                .setPositiveButton(textPositiveButton, new DialogInterface.OnClickListener()
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
        //FooLog.e(TAG, "onResult(dialog=" + dialog + ", result=" + result + ')');
        if (mResult != null)
        {
            //FooLog.e(TAG, "onResult: mResult(" + mResult + ") already set; ignoring");
            return;
        }

        switch (result)
        {
            case Positive:
                if (mShowCheckbox && mIsChecked)
                {
                    mResult = Result.PositiveChecked;
                    break;
                }
            default:
                mResult = result;
        }

        mCallback.onGenericPromptPositiveNegativeDialogFragmentResult(this);

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
