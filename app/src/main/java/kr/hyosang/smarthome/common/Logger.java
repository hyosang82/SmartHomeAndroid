package kr.hyosang.smarthome.common;

import android.util.Log;

/**
 * Created by Hyosang on 2016-01-07.
 */
public class Logger {
    public static final String TAG = "SmartHome";

    public static void d(String str) {
        Log.d(TAG, str);
    }

    public static void e(Throwable th) {
        Log.e(TAG, Log.getStackTraceString(th));
    }

    public static void w(Throwable th) {
        Log.w(TAG, Log.getStackTraceString(th));
    }
}
