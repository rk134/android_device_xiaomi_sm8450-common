package com.qualcomm.qti;

interface IPerfManager {
    int perfLockReleaseHandler(int handle);
    int perfHint(int hint, String pkg_name, int duration, int type, int tid);
}
