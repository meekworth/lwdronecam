package com.meekworth.lwdronecam.lwcomms;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.TimeZone;

public class Heartbeat implements Response {
    private static final int LEN = 64;

    private boolean mSdcMounted;
    private long mSdcSize;
    private long mSdcFree;
    private int mClientCount;
    private Calendar mDate;

    static Heartbeat fromBytes(byte[] data) throws LwcommsException {
        ByteBuffer buf;
        Heartbeat hb;

        if (data.length != LEN) {
            throw new LwcommsException("incorrect data length for heartbeat");
        }

        buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        hb = new Heartbeat();
        hb.mSdcMounted = (buf.getInt() == 1);
        hb.mSdcSize = buf.getLong();
        hb.mSdcFree = buf.getLong();
        hb.mClientCount = buf.getInt();

        // cam returns epoch in GMT+8, so need TZ=-8 to get back to UTC
        hb.mDate = Calendar.getInstance(TimeZone.getTimeZone("GMT:-8:00"));
        hb.mDate.setTimeInMillis(buf.getLong() * 1000);

        return hb;
    }

    public int getClientCount() {
        return mClientCount;
    }

    public Calendar getDate() {
        return mDate;
    }

    public long getSDCardFree() {
        return mSdcFree;
    }

    public boolean getSDCardMounted() {
        return mSdcMounted;
    }

    public long getSDCardSize() {
        return mSdcSize;
    }
}
