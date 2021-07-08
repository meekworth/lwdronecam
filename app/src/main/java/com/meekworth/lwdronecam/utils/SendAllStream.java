package com.meekworth.lwdronecam.utils;

import java.io.IOException;
import java.io.OutputStream;

public class SendAllStream {
    private final OutputStream mOut;

    public SendAllStream(OutputStream out) {
        mOut = out;
    }

    /**
     * Just a wrapper for write() and flush().
     * @param b    Bytes to send
     * @throws IOException  Propagates socket's IOException
     */
    public void sendAll(byte[] b) throws IOException {
        mOut.write(b);
        mOut.flush();
    }
}
