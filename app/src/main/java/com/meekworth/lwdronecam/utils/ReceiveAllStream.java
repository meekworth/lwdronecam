package com.meekworth.lwdronecam.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ReceiveAllStream {
    private final InputStream mIn;

    public ReceiveAllStream(InputStream in) {
        mIn = in;
    }

    /**
     * Calls input stream's read() until it fills up the given allocated buffer.
     * @param buf  Buffer to write
     * @throws IOException  Propagates socket's IOException
     */
    public void recvAll(byte[] buf) throws IOException {
        int tot = 0;

        while (tot < buf.length) {
            int nread = mIn.read(buf, tot, buf.length - tot);
            if (nread <= 0)
                throw new IOException("failed to read all bytes");
            tot += nread;
        }
    }

    /**
     * Calls input stream's read() until n bytes are read and written into the given buffer.
     * @param buf  Buffer to write read bytes into.
     * @param n    Number of bytes to read.
     * @throws IOException  Propagates socket's IOException
     */
    public void recvAll(ByteArrayOutputStream buf, int n) throws IOException {
        final int READ_BUF_LEN = 4096;
        byte[] b = new byte[READ_BUF_LEN];
        int tot = 0;

        while (tot < n) {
            int nread = mIn.read(b, 0, Math.min(b.length, n - tot));
            if (nread <= 0)
                throw new IOException("failed to read all bytes");
            tot += nread;
            buf.write(b, 0, nread);
        }
    }
}
