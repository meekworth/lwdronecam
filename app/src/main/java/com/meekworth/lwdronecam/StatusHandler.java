package com.meekworth.lwdronecam;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

class StatusHandler extends Handler {
    private static final String TAG = "LWDroneCam/StatusHandler";
    static final int STATUS_MESSAGE = 1;

    StatusHandler(Looper looper, Handler.Callback callback) {
        super(looper, callback);
    }

    void sendMessage(StatusMessage statusMessage) {
        Message msg = obtainMessage(STATUS_MESSAGE, statusMessage);
        msg.sendToTarget();;
    }
}
