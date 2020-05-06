package com.meekworth.lwdronecam;

class DroneCamException extends Exception {

    DroneCamException(String message) {
        super(message);
    }

    DroneCamException(String fmt, Object... args) {
        super(String.format(fmt, args));
    }
}
