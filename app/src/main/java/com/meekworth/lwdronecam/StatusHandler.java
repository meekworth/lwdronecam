package com.meekworth.lwdronecam;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.concurrent.LinkedBlockingQueue;

class StatusHandler {
    private static final String TAG = "LWDroneCam/StatusHandler";
    static final int STATUS_MESSAGE = 1;
    private static StatusHandler sInstance = null;

    private final Handler mHandler;
    private final LinkedBlockingQueue<String> mQueue;

    private StatusHandler(Looper looper, Handler.Callback callback) {
        Thread toastThread;

        mHandler = new Handler(looper, callback);
        mQueue = new LinkedBlockingQueue<>();
        toastThread = new Thread(() -> {
            for (;;) {
                try {
                    sendMessage(new StatusMessage(StatusMessage.Type.NOTE, mQueue.take()));
                    Thread.sleep(2500); // LENGTH_SHORT should be 2s, so sleep +500ms
                }
                catch (InterruptedException e) {
                    break;
                }
            }
        });
        toastThread.start();
    }

    void sendMessage(StatusMessage statusMessage) {
        Message msg = mHandler.obtainMessage(STATUS_MESSAGE, statusMessage);
        msg.sendToTarget();
    }

    static StatusHandler getInstance(Looper looper, Handler.Callback callback) {
        if (sInstance == null) {
            sInstance = new StatusHandler(looper, callback);
        }

        return sInstance;
    }

    static void showMessage(String fmt, Object... args) {
        sInstance.mQueue.add(String.format(fmt, args));
    }
}
