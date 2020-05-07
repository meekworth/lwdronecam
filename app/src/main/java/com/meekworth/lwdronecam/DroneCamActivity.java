package com.meekworth.lwdronecam;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class DroneCamActivity extends AppCompatActivity {
    private static final String TAG = "LWDroneCam/DroneCamActivity";

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private TextureView mCamView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mCamView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private Button mStreamButton;
    private Button mRecordButton;
    private DroneCam mDroneCam;
    private final AtomicBoolean mResumeStreaming = new AtomicBoolean(false);
    private Date mLastRecordNotify;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        StatusHandler handler = new StatusHandler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                return handleStatusMessage(msg);
            }
        });
        Utils.setHandler(handler);
        mDroneCam = new DroneCam(handler);
        mLastRecordNotify = new Date(); // init here so it's never null

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mCamView = findViewById(R.id.camview_texture);
        mStreamButton = findViewById(R.id.stream_button);
        mRecordButton = findViewById(R.id.record_button);

        // Set up the user interaction to manually show or hide the system UI.
        mCamView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
        mCamView.setSurfaceTextureListener(new CameraViewListener(mDroneCam, mCamView));

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        mStreamButton.setOnTouchListener(mDelayHideTouchListener);
        mStreamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleStreamClick((Button)v);
            }
        });
        mRecordButton.setOnTouchListener(mDelayHideTouchListener);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRecordClick((Button)v);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    protected void onStop() {
        Utils.logv(TAG, "on stop called");
        mDroneCam.stopStreaming();
        super.onStop();
    }

    private void toggle() {
        if (mVisible) {
            hide();
        }
        else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mCamView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private void handleStreamClick(@NonNull Button b) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String currText = b.getText().toString();

        if (currText.equals(getString(R.string.start_stream))) {
            disableButton(b);
            try {
                String host = getSettingString(
                        R.string.settings_key_cam_ip, R.string.default_cam_ip);
                int port = getSettingInt(
                        R.string.settings_key_cam_stream_port, R.string.default_cam_stream_port);
                mDroneCam.startStreaming(host, port);
            }
            catch (DroneCamException e) {
                Utils.showMessage(e.getMessage());
            }
        }
        else if (currText.equals(getString(R.string.stop_stream))) {
            mResumeStreaming.set(false);
            if (mDroneCam.isStreaming()) {
                mDroneCam.stopStreaming();
                enableButton(b);
            }
        }
    }

    private void handleRecordClick(@NonNull Button b) {
        String currText = b.getText().toString();
        String host = getSettingString(R.string.settings_key_cam_ip, R.string.default_cam_ip);
        int port = getSettingInt(
                R.string.settings_key_cam_ctrl_port, R.string.default_cam_ctrl_port);

        if (currText.equals(getString(R.string.start_record))) {
            // Set current time here for checkResumeStreaming() to use in case streaming
            // gets interrupted from this recording action.
            synchronized (mResumeStreaming) {
                mLastRecordNotify = new Date();
                mResumeStreaming.set(mDroneCam.isStreaming());
            }

            disableButton(b);
            mDroneCam.startRemoteRecord(host, port);
        }
        else if (currText.equals(getString(R.string.stop_record))) {
            mDroneCam.stopRemoteRecord(host, port);
            enableButton(b);
        }
    }

    private String getSettingString(int keyId, int defaultId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getString(getString(keyId), getString(defaultId));
    }

    private int getSettingInt(int keyId, int defaultId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String val = getSettingString(keyId, defaultId);

        try {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e) {
            Utils.loge(TAG, "failed to parse int setting value: %s", val);
            return 0;
        }
    }

    private void enableButton(Button btn) {
        btn.setTextColor(getColor(R.color.button_text_enabled));
        btn.setEnabled(true);
    }

    private void disableButton(Button btn) {
        btn.setEnabled(false);
        btn.setTextColor(getColor(R.color.button_text_disabled));
    }

    private boolean handleStatusMessage(@NonNull Message m) {
        if (m.what != StatusHandler.STATUS_MESSAGE) {
            return false;
        }

        StatusMessage msg = (StatusMessage)m.obj;
        String msgStr = msg.getMessage(this);

        switch (msg.getType()) {
            case NOTE:
                Toast t = Toast.makeText(this, msgStr, Toast.LENGTH_SHORT);
                t.setGravity(Gravity.CENTER, 0, 0);
                t.show();
                break;

            case STREAM:
                if (msg.getSubType() == StatusMessage.SubType.STARTED) {
                    mStreamButton.setText(R.string.stop_stream);
                    enableButton(mStreamButton);
                }
                else if (msg.getSubType() == StatusMessage.SubType.STOPPED) {
                    mStreamButton.setText(R.string.start_stream);
                    enableButton(mStreamButton);
                    checkResumeStreaming();
                }
                else if (msg.getSubType() == StatusMessage.SubType.ERROR) {
                    Utils.showMessage(msgStr);
                    mStreamButton.setText(R.string.start_stream);
                    enableButton(mStreamButton);
                    checkResumeStreaming();
                }
                break;

            case RECORD:
                if (msg.getSubType() == StatusMessage.SubType.STARTED) {
                    mRecordButton.setText(R.string.stop_record);
                    enableButton(mRecordButton);
                }
                else if (msg.getSubType() == StatusMessage.SubType.STOPPED) {
                    mRecordButton.setText(R.string.start_record);
                    enableButton(mRecordButton);
                }
                else if (msg.getSubType() == StatusMessage.SubType.ERROR) {
                    Utils.showMessage(msgStr);
                    mRecordButton.setText(R.string.start_record);
                    enableButton(mRecordButton);
                }
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + msg.getType());
        }

        return true;
    }

    /*
     * Sometimes streaming gets interrupted when starting a recording. This will check if
     * streaming was enabled at the time of starting a recording, and re-enable streaming
     * through a button click. It only does this if the stream was interrupted <2 seconds
     * from recording, so that it doesn't try to re-enable due to some other interruption.
     */
    private void checkResumeStreaming() {
        synchronized (mResumeStreaming) {
            if (mResumeStreaming.get()) {
                if (new Date().getTime() - mLastRecordNotify.getTime() < 2000) {
                    if (!mDroneCam.isStreaming()) {
                        mResumeStreaming.set(false);
                        mStreamButton.performClick();
                    }
                }
                else {
                    mResumeStreaming.set(false);
                }
            }
        }
    }
}
