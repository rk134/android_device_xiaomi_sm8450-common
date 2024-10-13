/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.perf;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Process;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.util.Log;

import co.aospa.xiaomiparts.R;
import co.aospa.xiaomiparts.thermal.ThermalUtils;
import com.qualcomm.qti.IPerfManager;

public class PerfModeUtils {

    private static final String TAG = "PerfModeUtils";
    public static final String PREF_KEY = "performance_mode";
    private static final String PERF_SERVICE_BINDER_NAME = "vendor.perfservice";
    private static final int PERFORMANCE_MODE_BOOST_ID = 0x00001091;
    private static final int NOTIFICATION_ID = 0;

    private static PerfModeUtils sInstance;
    private final Context mContext;
    private final SharedPreferences mSharedPrefs;
    private final NotificationManager mNotificationManager;
    private Notification mNotification;

    private final IPerfManager mPerfManager;
    private int mPerfHandle = -1;

    public static synchronized PerfModeUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PerfModeUtils(context);
        }
        return sInstance;
    }

    private PerfModeUtils(Context context) {
        mContext = context;
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        IBinder perfBinder = ServiceManager.getService(PERF_SERVICE_BINDER_NAME);
        if (perfBinder != null) {
            mPerfManager = IPerfManager.Stub.asInterface(perfBinder);
        } else {
            Log.e(TAG, "Failed to get perf service!");
            mPerfManager = null;
        }

        mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        setupNotification();
    }

    private void setupNotification() {
        final Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        final NotificationChannel channel = new NotificationChannel(TAG /* channel id */,
                mContext.getText(R.string.perf_mode_title),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setBlockable(true);
        mNotificationManager.createNotificationChannel(channel);

        mNotification = new Notification.Builder(mContext, TAG /* channel id */)
                .setContentTitle(mContext.getText(R.string.perf_mode_title))
                .setContentText(mContext.getText(R.string.perf_mode_notification))
                .setSmallIcon(R.drawable.speed_24px)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setFlag(Notification.FLAG_NO_CLEAR, true)
                .build();
    }


    public void onBootCompleted() {
        if (isPerformanceModeOn()) {
            dlog("boot completed, performance mode is enabled");
            turnOnPerformanceMode();
        }
    }

    public Boolean isPerformanceModeOn() {
        return mSharedPrefs.getBoolean(PREF_KEY, false) || mPerfHandle != -1;
    }

    public Boolean turnOnPerformanceMode() {
        if (mPerfManager == null) {
            return false;
        }

        try {
            mPerfHandle = mPerfManager.perfHint(PERFORMANCE_MODE_BOOST_ID,
                    mContext.getPackageName(), Integer.MAX_VALUE, -1, Process.myTid());
            dlog("turnOnPerformanceMode: turn on handle = " + mPerfHandle);

            if (mPerfHandle == -1) {
                Log.e(TAG, "turnOnPerformanceMode: turn on failure");
                return false;
            }

            // side effects
            mSharedPrefs.edit().putBoolean(PREF_KEY, true).commit();
            ThermalUtils tu = ThermalUtils.getInstance(mContext);
            tu.stopService();
            tu.setBenchmarkThermalProfile();
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call perfHint!");
            return false;
        }

        return true;
    }

    public Boolean turnOffPerformanceMode() {
        if (mPerfHandle == -1 || mPerfManager == null) {
            return false;
        }

        int ret = -1;
        dlog("turnOffPerformanceMode: handle = " + mPerfHandle);
        try {
            ret = mPerfManager.perfLockReleaseHandler(mPerfHandle);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call mPerfLockReleaseHandler!");
        }

        if (ret != -1) {
            dlog("turnOffPerformanceMode: turn off success");
            mPerfHandle = -1;

            // side effects
            mSharedPrefs.edit().putBoolean(PREF_KEY, false).commit();
            ThermalUtils tu = ThermalUtils.getInstance(mContext);
            tu.setDefaultThermalProfile();
            tu.startService();
            mNotificationManager.cancel(NOTIFICATION_ID);
        } else {
            Log.e(TAG, "turnOffPerformanceMode: turn off failure");
            return false;
        }

        return true;
    }

    private static void dlog(String msg) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, msg);
        }
    }
}
