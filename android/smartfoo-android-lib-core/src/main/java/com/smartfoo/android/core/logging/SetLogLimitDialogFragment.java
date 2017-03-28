package com.smartfoo.android.core.logging;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.smartfoo.android.core.R;

public class SetLogLimitDialogFragment
        extends DialogFragment
{
    public interface SetLogLimitDialogConfiguration
    {
    }

    public interface SetLogLimitDialogFragmentCallbacks
    {
        void onSetLogLimit(int logLimitKb, int logEmailLimitKb);
    }

    private static final String ARG_LOG_LIMIT_KB_DEFAULT = "ARG_LOG_LIMIT_KB_DEFAULT";
    private static final String ARG_LOG_LIMIT_KB         = "ARG_LOG_LIMIT_KB";
    private static final String ARG_LOG_EMAIL_LIMIT_KB   = "ARG_LOG_EMAIL_LIMIT_KB";

    public static SetLogLimitDialogFragment newInstance(int logLimitKbDefault, int logLimitKb, int logEmailLimitKb)
    {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_LOG_LIMIT_KB_DEFAULT, logLimitKbDefault);
        arguments.putInt(ARG_LOG_LIMIT_KB, logLimitKb);
        arguments.putInt(ARG_LOG_EMAIL_LIMIT_KB, logEmailLimitKb);

        SetLogLimitDialogFragment dialogFragment = new SetLogLimitDialogFragment();
        dialogFragment.setArguments(arguments);
        return dialogFragment;
    }

    private static final SetLogLimitDialogFragmentCallbacks sDummyCallbacks = new SetLogLimitDialogFragmentCallbacks()
    {
        @Override
        public void onSetLogLimit(int logLimitKb, int logEmailLimitKb)
        {
        }
    };

    private SetLogLimitDialogFragmentCallbacks mCallbacks = sDummyCallbacks;

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        mCallbacks = (SetLogLimitDialogFragmentCallbacks) getActivity();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle arguments = getArguments();
        final int logLimitKbDefault = arguments.getInt(ARG_LOG_LIMIT_KB_DEFAULT);
        final int logLimitKb = arguments.getInt(ARG_LOG_LIMIT_KB);
        final int logEmailLimitKb = arguments.getInt(ARG_LOG_EMAIL_LIMIT_KB);

        Context context = getActivity();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.dialog_set_log_limit, null, false);

        final TextView textViewLogLimit = (TextView) view.findViewById(R.id.textViewLogLimitValue);
        final SeekBar seekBarLogLimit = (SeekBar) view.findViewById(R.id.seekBarLogLimit);

        final TextView textViewLogEmailLimit = (TextView) view.findViewById(R.id.textViewLogEmailLimitValue);
        final SeekBar seekBarLogEmailLimit = (SeekBar) view.findViewById(R.id.seekBarLogEmailLimit);

        final Button buttonReset = (Button) view.findViewById(R.id.buttonReset);

        seekBarLogLimit.setMax(1024);
        seekBarLogLimit.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                String text = progress == 0 ? "Unlimited" : "" + progress + "KB";
                textViewLogLimit.setText(text);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
            }
        });
        seekBarLogLimit.setProgress(logLimitKb);

        seekBarLogEmailLimit.setMax(128);
        seekBarLogEmailLimit.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                String text = progress == 0 ? "Unlimited" : "" + progress + "KB";
                textViewLogEmailLimit.setText(text);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
            }
        });
        seekBarLogEmailLimit.setProgress(logLimitKb);

        buttonReset.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                seekBarLogLimit.setProgress(logLimitKbDefault);
                seekBarLogEmailLimit.setProgress(logEmailLimitKb);
            }
        });

        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.activity_debug_action_set_log_limit)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        int logLimitKb = seekBarLogLimit.getProgress();
                        int logEmailLimitKb = seekBarLogEmailLimit.getProgress();

                        mCallbacks.onSetLogLimit(logLimitKb, logEmailLimitKb);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        alertDialog.setCanceledOnTouchOutside(false);

        return alertDialog;
    }
}
