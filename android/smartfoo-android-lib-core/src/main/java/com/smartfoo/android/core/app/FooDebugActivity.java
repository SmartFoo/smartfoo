package com.smartfoo.android.core.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smartfoo.android.core.FooRun;
import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.R;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.logging.FooLogCat;
import com.smartfoo.android.core.logging.FooLogCat.LogProcessCallbacks;
import com.smartfoo.android.core.logging.FooLogFilePrinter;
import com.smartfoo.android.core.logging.SetLogLimitDialogFragment;
import com.smartfoo.android.core.logging.SetLogLimitDialogFragment.SetLogLimitDialogFragmentCallbacks;
import com.smartfoo.android.core.platform.FooPlatformUtils;
import com.smartfoo.android.core.platform.FooRes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/*
 * TODO:(pv) Fix the actionbar search menu:
 *  https://github.com/codepath/android_guides/wiki/Extended-ActionBar-Guide#adding-searchview-to-actionbar
 * TODO:(pv) Update w/ RecyclerView and remove deprecated APIs (setSupportProgressBarIndeterminateVisibility, etc)
 * TODO:(pv) Stop loading log once the first "I FooLogCat: T27810 +load()" is encountered
 * TODO:(pv) Search backward and forward in log
 * TODO:(pv) Ignore everything up the first PID?
 */
public class FooDebugActivity
        extends AppCompatActivity//PbPermisoActivity //
        implements OnQueryTextListener, //
        SetLogLimitDialogFragmentCallbacks
{
    private static final String TAG = FooLog.TAG(FooDebugActivity.class);

    public static Bundle makeExtras(Bundle extras,
                                    String username)
    {
        return makeExtras(extras, username, null, null, android.os.Process.myPid());
    }

    public static Bundle makeExtras(Bundle extras,
                                    String username,
                                    String message,
                                    String logRaw,
                                    int logPid)
    {
        if (extras == null)
        {
            extras = new Bundle();
        }

        if (!FooString.isNullOrEmpty(username))
        {
            extras.putString(EXTRA_USER_NAME, username);
        }
        if (!FooString.isNullOrEmpty(message))
        {
            extras.putString(EXTRA_MESSAGE, message);
        }
        if (!FooString.isNullOrEmpty(logRaw))
        {
            extras.putString(EXTRA_LOG_RAW, logRaw);
        }
        if (logPid != -1)
        {
            extras.putInt(EXTRA_LOG_PID, logPid);
        }

        return extras;
    }

    private static final String EXTRA_USER_NAME = "EXTRA_USER_NAME";
    private static final String EXTRA_MESSAGE   = "EXTRA_MESSAGE";
    private static final String EXTRA_LOG_RAW   = "EXTRA_LOG_RAW";
    private static final String EXTRA_LOG_PID   = "EXTRA_LOG_PID";

    private static final String FRAGMENT_DIALOG_SET_LOG_LIMIT = "FRAGMENT_DIALOG_SET_LOG_LIMIT";

    private static final int REQUEST_SHARE = 1;

    /**
     * Quickly fake some log lines, instead of reading the actual log.<br>
     * Set to <= 0 to disable.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private static int FAKE_LOG_LINES = 0;

    private static       int    EMAIL_MAX_KILOBYTES_DEFAULT = FooLogCat.EMAIL_MAX_BYTES_DEFAULT;
    private static       int    ACCUMULATOR_MAX             = FooLogCat.ACCUMULATOR_MAX;
    private static final String LINEFEED                    = FooLogCat.LINEFEED;
    private static final String TYPEFACE_FAMILY             = FooLogCat.TYPEFACE_FAMILY;
    private static final float  TYPEFACE_SIZE               = FooLogCat.TYPEFACE_SIZE;

    private String mUserName;

    private FooDebugConfiguration mDebugConfiguration;

    private String mHeader;

    /**
     * Raw log file that has not been parsed by LogReaderTask in to LogAdapter
     */
    private String mLogRaw;

    private Boolean mIsSharedLogFileCompressed;

    private ViewGroup mGroupProgress;
    private TextView  mTextProgressTitle;

    private RecyclerView        mRecyclerView;
    private LinearLayoutManager mRecyclerLayoutManager;
    private LogAdapter          mRecyclerAdapter;

    private int mColorSelected;
    private int mColorAssert;
    private int mColorError;
    private int mColorWarn;
    private int mColorInfo;
    private int mColorDebug;
    private int mColorVerbose;
    private int mColorOther;

    private int mLogLimitKb;
    private int mLogEmailLimitKb;

    public void showProgressIndicator(String text)
    {
        if (FooString.isNullOrEmpty(text))
        {
            mGroupProgress.setVisibility(View.GONE);
        }
        else
        {
            mTextProgressTitle.setText(text);
            mGroupProgress.setVisibility(View.VISIBLE);
        }
    }

    /*
    @Override
    public String getPermissionRequiredToText(int requestCode, String permissionDenied)
    {
        String permissionRationale = null;

        switch (permissionDenied)
        {
            case Manifest.permission.READ_PHONE_STATE:
                permissionRationale = getString(R.string.phone_state_permission_rationale);
                break;
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                permissionRationale = getString(R.string.activity_debug_external_storage_permission_rationale);
                break;
        }

        return permissionRationale;
    }

    @Override
    public boolean onRequestPermissionGranted(int requestCode, String permissionGranted)
    {
        switch (permissionGranted)
        {
            case Manifest.permission.READ_PHONE_STATE:
            {
                loadLog(true);
                break;
            }
        }

        return false;
    }
    */

    public static FooDebugApplication getFooDebugApplication(@NonNull Context context)
    {
        FooRun.throwIllegalArgumentExceptionIfNull(context, "context");
        Application applicationContext = (Application) context.getApplicationContext();
        if (!(applicationContext instanceof FooDebugApplication))
        {
            throw new IllegalStateException("Application context must implement FooDebugApplication");
        }
        return (FooDebugApplication) applicationContext;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null)
        {
            mUserName = intent.getStringExtra(EXTRA_USER_NAME);
        }

        FooDebugApplication application = getFooDebugApplication(this);
        mDebugConfiguration = application.getFooDebugConfiguration();

        setContentView(R.layout.activity_debug);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null)
        {
            setSupportActionBar(toolbar);

            ProgressBar toolbarProgressBar = (ProgressBar) findViewById(R.id.toolbar_progress_bar);
            toolbarProgressBar.setVisibility(View.GONE);

            if (NavUtils.getParentActivityName(this) != null)
            {
                ActionBar actionbar = getSupportActionBar();
                if (actionbar != null)
                {
                    actionbar.setHomeButtonEnabled(true);
                    actionbar.setDisplayHomeAsUpEnabled(true);
                }
            }
        }

        //
        // NOTE:(pv) For some reason setting visibility to gone in the layout isn't working on some devices.
        //  Forcing it to default to GONE here.
        //
        mGroupProgress = (ViewGroup) findViewById(R.id.groupProgress);
        mTextProgressTitle = (TextView) findViewById(R.id.textProgressTitle);

        if (savedInstanceState == null)
        {
            showProgressIndicator(null);

            mHeader = null;
            mLogRaw = null;
            mLogLimitKb = mDebugConfiguration.getDebugLogLimitKb(EMAIL_MAX_KILOBYTES_DEFAULT);
            mLogEmailLimitKb = mDebugConfiguration.getDebugLogEmailLimitKb(EMAIL_MAX_KILOBYTES_DEFAULT);
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mRecyclerLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mRecyclerLayoutManager);

        /*
        // TODO:(pv) Figure out why this fast scroller jumps BACKWARDS so much; fix and then uncomment…
        VerticalRecyclerViewFastScroller fastScroller = (VerticalRecyclerViewFastScroller) findViewById(R.id.fast_scroller);
        if (fastScroller != null)
        {
            mRecyclerView.setOnScrollListener(fastScroller.getOnScrollListener());
            fastScroller.setRecyclerView(mRecyclerView);
        }
        */

        Resources resources = getResources();
        //noinspection deprecation
        mColorSelected = FooRes.getColor(resources, R.color.log_selected);
        //noinspection deprecation
        mColorAssert = FooRes.getColor(resources, R.color.log_level_assert);
        //noinspection deprecation
        mColorError = FooRes.getColor(resources, R.color.log_level_error);
        //noinspection deprecation
        mColorWarn = FooRes.getColor(resources, R.color.log_level_warn);
        //noinspection deprecation
        mColorInfo = FooRes.getColor(resources, R.color.log_level_info);
        //noinspection deprecation
        mColorDebug = FooRes.getColor(resources, R.color.log_level_debug);
        //noinspection deprecation
        mColorVerbose = FooRes.getColor(resources, R.color.log_level_verbose);
        //noinspection deprecation
        mColorOther = FooRes.getColor(resources, R.color.log_level_other);

        loadLog(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("mHeader", mHeader);
        outState.putString("mLogRaw", mLogRaw);
        if (mIsSharedLogFileCompressed != null)
        {
            outState.putBoolean("mIsSharedLogFileCompressed", mIsSharedLogFileCompressed);
        }
        outState.putInt("mLogLimitKb", mLogLimitKb);
        outState.putInt("mLogEmailLimitKb", mLogEmailLimitKb);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        mHeader = savedInstanceState.getString("mHeader");
        mLogRaw = savedInstanceState.getString("mLogRaw");
        mIsSharedLogFileCompressed = savedInstanceState.getBoolean("mIsSharedLogFileCompressed");
        mLogLimitKb = savedInstanceState.getInt("mLogLimitKb");
        mLogEmailLimitKb = savedInstanceState.getInt("mLogEmailLimitKb");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_debug, menu);

        MenuItem searchItem = menu.findItem(R.id.action_debug_find);
        if (searchItem != null)
        {
            SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            if (searchView != null)
            {
                searchView.setOnQueryTextListener(this);
            }
        }

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        boolean isFixedLog = false;
        if (extras != null)
        {
            isFixedLog = (extras.containsKey(EXTRA_MESSAGE) || extras.containsKey(EXTRA_LOG_RAW));
        }

        MenuItem clear = menu.findItem(R.id.action_debug_clear);
        if (clear != null)
        {
            clear.setVisible(!isFixedLog);
        }

        MenuItem refresh = menu.findItem(R.id.action_debug_refresh);
        if (refresh != null)
        {
            refresh.setVisible(!isFixedLog);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        MenuItem logFile = menu.findItem(R.id.action_debug_log_file);
        if (logFile != null)
        {
            boolean isDebugToFileEnabled = mDebugConfiguration.getDebugToFileEnabled();
            int resId = isDebugToFileEnabled ? R.string.activity_debug_action_log_file_disable : R.string.activity_debug_action_log_file_enable;
            logFile.setTitle(resId);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        //
        // NOTE:(pv) Must be an "if" and not a "switch" statement if this project is a Library:
        // http://tools.android.com/tips/non-constant-fields
        // http://tools.android.com/recent/switchstatementconversion
        // A switch statement will cause a "case expressions must be constant expressions" compiler error.
        // For compatibility reasons, leave it this way even if this project is not a Library.
        //
        int itemId = item.getItemId();
        if (itemId == android.R.id.home)
        {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            //NavUtils.navigateUpFromSameTask(this);
            finish();
            return true;
        }
        else if (itemId == R.id.action_debug_find)
        {
            SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
            if (searchView != null)
            {
                searchView.setIconified(false);
            }
            return true;
        }
        else if (itemId == R.id.action_debug_share)
        {
            shareLog();
            return true;
        }
        else if (itemId == R.id.action_debug_clear)
        {
            FooLog.clear();
            FooLogFilePrinter.deleteLogFile(this);
            loadLog(true);
            return true;
        }
        else if (itemId == R.id.action_debug_refresh)
        {
            loadLog(true);
            return true;
        }
        else if (itemId == R.id.action_debug_set_log_limit)
        {
            SetLogLimitDialogFragment dialogFragment = SetLogLimitDialogFragment.newInstance(EMAIL_MAX_KILOBYTES_DEFAULT, mLogLimitKb, mLogEmailLimitKb);
            dialogFragment.show(getFragmentManager(), FRAGMENT_DIALOG_SET_LOG_LIMIT);
            return true;
        }
        else if (itemId == R.id.action_debug_log_file)
        {
            boolean isDebugToFileEnabled = mDebugConfiguration.getDebugToFileEnabled();
            setDebugToFileEnabled(!isDebugToFileEnabled);
            return true;
        }
        // TODO:(pv) Enable/Disable Scan Logging
        else
        {
            return super.onOptionsItemSelected(item);
        }
    }

    private void setDebugToFileEnabled(boolean enabled)
    {
        /*
        if (enabled)
        {
            ResultSet resultSet = PbPermiso.checkPermissions(this, FooLogFilePrinter.REQUIRED_PERMISSIONS);
            if (!resultSet.areAllPermissionsGranted())
            {
                mPlatformManager.showToastLong("Storage Permissions denied; Logging to file is disabled.", false);
                return;
            }
        }
        */

        mDebugConfiguration.setDebugToFileEnabled(enabled);

        FooPlatformUtils.toastLong(this, "Logging to file is " + (enabled ? "enabled" : "disabled") + ".");

        invalidateOptionsMenu();
    }

    private void loadLog(final boolean reset)
    {
        /*
        PbPermiso.getInstance().requestPermissions(new IOnPermissionResult<Void>()
        {
            @Override
            public Void onPermissionResult(ResultSet resultSet)
            {
                loadLogInternal(reset);
                return null;
            }

            @Override
            public String getPermissionRequiredToText(String permission)
            {
                String permissionRationale = null;

                switch (permission)
                {
                    case Manifest.permission.READ_PHONE_STATE:
                        permissionRationale = getString(R.string.activity_debug_phone_state_permission_rationale);
                        break;
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        permissionRationale = getString(R.string.activity_debug_external_storage_permission_rationale);
                        break;
                }

                return permissionRationale;
            }
        }, Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        */
        loadLogInternal(reset);
    }

    private void loadLogInternal(boolean reset)
    {
        if (reset)
        {
            mHeader = null;
            mLogRaw = null;
        }

        if (mHeader == null)
        {
            StringBuilder sb = new StringBuilder();

            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            if (extras != null)
            {
                String message = extras.getString(EXTRA_MESSAGE);
                if (!FooString.isNullOrEmpty(message))
                {
                    sb.append(message).append(LINEFEED).append(LINEFEED);
                }
            }

            LinkedHashMap<String, String> platformInfoExtras = new LinkedHashMap<>();
            if (!FooString.isNullOrEmpty(mUserName))
            {
                platformInfoExtras.put("Username", FooString.quote(mUserName));
            }

            String platformInfo = FooPlatformUtils.getPlatformInfoString(this, platformInfoExtras);
            sb.append(platformInfo);

            mHeader = sb.toString();
        }

        mRecyclerAdapter = new LogAdapter(this, mColorSelected, mColorOther);
        mRecyclerView.setAdapter(mRecyclerAdapter);

        final long start = System.currentTimeMillis();
        final int logLimitBytes = mLogLimitKb * 1024;

        showProgressIndicator("Loading Log…");
        LogReaderTask logReaderTask = new LogReaderTask(this,
                logLimitBytes,
                mColorAssert,
                mColorError,
                mColorWarn,
                mColorInfo,
                mColorDebug,
                mColorVerbose,
                mColorOther,
                new LogReaderTask.LogReaderTaskListener()
                {
                    @Override
                    public void onLogRawLoaded(String logRaw)
                    {
                        mLogRaw = logRaw;
                    }

                    @Override
                    public void onLogLines(List<Spanned> logLines)
                    {
                        mRecyclerAdapter.addLogLines(logLines);
                    }

                    @Override
                    public void onLogEnd()
                    {
                        SpannableString header =
                                FooString.newSpannableString(mHeader, mColorOther, -1,
                                        Typeface.BOLD, TYPEFACE_FAMILY, TYPEFACE_SIZE);
                        mRecyclerAdapter.setHeaderAndMaxLogLength(header, logLimitBytes);

                        long stop = System.currentTimeMillis();
                        long elapsed = stop - start;

                        int length = mRecyclerAdapter.getLogLength();

                        FooPlatformUtils.toastLong(FooDebugActivity.this,
                                "onLogEnd: " + elapsed + "ms, " + length + " bytes");

                        showProgressIndicator(null);
                    }
                });
        logReaderTask.execute(mLogRaw);
    }

    private void shareLog()
    {
        final boolean isDebugToFileEnabled = mDebugConfiguration.getDebugToFileEnabled();
        /*
        if (isDebugToFileEnabled)
        {
            ResultSet resultSet = PbPermiso.checkPermissions(this, FooLogFilePrinter.REQUIRED_PERMISSIONS);
            if (!resultSet.areAllPermissionsGranted())
            {
                return;
            }
        }
        */

        //
        // Semi-slow on large texts, even on a quad-core...but it *IS* colorized!
        //
        new AsyncTask<Void, Integer, Intent>()
        {
            //
            // WARNING: Too large of an email will cause the below Intent.createChooser to throw android.os.TransactionTooLargeException
            //
            final int logEmailLimitBytes = mLogEmailLimitKb * 1024;

            @Override
            protected void onPreExecute()
            {
                showProgressIndicator("Sharing Log…");
            }

            @Override
            protected void onProgressUpdate(Integer... progress)
            {
                // TODO:(pv) Show more visible "Processing…" progress dialog (may be indeterminate)?
            }

            @Override
            protected Intent doInBackground(Void... params)
            {
                // TODO:(pv) Find way to make SOLID background color w/ no visible gap between lines

                // TODO:(pv) quick/efficient Spanned to pdf w/ colorized text on SOLID dark background
                //  http://developer.android.com/reference/android/graphics/pdf/PdfDocument.html (API >= 19)
                //  http://developer.android.com/reference/android/graphics/pdf/PdfDocument.Page.html
                //  http://developer.android.com/reference/android/graphics/Canvas.html
                //  http://developer.android.com/reference/android/graphics/Canvas.html#drawText(java.lang.String, float, float, android.graphics.Paint)

                SpannableStringBuilder emailMessage = new SpannableStringBuilder();

                //noinspection UnnecessaryLocalVariable
                boolean headerOnly = isDebugToFileEnabled;
                int maxTextLength = logEmailLimitBytes;

                List<Spanned> items = mRecyclerAdapter.getItemsCopy();
                int positionHightlighted = mRecyclerAdapter.getPositionHighlighted();

                if (maxTextLength > 0)
                {
                    //
                    // Walk items backwards until maxTextLength is reached,
                    // but always include items 0 [header] and 0 [demarkator]
                    //

                    int linefeedLength = LINEFEED.length();
                    int headerLength = 0;

                    Spanned item;

                    item = items.get(0); // header
                    headerLength += item.length();
                    emailMessage.append(item);
                    headerLength += linefeedLength;
                    emailMessage.append(LINEFEED);

                    if (!headerOnly)
                    {
                        int itemCount = items.size();

                        item = items.get(1); // demarkator
                        headerLength += item.length();
                        emailMessage.append(item);
                        headerLength += linefeedLength;
                        emailMessage.append(LINEFEED);

                        int position = (positionHightlighted != -1) ? positionHightlighted : itemCount;

                        while (position > 2) // first line after the demarkator
                        {
                            // TODO:(pv) publishProgress();

                            //
                            // NOTE:(pv) We are going iterating and inserting in reverse!
                            //

                            item = items.get(--position);

                            if (emailMessage.length() + item.length() + linefeedLength > maxTextLength)
                            {
                                break;
                            }

                            emailMessage.insert(headerLength, LINEFEED);
                            emailMessage.insert(headerLength, item);
                        }
                    }
                }
                else
                {
                    if (headerOnly)
                    {
                        Spanned item;

                        item = items.get(0); // header
                        emailMessage.append(item);
                        emailMessage.append(LINEFEED);
                    }
                    else
                    {
                        int itemCount = items.size();

                        for (int i = 0; i < itemCount; i++)
                        {
                            // TODO:(pv) publishProgress();

                            Spanned item = items.get(i);
                            emailMessage.append(item).append(LINEFEED);

                            if (positionHightlighted != -1 && i > positionHightlighted)
                            {
                                break;
                            }
                        }
                    }
                }

                FooLog.d(TAG, "shareLog: emailMessage.length()=" + emailMessage.length());

                Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));

                String subject = String.format(getString(R.string.activity_debug_email_subject_formatted), getString(R.string.app_name));
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);

                intent.putExtra(Intent.EXTRA_TEXT, emailMessage);
                intent.setType("text/html");

                mIsSharedLogFileCompressed = null;

                if (isDebugToFileEnabled)
                {
                    FooLogFilePrinter logFileWriter = FooLogFilePrinter.getInstance();

                    File logFile;

                    try
                    {
                        logFile = logFileWriter.getCompressedLogFile(true);
                        mIsSharedLogFileCompressed = true;
                    }
                    catch (IOException e1)
                    {
                        logFile = logFileWriter.getUncompressedLogFile();
                    }

                    Uri logFileUri = Uri.fromFile(logFile);

                    intent.putExtra(Intent.EXTRA_STREAM, logFileUri);
                }

                String title = String.format(getString(R.string.activity_debug_send_title_formatted), subject);

                //noinspection UnnecessaryLocalVariable
                Intent chooser = Intent.createChooser(intent, title);

                return chooser;
            }

            @Override
            protected void onPostExecute(Intent chooser)
            {
                showProgressIndicator(null);
                startActivityForResult(chooser, REQUEST_SHARE);
            }
        }.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case REQUEST_SHARE:
            {
                if (mIsSharedLogFileCompressed != null && mIsSharedLogFileCompressed)
                {
                    mIsSharedLogFileCompressed = null;

                    try
                    {
                        File logFile = FooLogFilePrinter.getInstance().getCompressedLogFile(false);
                        //noinspection ResultOfMethodCallIgnored
                        logFile.delete();
                    }
                    catch (IOException e)
                    {
                        // ignore
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onSetLogLimit(int logLimitKb, int logEmailLimitKb)
    {
        mDebugConfiguration.setDebugLogLimitKb(logLimitKb);
        mDebugConfiguration.setDebugLogEmailLimitKb(logEmailLimitKb);

        mLogLimitKb = logLimitKb;
        mLogEmailLimitKb = logEmailLimitKb;

        loadLog(true);
    }

    private static class LogViewHolder
            extends RecyclerView.ViewHolder
    {
        private final TextView mTextView;

        private LogViewHolder(View itemView)
        {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.textViewLogLine);
        }

        private void onBind(Spanned text, int backgroundColor,
                            OnLongClickListener itemViewOnLongClickListener)
        {
            itemView.setOnLongClickListener(itemViewOnLongClickListener);
            itemView.setTag(this);

            mTextView.setText(text);
            mTextView.setBackgroundColor(backgroundColor);
        }
    }

    private static class LogAdapter
            extends RecyclerView.Adapter<LogViewHolder>
            implements OnLongClickListener
    {
        private final LayoutInflater         mLayoutInflater;
        private final List<Spanned>          mItems;
        private final SpannableStringBuilder mHeader;
        private final int                    mColorSelected;
        private final int                    mColorOther;

        private int mItemsTextLength;
        private int mPositionHighlighted;

        private LogAdapter(Context context, int colorSelected, int colorOther)
        {
            mLayoutInflater = LayoutInflater.from(context);
            mItems = new ArrayList<>();
            mHeader = new SpannableStringBuilder();
            mColorSelected = colorSelected;
            mColorOther = colorOther;
            clear(false);
        }

        private void clear(boolean clearItems)
        {
            synchronized (mItems)
            {
                setPositionHighlighted(-1);

                mHeader.clear();

                if (clearItems)
                {
                    mItems.clear();
                    mItemsTextLength = 0;
                    notifyDataSetChanged();
                }
            }
        }

        private int getLogLength()
        {
            return mItemsTextLength;
        }

        private int findFirstPosition(int offset, String newText)
        {
            newText = newText.toLowerCase(Locale.getDefault());

            synchronized (mItems)
            {
                String itemText;
                Iterator<Spanned> iterator = mItems.listIterator(offset);
                while (iterator.hasNext())
                {
                    itemText = iterator.next().toString().toLowerCase(Locale.getDefault());
                    if (itemText.contains(newText))
                    {
                        return offset;
                    }
                    offset++;
                }
            }

            return -1;
        }

        private int getPositionHighlighted()
        {
            return mPositionHighlighted;
        }

        private void setPositionHighlighted(int position)
        {
            int oldPositionHighlighted = mPositionHighlighted;

            if (position == oldPositionHighlighted)
            {
                // Toggle the highlight
                position = -1;
            }

            mPositionHighlighted = position;

            if (position != -1)
            {
                notifyItemChanged(position);
            }

            if (oldPositionHighlighted != -1)
            {
                notifyItemChanged(oldPositionHighlighted);
            }
        }

        @Override
        public LogViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = mLayoutInflater.inflate(R.layout.activity_debug_list_item, parent, false);
            return new LogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(LogViewHolder holder, int position)
        {
            Spanned text = getItemByIndex(position);
            int backgroundColor = (position == mPositionHighlighted) ? mColorSelected : Color.TRANSPARENT;
            holder.onBind(text, backgroundColor, this);
        }

        @Override
        public int getItemCount()
        {
            synchronized (mItems)
            {
                return mItems.size();
            }
        }

        private List<Spanned> getItemsCopy()
        {
            return new ArrayList<>(mItems);
        }

        private Spanned getItemByIndex(int position)
        {
            synchronized (mItems)
            {
                return mItems.get(position);
            }
        }

        /**
         * @param header       header
         * @param maxLogLength Set to <= 0 to disable.
         */
        private void setHeaderAndMaxLogLength(Spanned header, int maxLogLength)
        {
            final Spannable demarkator =
                    FooString.newSpannableString(FooLogCat.HEADER_DEV_LOG_MAIN2, mColorOther, -1,
                            Typeface.BOLD, TYPEFACE_FAMILY, TYPEFACE_SIZE);

            synchronized (mItems)
            {
                clear(false);

                mHeader //
                        .append(header).append(LINEFEED) //
                        .append(demarkator).append(LINEFEED);

                //
                // NOTE: mHeader is mItem[0], demarkator is mItem[1], and first log line is mItem[2]
                //
                mItems.add(0, header);
                mItems.add(1, demarkator);

                if (maxLogLength > 0)
                {
                    //
                    // Remove the first log line until the below generated mText would be <= maxTextLength
                    //
                    int headerLength = mHeader.length() + demarkator.length();

                    Spanned logLine;
                    while (headerLength + mItemsTextLength > maxLogLength)
                    {
                        logLine = mItems.remove(2); // first line after the demarkator
                        mItemsTextLength -= logLine.length() + 1; // + 1 for LINEFEED
                    }
                }

                notifyDataSetChanged();
            }
        }

        /**
         * @param logLines to insert; the list is reset/cleared if any "null" value is encountered
         */
        private void addLogLines(List<Spanned> logLines)
        {
            synchronized (mItems)
            {
                for (Spanned logLine : logLines)
                {
                    if (logLine == null)
                    {
                        // Reset the list
                        clear(true);
                    }
                    else
                    {
                        int oldLastLine = mItems.size();

                        mItems.add(logLine);
                        mItemsTextLength += logLine.length() + 1; // + 1 for LINEFEED

                        int newLastLine = mItems.size();

                        notifyItemRangeChanged(oldLastLine, newLastLine);
                    }
                }
            }
        }

        @Override
        public boolean onLongClick(View v)
        {
            LogViewHolder holder = (LogViewHolder) v.getTag();
            int position = holder.getAdapterPosition();

            //Context context = v.getContext();
            //PbPlatformUtils.toastLong(context, "onLongClick row #" + position);

            setPositionHighlighted(position);

            return true;
        }
    }

    private static class LogReaderTask
            extends AsyncTask<String, List<Spanned>, Void>
    {
        private interface LogReaderTaskListener
        {
            void onLogRawLoaded(String logRaw);

            void onLogLines(List<Spanned> logLines);

            void onLogEnd();
        }

        private final Activity              mActivity;
        private final int                   mLogLimitBytes;
        private final LogReaderTaskListener mListener;
        private final int                   mColorAssert;
        private final int                   mColorError;
        private final int                   mColorWarn;
        private final int                   mColorInfo;
        private final int                   mColorDebug;
        private final int                   mColorVerbose;
        private final int                   mColorOther;

        private LogReaderTask(Activity activity,
                              int logLimitBytes,
                              int colorAssert,
                              int colorError,
                              int colorWarn,
                              int colorInfo,
                              int colorDebug,
                              int colorVerbose,
                              int colorOther,
                              LogReaderTaskListener listener)
        {
            mActivity = activity;
            mLogLimitBytes = logLimitBytes;
            mColorAssert = colorAssert;
            mColorError = colorError;
            mColorWarn = colorWarn;
            mColorInfo = colorInfo;
            mColorDebug = colorDebug;
            mColorVerbose = colorVerbose;
            mColorOther = colorOther;
            mListener = listener;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            mListener.onLogEnd();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onProgressUpdate(List<Spanned>... progress)
        {
            for (List<Spanned> accumulator : progress)
            {
                mListener.onLogLines(accumulator);
            }
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected Void doInBackground(String... params)
        {
            String logRaw = params[0];

            int pid = FooLogCat.getMyPid();

            if (logRaw == null)
            {
                if (FAKE_LOG_LINES > 0)
                {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= FAKE_LOG_LINES; i++)
                    {
                        sb.append("00-00 00:00:00.000 123 456 I TAG: #").append(i).append(LINEFEED);
                    }
                    logRaw = sb.toString();
                }
                else
                {
                    Intent intent = mActivity.getIntent();
                    Bundle extras = intent.getExtras();
                    if (extras != null && extras.containsKey(EXTRA_LOG_RAW))
                    {
                        logRaw = extras.getString(EXTRA_LOG_RAW);

                        if (extras.containsKey(EXTRA_LOG_PID))
                        {
                            pid = extras.getInt(EXTRA_LOG_PID);
                        }
                    }
                    else
                    {
                        // TODO:(pv) load log until *LAST* terminator is found (this is a bit more complicated than it sounds)
                        // "I FooLogCat: TXXXXX +load()"
                        //String terminator = myPid + " " + myTid + " I " + FooLog.TAG(FooLogCat.class) + " T" + myTid + " +load()";
                        logRaw = FooLogCat.load(mLogLimitBytes);//, terminator);
                    }
                }

                mListener.onLogRawLoaded(logRaw);
            }

            FooLogCat.process(pid, logRaw, new LogProcessCallbacks()
            {
                @Override
                public int getColorAssert()
                {
                    return mColorAssert;
                }

                @Override
                public int getColorError()
                {
                    return mColorError;
                }

                @Override
                public int getColorWarn()
                {
                    return mColorWarn;
                }

                @Override
                public int getColorInfo()
                {
                    return mColorInfo;
                }

                @Override
                public int getColorDebug()
                {
                    return mColorDebug;
                }

                @Override
                public int getColorVerbose()
                {
                    return mColorVerbose;
                }

                @Override
                public int getColorOther()
                {
                    return mColorOther;
                }

                @Override
                public String getTypefaceFamily()
                {
                    return TYPEFACE_FAMILY;
                }

                @Override
                public float getTypefaceSize()
                {
                    return TYPEFACE_SIZE;
                }

                @Override
                public int getAccumulatorMax()
                {
                    return ACCUMULATOR_MAX;
                }

                @Override
                public void onLogLines(List<Spanned> logLines)
                {
                    //noinspection unchecked
                    publishProgress(logLines);
                }
            });

            return null;
        }
    }

    private int mPositionFound = -1;

    @Override
    public boolean onQueryTextSubmit(String query)
    {
        findPosition(mPositionFound + 1, query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText)
    {
        // Called when the action bar search text has changed.
        // Since this is a simple array adapter, we can just have it do the filtering.
        if (!FooString.isNullOrEmpty(newText))
        {
            int offset;
            int positionSelected = mRecyclerAdapter.getPositionHighlighted();
            if (positionSelected != -1)
            {
                offset = positionSelected;
            }
            else
            {
                offset = mRecyclerLayoutManager.findFirstVisibleItemPosition();
            }
            findPosition(offset, newText);
        }
        return true;
    }

    private void findPosition(int offset, String query)
    {
        if (offset < 0 || offset >= mRecyclerAdapter.getItemCount())
        {
            offset = 0;
        }

        mPositionFound = mRecyclerAdapter.findFirstPosition(offset, query);
        if (mPositionFound != -1)
        {
            mRecyclerAdapter.setPositionHighlighted(mPositionFound);
            mRecyclerView.scrollToPosition(mPositionFound);
        }
        else
        {
            FooPlatformUtils.toastLong(this, "Reached end of log");
        }
    }
}