package com.meekworth.lwdronecam;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import com.meekworth.lwdronecam.lwcomms.StreamingTarget;

import java.io.IOException;
import java.nio.ByteBuffer;

public class StreamingCodecTarget implements StreamingTarget {
    private final MediaCodec mCodec;

    StreamingCodecTarget(SurfaceTexture surface, int width, int height) throws IOException {
        mCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        MediaFormat format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height);
        mCodec.configure(format, new Surface(surface), null, 0);
        mCodec.start();
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
        mCodec.stop();
        mCodec.release();
    }
}
