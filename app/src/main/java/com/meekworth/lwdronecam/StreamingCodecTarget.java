package com.meekworth.lwdronecam;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

class StreamingCodecTarget implements StreamingTarget {
    private MediaCodec mCodec;

    StreamingCodecTarget(MediaCodec codec) {
        mCodec = codec;
    }

    @Override
    public void sendFrame(byte[] frameBytes) {
        ByteBuffer buf;
        int bufId;
        int flag;

        // Get the codec input buffer and add the frame data.
        bufId = mCodec.dequeueInputBuffer(-1);
        if (bufId >= 0) {
            if ((buf = mCodec.getInputBuffer(bufId)) != null) {
                buf.put(frameBytes);
            }
            flag = frameBytes.length == 0 ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0;
            mCodec.queueInputBuffer(bufId, 0, frameBytes.length, 0, flag);
        }

        // Get the codec output buffer and release to be processed.
        bufId = mCodec.dequeueOutputBuffer(new MediaCodec.BufferInfo(), 0);
        if (bufId >= 0) {
            mCodec.releaseOutputBuffer(bufId, true);
        }
    }

    @Override
    public void finished() {
        sendFrame(new byte[0]);
        mCodec.flush();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof StreamingCodecTarget)
                && mCodec.equals(((StreamingCodecTarget)o).mCodec);
    }
}
