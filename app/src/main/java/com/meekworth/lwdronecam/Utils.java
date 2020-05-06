package com.meekworth.lwdronecam;

import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

final class Utils {
    private static StatusHandler sHandler = null;
    private static final LinkedBlockingQueue<String> sQueue = new LinkedBlockingQueue<>();

    // Thread to handle multiple Toast messages called quickly together. This will queue them up
    // and send for display (to the handler) every 2.5s.
    private static final Thread sToastThread = new Thread(new Runnable() {
        @Override
        public void run() {
            for (;;) {
                try {
                    if (sHandler != null) {
                        sHandler.sendMessage(
                                new StatusMessage(StatusMessage.Type.NOTE, sQueue.take()));
                        Thread.sleep(2500); // LENGTH_SHORT should be 2s, so sleep +500ms
                    }
                }
                catch (InterruptedException e) {
                    break;
                }
            }
        }
    });

    static {
        sToastThread.start();
    }

    static void setHandler(StatusHandler handler) {
        sHandler = handler;
    }

    static void showMessage(String fmt, Object... args) {
        sQueue.add(String.format(fmt, args));
    }

    static void loge(String tag, String fmt, Object... args) {
        Log.e(tag, String.format(fmt, args));
    }

    static void logi(String tag, String fmt, Object... args) {
        Log.i(tag, String.format(fmt, args));
    }

    static void logd(String tag, String fmt, Object... args) {
        Log.d(tag, String.format(fmt, args));
    }

    static void logv(String tag, String fmt, Object... args) {
        Log.v(tag, String.format(fmt, args));
    }
}
