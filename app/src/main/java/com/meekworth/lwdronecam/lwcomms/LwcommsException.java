package com.meekworth.lwdronecam.lwcomms;

public class LwcommsException extends Exception {
    public LwcommsException(String message) {
        super(message);
    }

    public LwcommsException(String fmt, Object... args) {
        super(String.format(fmt, args));
    }
}
