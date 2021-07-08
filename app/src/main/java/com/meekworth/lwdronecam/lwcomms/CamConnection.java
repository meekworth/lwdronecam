package com.meekworth.lwdronecam.lwcomms;

import com.meekworth.lwdronecam.utils.Log;
import com.meekworth.lwdronecam.utils.ReceiveAllStream;
import com.meekworth.lwdronecam.utils.SendAllStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class CamConnection extends Socket {
    private static final String TAG = "LWDroneCam/lwcomms.CamConnection";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int BLOCKING_TIMEOUT_MS = 1000;

    // IO streams for the socket
    private ReceiveAllStream mIn;
    private SendAllStream mOut;

    private CamConnection() {
        super();
    }

    @Override
    public void close() throws IOException {
        Log.d(TAG, "closing camera connection");
        // TODO: any other closing stuff
        super.close();
    }

    public static CamConnection createAndConnect(String host, int port) throws IOException {
        final CamConnection conn = new CamConnection();

        InetSocketAddress addr = new InetSocketAddress(host, port);
        conn.connect(addr, CONNECT_TIMEOUT_MS);
        conn.setSoTimeout(BLOCKING_TIMEOUT_MS);
        Log.d(TAG, "connected to [%s]:%d", host, port);

        conn.mIn = new ReceiveAllStream(new BufferedInputStream(conn.getInputStream()));
        conn.mOut = new SendAllStream(new BufferedOutputStream(conn.getOutputStream()));

        return conn;
    }

    public Heartbeat getHeartbeat() throws LwcommsException, IOException {
        return (Heartbeat)sendCmdAndGetResponse(new Command(Command.Type.HEARTBEAT));
    }

    private Response getResponse() throws LwcommsException, IOException {
        Command respCmd = Command.fromStream(mIn);

        switch (respCmd.getType()) {
            case HEARTBEAT: return Heartbeat.fromBytes(respCmd.getBody());
            case GET_RECORD_PLAN: return RecordPlan.fromBytes(respCmd.getBody());
            case STREAM_FRAME: return StreamFrame.fromBytes(respCmd);

            case SET_RECORD_PLAN:
                // fall through
            case SET_TIME:
                return new StatusResponse(respCmd.getArgument(Command.ARG_STATUS));

            default:
                throw new LwcommsException("invalid response type %d", respCmd.getType());
        }
    }

    public boolean isRecording() throws LwcommsException, IOException {
        RecordPlan plan = (RecordPlan)sendCmdAndGetResponse(
                new Command(Command.Type.GET_RECORD_PLAN));
        return plan.isActive();
    }

    public void setCurrentTime() throws LwcommsException, IOException {
        ByteBuffer body = ByteBuffer.allocate(Long.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(System.currentTimeMillis() / 1000);
        sendCmdAndGetResponse(new Command(Command.Type.SET_TIME, body.array()));
    }

    private void sendCmd(Command cmd) throws IOException {
        mOut.sendAll(cmd.toBytes());
    }

    private Response sendCmdAndGetResponse(Command cmd) throws IOException, LwcommsException {
        sendCmd(cmd);
        return getResponse();
    }

    public boolean startRecording() throws LwcommsException, IOException {
        RecordPlan plan = RecordPlan.getDefault(true);
        StatusResponse resp = (StatusResponse)sendCmdAndGetResponse(
                new Command(Command.Type.SET_RECORD_PLAN, plan.toBytes()));
        return resp.getStatus() == 0;
    }

    public boolean stopRecording() throws LwcommsException, IOException {
        RecordPlan plan = RecordPlan.getDefault(false);
        StatusResponse resp = (StatusResponse)sendCmdAndGetResponse(
                new Command(Command.Type.SET_RECORD_PLAN, plan.toBytes()));
        return resp.getStatus() == 0;
    }

    public void streamVideo(AtomicBoolean runningFlag, StreamingTarget... targets) {
        Command hbCmd = new Command(Command.Type.HEARTBEAT);
        final long HB_INTERVAL_MS = 3000;
        final AtomicBoolean hbFlag = new AtomicBoolean(false);
        Timer hbTimer = new Timer();

        hbTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                hbFlag.set(true);
            }
        }, 1000, HB_INTERVAL_MS);

        try {
            sendCmd(new Command(Command.Type.START_STREAM, 1));

            while (runningFlag.get()) {
                Response resp = getResponse();
                if (!(resp instanceof StreamFrame)) {
                    // ignore responses that aren't the frame, such as heartbeats
                    continue;
                }

                for (StreamingTarget t : targets) {
                    t.sendFrame(((StreamFrame)resp).getBytes());
                }

                if (hbFlag.compareAndSet(true, false)) {
                    sendCmd(hbCmd);
                }
            }

            sendCmd(new Command(Command.Type.STOP_STREAM));
        }
        catch (IOException | LwcommsException e) {
            Log.e(TAG, "streamming stopped: %s", e.getMessage());
        }

        hbTimer.cancel();

        for (StreamingTarget t : targets) {
            t.finished();
        }
    }
}
