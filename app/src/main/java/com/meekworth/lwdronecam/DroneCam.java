package com.meekworth.lwdronecam;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

class DroneCam {
    private static final String TAG = "LWDroneCam/DroneCam";

    // Default from what the drone's camera sends back
    static final int DEFAULT_VID_WIDTH = 1280;
    static final int DEFAULT_VID_HEIGHT = 720;

    private StatusHandler mHandler;
    private SurfaceTexture mSurface;
    private StreamingConnector mStreamer;
    private AtomicBoolean mStreaming;

    DroneCam(StatusHandler handler) {
        mHandler = handler;
        mStreaming = new AtomicBoolean(false);
    }

    void setSurface(SurfaceTexture surface) {
        mSurface = surface;
    }

    void startStreaming(final String host, final int port) throws DroneCamException {
        if (mSurface == null) {
            throw new DroneCamException("Streaming view not ready");
        }
        if (!mStreaming.compareAndSet(false, true)) {
            throw new DroneCamException("Streaming already enabled");
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                MediaCodec codec;

                try {
                    codec = createCodec();
                }
                catch (IOException e) {
                    Utils.loge(TAG, "failed to create codec: %s", e.getMessage());
                    mStreaming.set(false);
                    mHandler.sendMessage(new StatusMessage(
                            StatusMessage.Type.STREAM,
                            StatusMessage.SubType.ERROR,
                            R.string.error_create_codec_failed));
                    return;
                }

                try (CameraConnection camConn = CameraConnection.createAndConnect(host, port)) {
                    mStreamer = new StreamingConnector(camConn);
                    mStreamer.addTarget(new StreamingCodecTarget(codec));
                    mHandler.sendMessage(new StatusMessage(
                            StatusMessage.Type.STREAM,
                            StatusMessage.SubType.STARTED));
                    mStreamer.streamingLoop();
                    mHandler.sendMessage(new StatusMessage(
                            StatusMessage.Type.NOTE,
                            R.string.stream_ended));
                }
                catch (DroneCamException e) {
                    Utils.loge(TAG, e.getMessage());
                }
                catch (UnknownHostException e) {
                    Utils.loge(TAG, "resolving %s failed: %s", host, e.getMessage());
                    mHandler.sendMessage(new StatusMessage(
                            StatusMessage.Type.STREAM,
                            StatusMessage.SubType.ERROR,
                            R.string.error_resolve_host, host));
                }
                catch (SocketTimeoutException e) {
                    Utils.loge(TAG, "timeout connecting to %s", host);
                    mHandler.sendMessage(new StatusMessage(
                            StatusMessage.Type.STREAM,
                            StatusMessage.SubType.ERROR,
                            R.string.error_timeout_connect, host, port));
                }
                catch (IOException e) {
                    Utils.loge(TAG, "error streaming: %s", e.getMessage());
                }

                codec.stop();
                codec.release();
                mStreaming.set(false);
                mHandler.sendMessage(new StatusMessage(
                        StatusMessage.Type.STREAM,
                        StatusMessage.SubType.STOPPED));
            }
        }).start();
    }

    private MediaCodec createCodec() throws IOException {
        MediaCodec codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        MediaFormat format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, DEFAULT_VID_WIDTH, DEFAULT_VID_HEIGHT);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, DEFAULT_VID_WIDTH * DEFAULT_VID_HEIGHT);
        codec.configure(format, new Surface(mSurface), null, 0);
        codec.start();

        return codec;
    }

    boolean isStreaming() {
        return mStreaming.get();
    }

    void stopStreaming() {
        if (mStreamer != null) {
            Utils.logd(TAG, "stopping streaming");
            mStreamer.stop();
        }
    }

    void startRemoteRecord(final String host, final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean succ = false;

                try {
                    try (CameraConnection camConn =
                                 CameraConnection.createAndConnect(host, port)) {
                        camConn.setCurrentTime();
                    }
                    try (CameraConnection camConn =
                                 CameraConnection.createAndConnect(host, port)) {
                        succ = camConn.startRemoteRecord();
                    }
                }
                catch (DroneCamException e) {
                    Utils.loge(TAG, e.getMessage());
                }
                catch (IOException e) {
                    Utils.loge(TAG, "error with ctrl connection: %s", e.getMessage());
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
            }
        }).start();
    }

    void stopRemoteRecord(final String host, final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean succ = false;

                try (CameraConnection camConn = CameraConnection.createAndConnect(host, port)) {
                    succ = camConn.stopRemoteRecord();
                }
                catch (DroneCamException e) {
                    Utils.loge(TAG, e.getMessage());
                }
                catch (IOException e) {
                    Utils.loge(TAG, "error with ctrl connection: %s", e.getMessage());
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
            }
        }).start();
    }
}
