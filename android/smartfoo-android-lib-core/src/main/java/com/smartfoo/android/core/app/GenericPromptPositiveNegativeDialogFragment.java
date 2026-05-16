package com.smartfoo.android.core.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.annotations.NonNullNonEmpty;
import com.smartfoo.android.core.app.GenericPromptPositiveNegativeDialogFragment.GenericPromptPositiveNegativeDialogFragmentCallbacks;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.platform.FooRes;

/**
 * A reusable {@link androidx.fragment.app.DialogFragment} that shows a prompt with a positive
 * and a negative button, and optionally a checkbox.
 *
 * <p>Create instances via one of the {@code newInstance} factory methods. The dialog result is
 * reported through {@link GenericPromptPositiveNegativeDialogFragmentCallbacks}.
 * The result value is available via {@link #getResult()} after the dialog is dismissed.</p>
 */
public class GenericPromptPositiveNegativeDialogFragment
        extends CallbackDialogFragment<GenericPromptPositiveNegativeDialogFragmentCallbacks>
{
    private static final String TAG = FooLog.TAG(GenericPromptPositiveNegativeDialogFragment.class);

    /**
     * Callback interface delivered when the user dismisses the dialog.
     */
    public interface GenericPromptPositiveNegativeDialogFragmentCallbacks
    {
        /**
         * Called when the dialog has been dismissed with any result (positive, negative, or cancelled).
         *
         * @param dialogFragment the dialog that was dismissed; call {@link #getResult()} to read the outcome
         * @return {@code true} if the event was handled, {@code false} otherwise
         */
        boolean onGenericPromptPositiveNegativeDialogFragmentResult(@NonNull GenericPromptPositiveNegativeDialogFragment dialogFragment);
    }

    /**
     * The outcome of a {@link GenericPromptPositiveNegativeDialogFragment} interaction.
     */
    public enum Result
    {
        /** The dialog was cancelled (back press or touch-outside). */
        Canceled,
        /** The user tapped the positive button without the optional checkbox checked. */
        Positive,
        /** The user tapped the positive button while the optional checkbox was checked. */
        PositiveChecked,
        /** The user tapped the negative button. */
        Negative,
    }

    private static boolean isValidResourceId(int resId)
    {
        return resId != -1 && resId != 0;
    }

    /**
     * Creates a new instance using Android string-resource IDs, without a checkbox.
     *
     * @param context           a context used to resolve the resource IDs; must not be null
     * @param title             string resource ID for the dialog title
     * @param message           string resource ID for the dialog message
     * @param textPositiveButton string resource ID for the positive button label
     * @param textNegativeButton string resource ID for the negative button label
     * @return a configured dialog fragment, never null
     */
    @NonNull
    public static GenericPromptPositiveNegativeDialogFragment newInstance(
            @NonNull Context context,
            int title,
            int message,
            int textPositiveButton,
            int textNegativeButton)
    {
        return newInstance(context, title, message, 0, false, textPositiveButton, textNegativeButton);
    }

    /**
     * Creates a new instance using Android string-resource IDs, optionally with a checkbox.
     *
     * @param context            a context used to resolve the resource IDs; must not be null
     * @param title              string resource ID for the dialog title
     * @param message            string resource ID for the dialog message
     * @param checkboxMessage    string resource ID for the optional checkbox label;
     *                           pass {@code 0} to omit the checkbox
     * @param checkboxChecked    initial checked state of the checkbox; ignored when
     *                           {@code checkboxMessage} is {@code 0}
     * @param textPositiveButton string resource ID for the positive button label
     * @param textNegativeButton string resource ID for the negative button label
     * @return a configured dialog fragment, never null
     */
    @NonNull
    public static GenericPromptPositiveNegativeDialogFragment newInstance(
            @NonNull Context context,
            int title,
            int message,
            int checkboxMessage,
            boolean checkboxChecked,
            int textPositiveButton,
            int textNegativeButton)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        boolean showCheckboxMessage = isValidResourceId(checkboxMessage);
        return newInstance(context.getString(title),
                context.getString(message),
                showCheckboxMessage ? context.getString(checkboxMessage) : null,
                showCheckboxMessage && checkboxChecked,
                context.getString(textPositiveButton),
                context.getString(textNegativeButton));
    }

    /**
     * Creates a new instance with string arguments and default button labels ({@link android.R.string#ok}
     * / {@link android.R.string#cancel}).
     *
     * @param title   dialog title text
     * @param message dialog message text
     * @return a configured dialog fragment, never null
     */
    @NonNull
    public static GenericPromptPositiveNegativeDialogFragment newInstance(String title,
                                                                          String message)
    {
        return newInstance(title, message, null, null);
    }

    /**
     * Creates a new instance with string arguments and explicit button labels, without a checkbox.
     *
     * @param title              dialog title text
     * @param message            dialog message text
     * @param textPositiveButton positive button label, or {@code null} for the system default
     * @param textNegativeButton negative button label, or {@code null} for the system default
     * @return a configured dialog fragment, never null
     */
    @NonNull
    public static GenericPromptPositiveNegativeDialogFragment newInstance(String title,
                                                                          String message,
                                                                          String textPositiveButton,
                                                                          String textNegativeButton)
    {
        return newInstance(title, message, null, false, textPositiveButton, textNegativeButton);
    }

    /**
     * Creates a new instance with string arguments, an optional checkbox, and explicit button labels.
     *
     * @param title              dialog title text
     * @param message            dialog message text
     * @param checkboxMessage    optional checkbox label text; pass {@code null} to omit the checkbox
     * @param checkboxChecked    initial checked state of the checkbox; ignored when
     *                           {@code checkboxMessage} is {@code null}
     * @param textPositiveButton positive button label, or {@code null} for the system default
     * @param textNegativeButton negative button label, or {@code null} for the system default
     * @return a configured dialog fragment, never null
     */
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

    /**
     * Builds a {@link Bundle} of fragment arguments without a checkbox.
     *
     * @param title              dialog title text; must not be null or empty
     * @param message            dialog message text; must not be null or empty
     * @param textPositiveButton positive button label, or {@code null} for the system default
     * @param textNegativeButton negative button label, or {@code null} for the system default
     * @return a bundle suitable for passing to {@link #setArguments}, never null
     */
    @NonNull
    protected Bundle makeArguments(
            @NonNullNonEmpty String title,
            @NonNullNonEmpty String message,
            String textPositiveButton,
            String textNegativeButton)
    {
        return makeArguments(title, message, null, false, textPositiveButton, textNegativeButton);
    }

    /**
     * Builds a {@link Bundle} of fragment arguments, optionally including a checkbox entry.
     *
     * @param title              dialog title text; must not be null or empty
     * @param message            dialog message text; must not be null or empty
     * @param checkboxMessage    optional checkbox label; pass {@code null} to omit the checkbox
     * @param checkboxChecked    initial checked state; ignored when {@code checkboxMessage} is null
     * @param textPositiveButton positive button label, or {@code null} for the system default
     * @param textNegativeButton negative button label, or {@code null} for the system default
     * @return a bundle suitable for passing to {@link #setArguments}, never null
     */
    @NonNull
    protected Bundle makeArguments(
            @NonNullNonEmpty String title,
            @NonNullNonEmpty String message,
            String checkboxMessage,
            boolean checkboxChecked,
            String textPositiveButton,
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

    protected String  mTitle;
    protected String  mMessage;
    protected boolean mShowCheckbox;
    protected boolean mIsChecked;
    protected Result  mResult;

    public GenericPromptPositiveNegativeDialogFragment()
    {
        super(new GenericPromptPositiveNegativeDialogFragmentCallbacks()
        {
            @Override
            public boolean onGenericPromptPositiveNegativeDialogFragmentResult(@NonNull GenericPromptPositiveNegativeDialogFragment dialogFragment)
            {
                return false;
            }
        });
    }

    /**
     * Returns the dialog title that was passed to the factory method.
     *
     * @return the title string, or {@code null} if the dialog has not yet been created
     */
    public String getTitle()
    {
        return mTitle;
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
     * @return one of {@link Result#Canceled}, {@link Result#Positive}, {@link Result#PositiveChecked},
     *         or {@link Result#Negative}; {@code null} if the dialog has not been dismissed yet
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
        mTitle = arguments.getString(ARG_TITLE);
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
                .setTitle(mTitle)
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

            int paddingPx = FooRes.dip2px(context, 16.0f);
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

    /**
     * Finalises the dialog result, notifies the callback, and dismisses the dialog.
     * Calls after the first are silently ignored so that {@code onCancel}/{@code onDismiss}
     * cannot overwrite a result that was already set by a button click.
     *
     * <p>If {@code result} is {@link Result#Positive} and the optional checkbox is checked,
     * the stored result is promoted to {@link Result#PositiveChecked}.</p>
     *
     * @param dialog the dialog interface to dismiss; may be {@code null} (e.g. called from
     *               {@code onDismiss} where the dialog is already going away)
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
