/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.perf;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Process;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.util.Log;

import co.aospa.xiaomiparts.thermal.ThermalUtils;
import com.qualcomm.qti.IPerfManager;

public class PerfModeUtils {

    private static final String TAG = "PerfModeUtils";
    public static final String PREF_KEY = "performance_mode";
    private static final String PERF_SERVICE_BINDER_NAME = "vendor.perfservice";
    private static final int PERFORMANCE_MODE_BOOST_ID = 0x00001091;
    private static PerfModeUtils sInstance;
    private final Context mContext;
    private final SharedPreferences mSharedPrefs;
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
