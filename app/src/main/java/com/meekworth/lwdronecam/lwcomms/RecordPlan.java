package com.meekworth.lwdronecam.lwcomms;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.TimeZone;

public class RecordPlan implements Response {
    private static final int PAYLOAD_LEN = 5 * Integer.BYTES;

    private final boolean mActive;
    private final byte mActivevDayBits;
    private final int mStartTimeSecs;
    private final int mEndTimeSecs;
    private final int mMaxDurMins;

    private RecordPlan(boolean active, byte activeDaysBits, int startTimeSecs, int endTimeSecsc,
                       int maxDuration) {
        mActive = active;
        mActivevDayBits = (byte)(activeDaysBits & 0b01111111);
        mStartTimeSecs = startTimeSecs;
        mEndTimeSecs = endTimeSecsc;
        mMaxDurMins = maxDuration;
    }

    public boolean isActive() {
        return mActive;
    }

    public byte[] toBytes() {
        ByteBuffer buf = ByteBuffer.allocate(PAYLOAD_LEN)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(mActive ? 1 : 0)
                .putInt(mActivevDayBits)
                .putInt(mStartTimeSecs)
                .putInt(mEndTimeSecs)
                .putInt(mMaxDurMins);
        return buf.array();
    }

    public static RecordPlan getDefault(boolean active) {
        // TZ on camera is GMT+8
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT:+8:00"));
        // get current weekday, indexing at 0
        int weekday = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;

        return new RecordPlan(
                active,
                (byte)(1 << weekday),   // set current weekday bit
                0,                      // start time = 00:00 of current day
                (60 * 60 * 24) - 1,     // end time = 23:59
                15);                    // def 15 minutes
    }

    public static RecordPlan fromBytes(byte[] data) throws LwcommsException {
        if (data.length != PAYLOAD_LEN) {
            throw new LwcommsException("invalid data length");
        }

        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        boolean active = (buf.getInt() == 1);
        byte dayBits = (byte)buf.getInt();
        int startTimeSecs = buf.getInt();
        int endTimeSecs = buf.getInt();
        int maxDurMins = buf.getInt();

        return new RecordPlan(active, dayBits, startTimeSecs, endTimeSecs, maxDurMins);
    }
}
