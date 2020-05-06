package com.meekworth.lwdronecam;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

class StreamingConnector {
    private static final String TAG = "LWDroneCam/StreamingConnector";

    private CameraConnection mCamConnection;
    private final LinkedList<StreamingTarget> mTargets = new LinkedList<>();
    private final LinkedBlockingQueue<ByteBuffer> mQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);

    StreamingConnector(CameraConnection source) {
        mCamConnection = source;
    }

    void addTarget(StreamingTarget target) {
        synchronized (mTargets) {
            if (!mTargets.contains(target)) {
                mTargets.add(target);
            }
        }
    }

    void streamingLoop() throws IOException, DroneCamException {
        mCamConnection.startStreaming();
        new Thread(new FramesTask()).start();

        while (running.get()) {
            byte[] frameBytes;

            try {
                frameBytes = mQueue.take().array();
                if (frameBytes.length == 0) {
                    break;
                }
            }
            catch (InterruptedException e) {
                Utils.loge(TAG, "queue take failed, %s", e.getMessage());
                break;
            }

            synchronized (mTargets) {
                for (StreamingTarget target : mTargets) {
                    target.sendFrame(frameBytes);
                }
            }
        }

        Utils.logd(TAG, "done streaming, calling finished");
        synchronized (mTargets) {
            for (StreamingTarget target : mTargets) {
                target.finished();
            }
        }
        mCamConnection.stopStreaming();
    }

    void stop() {
        running.set(false);
    }

    private class FramesTask implements Runnable {
        @Override
        public void run() {
            while (running.get()) {
                byte[] frameBytes;

                try {
                    frameBytes = mCamConnection.getFrameBytes();
                    if (frameBytes == null) {
                        break;
                    }
                }
                catch (InterruptedIOException e) {
                    // Catch if a timeout occurs on the blocking socket op. Go back to beginning
                    // of loop and try again if still running.
                    continue;
                }
                catch (IOException | DroneCamException e) {
                    // Just end the loop to stop things if some other error occurs.
                    Utils.loge(TAG, "error getting frame: %s", e.getMessage());
                    break;
                }

                mQueue.add(ByteBuffer.wrap(frameBytes));
            }

            // Add empty buffer to signify stream is done
            mQueue.add(ByteBuffer.allocate(0));
        }
    }
}
