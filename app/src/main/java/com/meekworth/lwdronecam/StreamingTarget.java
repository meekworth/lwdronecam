package com.meekworth.lwdronecam;

interface StreamingTarget {
    void sendFrame(byte[] frameBytes);
    void finished();
}
