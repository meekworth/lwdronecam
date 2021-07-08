package com.meekworth.lwdronecam.lwcomms;

public interface StreamingTarget {
    void sendFrame(byte[] frameBytes);
    void finished();
}
