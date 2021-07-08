package com.meekworth.lwdronecam.lwcomms;

import com.meekworth.lwdronecam.utils.ReceiveAllStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Command {
    private static final String TAG = "LWDroneCam/lwcomms.Command";
    private static final byte[] LEWEI_MAGIC = "lewei_cmd\0".getBytes(StandardCharsets.UTF_8);
    private static final int HDR_LEN = 0x2e;
    private static final int TYPE_OFF = LEWEI_MAGIC.length;

    private static final int NUM_ARGS = 8;
    public static final int ARG_STATUS = 0;
    public static final int ARG_BODY_LEN = 2;
    public static final int ARG_STREAM_TYPE = 3;
    public static final int ARG_STREAM_DEC1 = 4;
    public static final int ARG_STREAM_DEC2 = 5;

    // 10MiB should be WAY above what the camera should send. This is just used as a sanity
    // check to prevent a large allocation if some other module version has the fields in
    // different locations.
    private static final int MAX_BODY_LEN = 10 * 1024 * 1024;

    public enum Type {
        HEARTBEAT(0x01),
        START_STREAM(0x02),
        STOP_STREAM(0x03),
        SET_TIME(0x04),
        GET_RECORD_PLAN(0x06),
        SET_RECORD_PLAN(0x11),
        STREAM_FRAME(0x101);

        private final int mCmdVal;
        private static final Map<Integer, Type> lookup = new HashMap<>();

        static {
            for (Type t : Type.values()) {
                lookup.put(t.getCommandValue(), t);
            }
        }

        Type(int num) {
            mCmdVal = num;
        }

        int getCommandValue() {
            return mCmdVal;
        }

        static Type fromValue(int val) throws LwcommsException {
            Type type = lookup.get(val);

            if (type == null) {
                throw new LwcommsException("type not found for value %d", val);
            }

            return type;
        }
    }

    private final Type mType;
    private final int[] mArgs;
    private final byte[] mBody;

    Command(Type type) {
        this(type, new int[NUM_ARGS], new byte[0]);
    }

    Command(Type type, int status) {
        this(type);
        this.setArgument(ARG_STATUS, status);
    }

    Command(Type type, byte[] body) {
        this(type, new int[NUM_ARGS], body);
    }

    Command(Type type, int[] args, byte[] body) {
        mType = type;
        mBody = body;
        mArgs = args;
    }

    int getArgument(int n) {
        return mArgs[n];
    }

    byte[] getBody() {
        return mBody;
    }

    Type getType() {
        return mType;
    }

    void setArgument(int arg, int value) {
        mArgs[arg] = value;
    }

    byte[] toBytes() {
        ByteBuffer buf = ByteBuffer.allocate(HDR_LEN + mBody.length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(LEWEI_MAGIC)
                .putInt(mType.getCommandValue());
        for (int arg : mArgs) {
            buf.putInt(arg);
        }
        if (mBody.length > 0) {
            buf.put(mBody, HDR_LEN, mBody.length);
        }
        return buf.array();
    }

    private static Command headerFromBytes(byte[] data) throws LwcommsException {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        Type type;
        int[] args = new int[NUM_ARGS];
        int bodyLen;
        byte[] body;

        if (data.length < Command.HDR_LEN) {
            throw new LwcommsException("not enough data for response");
        }

        for (int i = 0; i < Command.LEWEI_MAGIC.length; i++) {
            if (buf.get(i) != Command.LEWEI_MAGIC[i]) {
                throw new LwcommsException("invalid magic bytes");
            }
        }

        buf.position(TYPE_OFF);
        type = Type.fromValue(buf.getInt());
        for (int i = 0; i < args.length; i++) {
            args[i] = buf.getInt();
        }
        bodyLen = args[ARG_BODY_LEN];

        if (bodyLen < 0 || bodyLen > MAX_BODY_LEN) {
            throw new LwcommsException("invalid body len %d", bodyLen);
        }
        body = new byte[bodyLen];

        return new Command(type, args, body);
    }

    static Command fromStream(ReceiveAllStream in) throws LwcommsException, IOException {
        byte[] hdr = new byte[HDR_LEN];
        Command cmd;

        in.recvAll(hdr);
        cmd = headerFromBytes(hdr);

        if (cmd.mBody.length > 0) {
            in.recvAll(cmd.mBody);
        }

        return cmd;
    }
}
