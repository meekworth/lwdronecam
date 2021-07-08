package com.meekworth.lwdronecam;

import android.graphics.SurfaceTexture;

import com.meekworth.lwdronecam.lwcomms.CamConnection;
import com.meekworth.lwdronecam.lwcomms.LwcommsException;
import com.meekworth.lwdronecam.utils.Log;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * This class is used on the main thread, so the actual connections are separated out
 * into the CamConnection class, which is used in threads started by this class.
 */
public class DroneCam {
    private static final String TAG = "LWDroneCam/DroneCam";
    private static final int MAX_STREAMS = 1;

    // Default from what the drone's camera sends back
    // TODO: maybe put these in settings
    static final int DEFAULT_VID_WIDTH = 1280;
    static final int DEFAULT_VID_HEIGHT = 720;

    private final StatusHandler mHandler;
    private final AtomicBoolean mStreamOn;
    private final Semaphore mStreamSem;
    private SurfaceTexture mSurface;
    private String mHost;
    private int mStreamPort;
    private int mCmdPort;

    DroneCam(StatusHandler handler, String host, int streamPort, int cmdPort) {
        mHandler = handler;
        mStreamOn = new AtomicBoolean(false);
        mStreamSem = new Semaphore(MAX_STREAMS, true);
        mHost = host;
        mStreamPort = streamPort;
        mCmdPort = cmdPort;
    }

    void setSurface(SurfaceTexture surface) {
        mSurface = surface;
    }

    void setConnectionSettings(String host, int streamPort, int cmdPort) {
        mHost = host;
        mStreamPort = streamPort;
        mCmdPort = cmdPort;
    }

    void startStreaming() throws DroneCamException {
        if (mSurface == null) {
            throw new DroneCamException("Streaming view not ready");
        }

        if (!mStreamSem.tryAcquire()) {
            throw new DroneCamException("Streaming already enabled");
        }

        new Thread(() -> {
            StreamingCodecTarget streamTarget;

            try {
                streamTarget = new StreamingCodecTarget(
                        mSurface, DEFAULT_VID_WIDTH, DEFAULT_VID_HEIGHT);
            }
            catch (IOException e) {
                Log.e(TAG, "failed to create codec: %s", e.getMessage());
                mStreamSem.release();
                mHandler.sendMessage(new StatusMessage(
                        StatusMessage.Type.STREAM,
                        StatusMessage.SubType.ERROR,
                        R.string.error_create_codec_failed));
                return;
            }

            try (CamConnection conn = CamConnection.createAndConnect(mHost, mStreamPort)) {
                mStreamOn.set(true);
                mHandler.sendMessage(new StatusMessage(
                        StatusMessage.Type.STREAM,
                        StatusMessage.SubType.STARTED));
                conn.streamVideo(mStreamOn, streamTarget);
                mHandler.sendMessage(new StatusMessage(
                        StatusMessage.Type.NOTE,
                        R.string.stream_ended));
            }
            catch (UnknownHostException e) {
                Log.e(TAG, "resolving %s failed: %s", mHost, e.getMessage());
                mHandler.sendMessage(new StatusMessage(
                        StatusMessage.Type.STREAM,
                        StatusMessage.SubType.ERROR,
                        R.string.error_resolve_host, mHost));
            }
            catch (SocketTimeoutException e) {
                Log.e(TAG, "timeout connecting to %s", mHost);
                mHandler.sendMessage(new StatusMessage(
                        StatusMessage.Type.STREAM,
                        StatusMessage.SubType.ERROR,
                        R.string.error_timeout_connect, mHost, streamTarget));
            }
            catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            mStreamOn.set(false);
            mStreamSem.release();
            mHandler.sendMessage(new StatusMessage(
                    StatusMessage.Type.STREAM,
                    StatusMessage.SubType.STOPPED));
        }).start();
    }

    boolean isStreaming() {
        return mStreamOn.get();
    }

    void stopStreaming() {
        Log.d(TAG, "stopping streaming");
        mStreamOn.set(false);
    }

    void checkRemoteRecording() {
        new Thread(() -> {
            try (CamConnection conn = CamConnection.createAndConnect(mHost, mCmdPort)) {
                if (conn.isRecording()) {
                    mHandler.sendMessage(new StatusMessage(
                            StatusMessage.Type.RECORD,
                            StatusMessage.SubType.STARTED));
                }
                else {
                    mHandler.sendMessage(new StatusMessage(
                            StatusMessage.Type.RECORD,
                            StatusMessage.SubType.STOPPED));
                }
            }
            catch (LwcommsException e) {
                Log.e(TAG, e.getMessage());
            }
            catch (IOException e) {
                Log.e(TAG, "error with cmd connection: %s", e.getMessage());
            }
        }).start();
    }

    void startRemoteRecord() {
        new Thread(() -> {
            boolean succ = false;

            try {
                try (CamConnection conn = CamConnection.createAndConnect(mHost, mCmdPort)) {
                    conn.setCurrentTime();
                }
                try (CamConnection conn = CamConnection.createAndConnect(mHost, mCmdPort)) {
                    succ = conn.startRecording();
                }
            }
            catch (LwcommsException e) {
                Log.e(TAG, e.getMessage());
            }
            catch (IOException e) {
                Log.e(TAG, "error with cmd connection: %s", e.getMessage());
            }

            if (succ) {
                mHandler.sendMessage(new StatusMessage(
                        StatusMessage.Type.RECORD,
                        StatusMessage.SubType.STARTED));
            }
            else {
                mHandler.sendMessage(new StatusMessage(
                        StatusMessage.Type.RECORD,
                        StatusMessage.SubType.ERROR,
                        R.string.error_start_record_failed));
            }
        }).start();
    }

    void stopRemoteRecord() {
        new Thread(() -> {
            boolean succ = false;

            try (CamConnection conn = CamConnection.createAndConnect(mHost, mCmdPort)) {
                succ = conn.stopRecording();
            }
            catch (LwcommsException e) {
                Log.e(TAG, e.getMessage());
            }
            catch (IOException e) {
                Log.e(TAG, "error with cmd connection: %s", e.getMessage());
            }

            if (succ) {
                mHandler.sendMessage(new StatusMessage(
                        StatusMessage.Type.RECORD,
                        StatusMessage.SubType.STOPPED));
            }
            else {
                mHandler.sendMessage(new StatusMessage(
                        StatusMessage.Type.RECORD,
                        StatusMessage.SubType.ERROR,
                        R.string.error_stop_record_failed));
            }
        }).start();
    }
}
