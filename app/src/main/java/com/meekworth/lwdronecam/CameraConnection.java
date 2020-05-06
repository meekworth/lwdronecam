package com.meekworth.lwdronecam;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

class CameraConnection extends Socket {
    private static final String TAG = "LWDroneCam/CameraConnection";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int BLOCKING_TIMEOUT_MS = 1000;

    private static final int BUF_LEN = 4096;
    private static final int HDR1_LEN = 0x2e;
    private static final int HDR2_LEN = 0x20;
    private static final byte[] LEWEI_MAGIC = "lewei_cmd\0".getBytes(StandardCharsets.UTF_8);
    private static final int HDR1_CMD_OFF = LEWEI_MAGIC.length;
    private static final int HDR1_PAYLOAD_SIZE_OFF = HDR1_CMD_OFF + 12;
    private enum CommandType {
        HEARTBEAT(0x01),
        START_STREAM(0x02),
        STOP_STREAM(0x03),
        SET_TIME(0x04),
        SET_REMOTE_RECORD(0x11);

        private final int mCmdVal;

        CommandType(int num) {
            mCmdVal = num;
        }

        int getCommandValue() {
            return mCmdVal;
        }
    }

    // IO streams for the socket
    private BufferedInputStream mIn;
    private BufferedOutputStream mOut;

    // Buffers to reuse between getFrameBytes() calls
    private byte[] mHdr1Bytes;
    private byte[] mHdr2Bytes;
    private ByteArrayOutputStream mFrameData;

    // Timer for scheduling the heartbeat packets
    private static final int HEARTBEAT_SCHEDULE_SEC = 3;
    private Timer mHeartbeatTimer;
    private AtomicBoolean mHeartbeatFlag;

    private CameraConnection() {
        super();

        // Init reuse buffers
        mHdr1Bytes = new byte[HDR1_LEN];
        mHdr2Bytes = new byte[HDR2_LEN];
        mFrameData = new ByteArrayOutputStream();

        mHeartbeatFlag = new AtomicBoolean(false);
    }

    static CameraConnection createAndConnect(String host, int port) throws IOException {
        final CameraConnection conn = new CameraConnection();

        InetSocketAddress addr = new InetSocketAddress(host, port);
        conn.connect(addr, CONNECT_TIMEOUT_MS);
        conn.setSoTimeout(BLOCKING_TIMEOUT_MS);
        Utils.logd(TAG, "connected to [%s]:%d", host, port);

        conn.mIn = new BufferedInputStream(conn.getInputStream());
        conn.mOut = new BufferedOutputStream(conn.getOutputStream());

        return conn;
    }

    void startStreaming() throws IOException, DroneCamException {
        // Send start stream command first, then start HB scheduler with an initial delay
        sendCmd(CommandType.START_STREAM);
        mHeartbeatTimer = new Timer();
        mHeartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHeartbeatFlag.compareAndSet(false, true);
            }
        }, 1000, HEARTBEAT_SCHEDULE_SEC * 1000);
    }

    byte[] getFrameBytes() throws IOException, DroneCamException {
        ByteBuffer hdr1Buf = ByteBuffer.wrap(mHdr1Bytes);
        ByteBuffer hdr2Buf = ByteBuffer.wrap(mHdr2Bytes);
        byte[] frameBytes;
        int streamFlag;
        int frameSize;
        int idx;

        hdr1Buf.order(ByteOrder.LITTLE_ENDIAN);
        hdr2Buf.order(ByteOrder.LITTLE_ENDIAN);

        // Get and verify the first header
        recvAll(mHdr1Bytes);
        for (int i = 0; i < LEWEI_MAGIC.length; i++) {
            if (mHdr1Bytes[i] != LEWEI_MAGIC[i]) {
                throw new DroneCamException("Invalid packet header");
            }
        }
        streamFlag = hdr1Buf.getInt(26);

        // Get the second header
        recvAll(mHdr2Bytes);
        frameSize = (streamFlag == 1) ? hdr2Buf.getInt(4) : 32;
        if (frameSize < 0) {
            throw new DroneCamException("Invalid frame size");
        }

        // Get the frame (or heartbeat response, which will be ignored)
        mFrameData.reset();
        recvAll(mFrameData, frameSize);

        // If this was a heartbeat response, call again to get and return the next frame packet
        if (streamFlag != 1) {
            return getFrameBytes();
        }

        // Fix munged byte in frame
        frameBytes = mFrameData.toByteArray();
        idx = encoded_index(hdr2Buf.getLong(8), frameSize);
        if (0 <= idx && idx < frameBytes.length) {
            frameBytes[idx] = (byte)~frameBytes[idx];
        }

        // Send heartbeat if ready
        if (mHeartbeatFlag.compareAndSet(true, false)) {
            sendCmd(CommandType.HEARTBEAT);
        }

        return frameBytes;
    }

    void stopStreaming() throws IOException, DroneCamException {
        if (mHeartbeatTimer != null) {
            mHeartbeatTimer.cancel();
            mHeartbeatTimer = null;
        }

        sendCmd(CommandType.STOP_STREAM);
    }

    private int encoded_index(long p1, long p2) {
        long v1;
        long v2;

        p2 &= 0xffffffff;
        v2 = ((p2 & 1) == 0) ?
                (p2 + 1 + (p2 ^ p1)) ^ p2 :
                ((p2 ^ p1) + p2) ^ p2;
        v1 = (p2 != 0) ? (v2 / p2) : 0;

        return (int)(v2 - (v1 * p2));
    }

    void setCurrentTime() throws IOException, DroneCamException {
        ByteBuffer payload = ByteBuffer.allocate(Long.BYTES); // store 1 long
        payload.order(ByteOrder.LITTLE_ENDIAN).putLong(System.currentTimeMillis() / 1000);
        sendCmd(CommandType.SET_TIME, payload.array());
        getAndCheckResponse(CommandType.SET_TIME); // just get response, ignore returned value
    }

    boolean startRemoteRecord() throws IOException, DroneCamException {
        return setRemoteRecord(true);
    }

    boolean stopRemoteRecord() throws IOException, DroneCamException {
        return setRemoteRecord(false);
    }

    private boolean setRemoteRecord(boolean start) throws IOException, DroneCamException {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT:+8:00"));
        int weekday = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY; // tm_wday starts at 0
        ByteBuffer payload = ByteBuffer.allocate(5 * Integer.BYTES) // store 5 ints
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(start ? 1 : 0) // active = 1
                .putInt(1 << weekday) // day flag
                .putInt(0) // start time, 0 = now
                .putInt((60 * 60 * 24) - 1) // stop time, +1 day
                .putInt(60 * 5); // record length, 5 minutes
        sendCmd(CommandType.SET_REMOTE_RECORD, payload.array());

        return getAndCheckResponse(CommandType.SET_REMOTE_RECORD);
    }

    private boolean getAndCheckResponse(CommandType type) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(HDR1_LEN).order(ByteOrder.LITTLE_ENDIAN);
        recvAll(buf.array());
        return buf.getInt(HDR1_CMD_OFF) == type.getCommandValue();
    }

    private void sendCmd(CommandType type) throws IOException, DroneCamException {
        sendCmd(type, null);
    }

    private void sendCmd(CommandType type, byte[] payload) throws IOException, DroneCamException {
        ByteBuffer buf = ByteBuffer.allocate(HDR1_LEN + (payload == null ? 0 : payload.length))
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(LEWEI_MAGIC)
                .putInt(type.getCommandValue());

        // Put any additional data according to command
        switch (type) {
            case START_STREAM:
                buf.putInt(1);
                break;

            case HEARTBEAT:
            case STOP_STREAM:
            case SET_TIME:
            case SET_REMOTE_RECORD:
                break;

            default:
                throw new DroneCamException("Invalid command type");
        }

        if (payload != null) {
            buf.position(HDR1_PAYLOAD_SIZE_OFF);
            buf.putInt(payload.length);
            buf.position(HDR1_LEN);
            buf.put(payload);
        }
        else {
            buf.position(HDR1_LEN);
        }

        sendAll(buf.array());
    }

    /**
     * Just a wrapper for write() and flush().
     * @param b  Bytes to send
     * @throws IOException  Propagates socket's IOException
     */
    private void sendAll(byte[] b) throws IOException {
        mOut.write(b);
        mOut.flush();
    }

    /**
     * Calls input stream's read() until it fills up the given allocated buffer.
     * @param buf  Buffer to write
     * @throws IOException  Propagates socket's IOException
     */
    private void recvAll(byte[] buf) throws IOException {
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
     * @param n  Number of bytes to read.
     * @throws IOException  Propagates socket's IOException
     */
    private void recvAll(ByteArrayOutputStream buf, int n) throws IOException {
        byte[] b = new byte[BUF_LEN];
        int tot = 0;

        while (tot < n) {
            int nread = mIn.read(b, 0, Math.min(b.length, n - tot));
            if (nread <= 0)
                throw new IOException("failed to read all bytes");
            tot += nread;
            buf.write(b, 0, nread);
        }
    }

    @Override
    public void close() throws IOException {
        Utils.logd(TAG, "closing camera connection");
        if (mHeartbeatTimer != null) {
            mHeartbeatTimer.cancel();
            mHeartbeatTimer = null;
        }
        super.close();
    }
}
