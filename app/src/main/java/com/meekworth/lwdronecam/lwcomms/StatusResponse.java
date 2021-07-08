package com.meekworth.lwdronecam.lwcomms;

public class StatusResponse implements  Response {
    private final int mStatus;

    public StatusResponse(int status) {
        mStatus = status;
    }

    public int getStatus() {
        return mStatus;
    }
}
