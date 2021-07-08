package com.meekworth.lwdronecam.lwcomms;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class StreamFrame implements Response {
    private static final String TAG = "LWDroneCam/lwcomms.StreamFrame";
    private static final int HDR_LEN = 0x20;
    private static final int LEN_OFF = 4;
    private static final int COUNT_OFF = 8;
    private static final int FRAME_OFF = HDR_LEN;

    private int mLen;
    private long mCount;
    private byte[] mBytes;

    public byte[] getBytes() {
        return mBytes;
    }

    public static StreamFrame fromBytes(Command resp) throws LwcommsException {
        byte[] data = resp.getBody();
        StreamFrame frame;
        ByteBuffer buf;

        if (data.length < HDR_LEN) {
            throw new LwcommsException("not enough data for frame header");
        }
        frame = new StreamFrame();
        buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        frame.mLen = buf.getInt(LEN_OFF);
        frame.mCount = buf.getLong(COUNT_OFF);

        if (data.length != HDR_LEN + frame.mLen) {
            throw new LwcommsException("not right amount of data for frame");
        }
        frame.mBytes = new byte[frame.mLen];
        buf.position(FRAME_OFF);
        buf.get(frame.mBytes);
        Unmunger.unmunge(
                frame.mBytes,
                resp.getArgument(Command.ARG_STREAM_TYPE),
                frame.mCount,
                resp.getArgument(Command.ARG_STREAM_DEC1),
                resp.getArgument(Command.ARG_STREAM_DEC2));

        return frame;
    }

    private static class Unmunger {
        private static final int[] MDATA1 = {
                0x0000, 0x0001, 0x6108, 0x177b, 0x5351, 0x7162, 0x7c1b, 0x4aab,
                0x687a, 0x863b, 0x026d, 0x0178, 0x06e7, 0x7b86, 0x55ff, 0x0ae5,
                0x2743, 0x4527, 0x086d, 0x47fe, 0x3c7e, 0x6c7a, 0x3fd9, 0x2ec7,
                0x4a87, 0x40ae, 0x7535, 0x7c27, 0x0c9f, 0x7384, 0x0d69, 0x1381,
                0x2050, 0x20e8, 0x632a, 0x7a1f, 0x7a7a, 0x1f20, 0x0004, 0x531c,
                0x4075, 0x03ef, 0x22c6, 0x2167, 0x818b, 0x44a3, 0x559e, 0x1547,
                0x1cb6, 0x1e5a, 0x2cba, 0x4b8d, 0x1d68, 0x774f, 0x47cf, 0x1b86,
                0x15f1, 0x6816, 0x458e, 0x3007, 0x6aaf, 0x00c7, 0x179d, 0x43a6,
                0x1e50, 0x17b4, 0x49ab, 0x47ae, 0x0d03, 0x7eea, 0x35b0, 0x4c99,
                0x57c8, 0x6951, 0x5109, 0x7f66, 0x7142, 0x26b4, 0x2edd, 0x2416,
                0x0b4a, 0x0f53, 0x849e, 0x3e7d, 0x020d, 0x87ba, 0x1b9d, 0x31e8,
                0x729d, 0x4046, 0x485d, 0x295e, 0x5382, 0x0720, 0x610d, 0x68e0,
                0x24c5, 0x70d0, 0x6d8d, 0x2087, 0x8413, 0x279f, 0x19af, 0x3eb7,
                0x8122, 0x2512, 0x0774, 0x31c5, 0x704a, 0x6d26, 0x577e, 0x2d41,
                0x6455, 0x4dd8, 0x5dec, 0x34b5, 0x3cbb, 0x88c6, 0x4ba0, 0x0abf,
                0x19ea, 0x0284, 0x4484, 0x0641, 0x7cc3, 0x31b8, 0x7c12, 0x7d6e,
                0x6546, 0x8071, 0x141c, 0x300a, 0x797a, 0x0817, 0x473b, 0x891a,
                0x68b7, 0x6593, 0x5d8a, 0x4499, 0x4f57, 0x54df, 0x371c, 0x81b1,
                0x6649, 0x7e94, 0x0b13, 0x44dd, 0x3cde, 0x07f6, 0x1f42, 0x2108,
                0x3a66, 0x3727, 0x25a3, 0x1f69, 0x155e, 0x6b00, 0x20ca, 0x2377,
                0x37c8, 0x05f7, 0x63f9, 0x7827, 0x4121, 0x6ec5, 0x21e6, 0x3edb,
                0x5afa, 0x39f9, 0x4053, 0x3eda, 0x7846, 0x168f, 0x143c, 0x890d,
                0x3903, 0x51d1, 0x7b90, 0x2f6c, 0x09ef, 0x390f, 0x6b64, 0x894e,
                0x30d5, 0x533d, 0x6bf7, 0x1a55, 0x12bd, 0x6a30, 0x293b, 0x7622,
                0x6ced, 0x5873, 0x80f5, 0x6248, 0x3840, 0x7ee8, 0x071f, 0x0cc9,
                0x2e47, 0x0c49, 0x1a01, 0x490e, 0x0dba, 0x6a9c, 0x1aef, 0x7bfc,
                0x606b, 0x8414, 0x157c, 0x4165, 0x0c93, 0x19f9, 0x3bfb, 0x4fdc,
                0x2d87, 0x6143, 0x5eaf, 0x25c9, 0x4485, 0x1d81, 0x25ab, 0x5b52,
                0x1ade, 0x0c06, 0x5585, 0x6027, 0x541f, 0x8464, 0x323e, 0x6ad8,
                0x49b8, 0x1c37, 0x5dc0, 0x6b0b, 0x52a4, 0x2a5c, 0x24ee, 0x5a6e,
                0x67cb, 0x1faa, 0x5d46, 0x215f, 0x57ef, 0x231e, 0x2f12, 0x56e8,
                0x5afd, 0x0b0c, 0x237a, 0x0b28, 0x5fa8, 0x8637, 0x35de, 0x2e8e
        };
        private static final int[] MDATA2 = {
                0x0000, 0x0001, 0x7864, 0x7401, 0x06e6, 0x4b4a, 0x2ae2, 0x0bbb,
                0x0013, 0x398b, 0x758b, 0x472b, 0x56d8, 0x4b4f, 0x52b2, 0x5996,
                0x587c, 0x0e21, 0x1f34, 0x5626, 0x3e33, 0x6701, 0x5e13, 0x34f0,
                0x0523, 0x6182, 0x2054, 0x043d, 0x64f8, 0x54d6, 0x8721, 0x7ad8,
                0x8312, 0x155d, 0x3f44, 0x0ecb, 0x3975, 0x7211, 0x0f3f, 0x77df,
                0x3828, 0x65ba, 0x01cf, 0x2610, 0x4454, 0x8383, 0x3b8f, 0x885a,
                0x7f9b, 0x2b99, 0x43d2, 0x015e, 0x60b1, 0x2690, 0x3dea, 0x682a,
                0x5572, 0x17cd, 0x6ae8, 0x3e2f, 0x16b3, 0x17f8, 0x17bc, 0x2f82,
                0x0169, 0x3da7, 0x6d6a, 0x01bc, 0x4c2b, 0x777e, 0x3411, 0x3b68,
                0x8348, 0x6aff, 0x70bf, 0x651f, 0x8896, 0x082c, 0x4aad, 0x7c1f,
                0x2261, 0x7080, 0x4d84, 0x2fb3, 0x5041, 0x4097, 0x5a87, 0x4da1,
                0x734a, 0x26ca, 0x890f, 0x4972, 0x20c8, 0x3588, 0x3029, 0x59bd,
                0x00d4, 0x33ed, 0x80f4, 0x1fe5, 0x83f0, 0x2cd7, 0x520a, 0x357a,
                0x3887, 0x4507, 0x6bf7, 0x76a7, 0x3f64, 0x5bfa, 0x1917, 0x6848,
                0x3b71, 0x5cb2, 0x38fd, 0x0a60, 0x11a6, 0x06ed, 0x7c54, 0x638f,
                0x34ee, 0x3cb3, 0x0b4f, 0x2de7, 0x47da, 0x629e, 0x6eb5, 0x6e79,
                0x2000, 0x5830, 0x69cd, 0x59fb, 0x46c0, 0x35bc, 0x8134, 0x3514,
                0x8304, 0x18a8, 0x4ea4, 0x22aa, 0x0c00, 0x1a61, 0x0a4a, 0x2fb4,
                0x2b20, 0x5065, 0x25f6, 0x5f25, 0x650f, 0x6d0f, 0x6bf0, 0x054f,
                0x7d0d, 0x3a75, 0x4ed8, 0x79eb, 0x2c9e, 0x262a, 0x5576, 0x5ddc,
                0x5251, 0x6214, 0x4b4c, 0x2094, 0x1767, 0x04d6, 0x6434, 0x0716,
                0x2101, 0x19e5, 0x46d0, 0x8043, 0x0d59, 0x7cbf, 0x4ee4, 0x5268,
                0x00c6, 0x6c6b, 0x5182, 0x592c, 0x7c5b, 0x884f, 0x353e, 0x1f63,
                0x2bc7, 0x8026, 0x5044, 0x016c, 0x0d34, 0x53ab, 0x1430, 0x435d,
                0x6199, 0x0d04, 0x4bd0, 0x88c0, 0x1ea6, 0x0386, 0x4064, 0x370e,
                0x4203, 0x6992, 0x4533, 0x61df, 0x4e15, 0x321e, 0x2c00, 0x181a,
                0x4080, 0x2ebe, 0x5823, 0x355f, 0x47bf, 0x59cc, 0x202c, 0x8589,
                0x5087, 0x1c04, 0x0c5a, 0x87be, 0x4396, 0x873c, 0x3f89, 0x6fd1,
                0x83c5, 0x4519, 0x256b, 0x83de, 0x72ef, 0x3443, 0x56f6, 0x6de1,
                0x641d, 0x52d3, 0x1235, 0x22d6, 0x06c9, 0x3723, 0x3572, 0x6522,
                0x5da5, 0x2f67, 0x03d8, 0x2745, 0x88b3, 0x203c, 0x6a41, 0x6c2e,
                0x8718, 0x8515, 0x25cb, 0x4f71, 0x404e, 0x60c9, 0x7cd9, 0x655b
        };
        private static final int[] MDATA3 = {
                0x0000, 0x0001, 0x48f6, 0x7af3, 0x6e4f, 0x8614, 0x4397, 0x000d,
                0x4ce9, 0x6cf6, 0x85b5, 0x0b27, 0x738b, 0x1679, 0x7be4, 0x490e,
                0x47fa, 0x3675, 0x37bc, 0x7f11, 0x4a19, 0x5692, 0x5b95, 0x56f7,
                0x8148, 0x85ad, 0x1614, 0x1b9c, 0x3bad, 0x14ed, 0x4abc, 0x10de,
                0x02d2, 0x0e9f, 0x0267, 0x5dd0, 0x549e, 0x3bbd, 0x059b, 0x5d95,
                0x27bc, 0x3697, 0x3611, 0x3a9f, 0x46ac, 0x8444, 0x01b5, 0x7b5c,
                0x06f2, 0x00a9, 0x2825, 0x64b6, 0x4a70, 0x4387, 0x2510, 0x287d,
                0x25cc, 0x618f, 0x8009, 0x11c5, 0x7114, 0x56b0, 0x2f88, 0x2a28,
                0x28bb, 0x4a20, 0x0c3d, 0x6cce, 0x43a0, 0x5b01, 0x58fd, 0x12bb,
                0x82ef, 0x5e78, 0x69b4, 0x352d, 0x6ffb, 0x078c, 0x2cbc, 0x4d48,
                0x1140, 0x0c77, 0x0945, 0x169f, 0x7fb9, 0x5644, 0x80c8, 0x392f,
                0x610c, 0x8624, 0x587f, 0x1147, 0x88c5, 0x7e30, 0x2035, 0x6daa,
                0x7cab, 0x25d9, 0x62cf, 0x4741, 0x774d, 0x71ce, 0x194b, 0x87cc,
                0x4164, 0x7d1c, 0x2fa3, 0x1b3e, 0x005d, 0x6af6, 0x507c, 0x2168,
                0x6f18, 0x4e27, 0x68d4, 0x3d13, 0x601e, 0x11a7, 0x869c, 0x14c6,
                0x832f, 0x5cc8, 0x2bf5, 0x148b, 0x691b, 0x42fd, 0x2561, 0x06f9,
                0x44c1, 0x34ee, 0x00ba, 0x759a, 0x228d, 0x02a9, 0x1767, 0x3b8f,
                0x5506, 0x1e0f, 0x81b5, 0x6dc7, 0x013c, 0x63df, 0x4f33, 0x7361,
                0x544c, 0x1c8e, 0x72d6, 0x19f2, 0x5965, 0x45c8, 0x7a18, 0x84b7,
                0x6ac4, 0x6c5a, 0x5b19, 0x4f0c, 0x59d7, 0x4269, 0x8396, 0x0c9e,
                0x3328, 0x1f13, 0x05d8, 0x6ab2, 0x6b69, 0x545f, 0x2dec, 0x0425,
                0x1f74, 0x619d, 0x8753, 0x7363, 0x0d6e, 0x225e, 0x4eb5, 0x5895,
                0x1ddd, 0x4eb4, 0x402e, 0x843a, 0x1035, 0x86e6, 0x0c26, 0x13ed,
                0x6719, 0x54fb, 0x516d, 0x256e, 0x1017, 0x540e, 0x8483, 0x495b,
                0x0af1, 0x57b0, 0x618d, 0x0bb0, 0x589e, 0x5bf4, 0x6ef9, 0x5986,
                0x5923, 0x3940, 0x5ffe, 0x869a, 0x3ecf, 0x6d9d, 0x4d0f, 0x5e6c,
                0x832e, 0x5756, 0x0983, 0x37dd, 0x0d8c, 0x58d2, 0x326a, 0x529c,
                0x331f, 0x51d7, 0x51ac, 0x1c1b, 0x2682, 0x5024, 0x0d6c, 0x1c4d,
                0x24aa, 0x2969, 0x2f47, 0x6ecd, 0x64be, 0x0d79, 0x2d6e, 0x34a4,
                0x773a, 0x048d, 0x0749, 0x8508, 0x17be, 0x7f9a, 0x1f3b, 0x37c2,
                0x510e, 0x05eb, 0x4401, 0x5f7b, 0x0124, 0x7818, 0x7c69, 0x03c0,
                0x3a7e, 0x5ce9, 0x0bae, 0x2843, 0x008e, 0x4f8f, 0x55a9, 0x4878
        };

        private static final int TYPE_NONE = 0;
        private static final int TYPE_OLD_DEC = 1;
        private static final int TYPE_NEW_DEC = 129;

        public static void unmunge(byte[] data, int type, long count, int dec1, int dec2) {
            switch (type) {
                case TYPE_OLD_DEC:
                    int idx = Unmunger.getDecIndex(count, data.length);
                    if (0 <= idx && idx < data.length) {
                        data[idx] = (byte)~data[idx];
                    }
                    break;

                case TYPE_NEW_DEC:
                    Unmunger.fixMidstream(
                            data, dec1 & 0xffff, (dec1 >> 16) & 0xffff, dec2 & 0xffff);
                    break;

                case TYPE_NONE:
                    // fall through
                default:
                    // do nothing
                    break;
            }
        }

        private static int getDecIndex(long p1, long p2) {
            long v1;
            long v2;

            p2 &= 0xffffffff;
            v2 = ((p2 & 1) == 0) ?
                    (p2 + 1 + (p2 ^ p1)) ^ p2 :
                    ((p2 ^ p1) + p2) ^ p2;
            v1 = (p2 != 0) ? (v2 / p2) : 0;

            return (int)(v2 - (v1 * p2));
        }

        private static void fixMidstream(byte[] data, int p1, int p2, int p3) {
            int mid = data.length >> 1;

            p1 &= 0xffff;
            p2 &= 0xffff;
            p3 &= 0xffff;

            for (int i = 0; i < Unmunger.MDATA1.length; i++) {
                if (Unmunger.MDATA1[i] == p1) {
                    data[mid] = (byte)i;
                    break;
                }
            }

            for (int i = 0; i < Unmunger.MDATA2.length; i++) {
                if (Unmunger.MDATA2[i] == p2) {
                    data[mid+1] = (byte)(i ^ data[mid]);
                    break;
                }
            }

            for (int i = 0; i < Unmunger.MDATA3.length; i++) {
                if (Unmunger.MDATA3[i] == p3) {
                    data[mid+2] = (byte)(i ^ data[mid] ^ data[mid+1]);
                    break;
                }
            }
        }
    }
}
