package com.meekworth.lwdronecam;

import android.content.Context;

class StatusMessage {
    private static final String TAG = "LWDroneCame/StatusMessage";

    enum Type {
        NOTE,
        STREAM,
        RECORD
    }

    enum SubType {
        NONE,
        ERROR,
        STARTED,
        STOPPED
    }

    private enum MessageType {
        NOT_SET,
        STRING,
        RES_ID
    }

    private Type mType;
    private SubType mSubType;
    private MessageType mMsgType;
    private String mMsgFmt;
    private int mMsgFmtId;
    private Object[] mMsgArgs;

    StatusMessage(Type type) {
        mType = type;
        mMsgType = MessageType.NOT_SET;
    }

    StatusMessage(Type type, SubType subType) {
        this(type);
        mSubType = subType;
    }

    StatusMessage(Type type, SubType subType, String fmt, Object... args) {
        this(type, subType);
        mMsgFmt = fmt;
        mMsgArgs = args;
        mMsgType = MessageType.STRING;
    }

    StatusMessage(Type type, SubType subType, int fmtId, Object... args) {
        this(type, subType);
        mMsgFmtId = fmtId;
        mMsgArgs = args;
        mMsgType = MessageType.RES_ID;
    }

    StatusMessage(Type type, String fmt, Object... args) {
        this(type, SubType.NONE, fmt, args);
    }

    StatusMessage(Type type, int fmtId, Object... args) {
        this(type, SubType.NONE, fmtId, args);
    }

    Type getType() {
        return mType;
    }

    SubType getSubType() {
        return mSubType;
    }

    String getMessage(Context context) {
        if (context == null && mMsgType == MessageType.RES_ID) {
            throw new IllegalStateException("cannot build message string without context");
        }

        switch (mMsgType) {
            case NOT_SET:
                return "";

            case STRING:
                return String.format(mMsgFmt, mMsgArgs);

            case RES_ID:
                return context.getString(mMsgFmtId, mMsgArgs);

            default:
                throw new IllegalStateException("Unexpected value: " + mMsgType);
        }
    }
}
