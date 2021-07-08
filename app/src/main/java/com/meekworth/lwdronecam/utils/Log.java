package com.meekworth.lwdronecam.utils;

/*
 * This is a wrapper around android.util.Log to provide string format args
 * in the high-level call.
 */
public class Log {
    public static void e(String tag, String fmt, Object... args) {
        android.util.Log.e(tag, String.format(fmt, args));
    }

    public static void i(String tag, String fmt, Object... args) {
        android.util.Log.i(tag, String.format(fmt, args));
    }

    public static void d(String tag, String fmt, Object... args) {
        android.util.Log.d(tag, String.format(fmt, args));
    }

    public static void v(String tag, String fmt, Object... args) {
        android.util.Log.v(tag, String.format(fmt, args));
    }
}
