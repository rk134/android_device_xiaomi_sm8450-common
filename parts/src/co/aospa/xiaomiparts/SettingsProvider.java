/*
 * Copyright (C) 2019 The Android Open Source Project
 *           (C) 2023 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package co.aospa.xiaomiparts;

import static com.android.settingslib.drawer.EntriesProvider.METHOD_GET_DYNAMIC_SUMMARY;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_KEYHINT;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SUMMARY;

import android.content.ContentProvider;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import co.aospa.xiaomiparts.R;
import co.aospa.xiaomiparts.gestures.GestureUtils;
import co.aospa.xiaomiparts.perf.PerfModeSwitchController;
import co.aospa.xiaomiparts.perf.PerfModeUtils;
import com.android.settingslib.drawer.EntriesProvider;
import com.android.settingslib.drawer.EntryController;

import java.util.Arrays;
import java.util.List;

/** Provide settings functions for injected preferences. */
public class SettingsProvider extends EntriesProvider {

    private static final String TAG = "XiaomiPartsSettingsProvider";
    private static final Boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String KEY_FP_DOUBLE_TAP = "fp_double_tap";
    private static final String KEY_PERF_MODE = PerfModeUtils.PREF_KEY;

    @Override
    protected List<EntryController> createEntryControllers() {
        return List.of(
            new PerfModeSwitchController(getContext())
        );
    }

    @Override
    public Bundle call(String method, String uri, Bundle extras) {
        if (DEBUG) Log.d(TAG, "Called method: " + method + " uri: " + uri + " extras:" + extras);

        final String key = extras != null && extras.containsKey(META_DATA_PREFERENCE_KEYHINT)
                ? extras.getString(META_DATA_PREFERENCE_KEYHINT)
                : getKeyFromUriStr(uri);

        if (key != null) {
            switch (key) {
                case KEY_FP_DOUBLE_TAP:
                    if (method.equals(METHOD_GET_DYNAMIC_SUMMARY)) {
                        return getDynamicSummary(key);
                    }
                    Log.w(TAG, "Unsupported method: " + method + " for key: " + key);
                    return null;
            }
        }

        return super.call(method, uri, extras);
    }

    private static String getKeyFromUriStr(String uri) {
        if (uri == null) {
            return null;
        }
        final List<String> pathSegments = Uri.parse(uri).getPathSegments();
        if (pathSegments == null || pathSegments.size() < 2) {
            return null;
        }
        return pathSegments.get(1);
    }

    private Bundle getDynamicSummary(String key) {
        String summary;
        switch (key) {
            case KEY_FP_DOUBLE_TAP:
                summary = getFpDoubleTapSummary();
                break;
            default:
                return null;
        }

        Bundle bundle = new Bundle();
        bundle.putString(META_DATA_PREFERENCE_SUMMARY, summary);
        return bundle;
    }

    private String getFpDoubleTapSummary() {
        if (!GestureUtils.isFpDoubleTapEnabled(getContext())) {
            return getContext().getString(R.string.fp_double_tap_summary_off);
        }
        final int action = GestureUtils.getFpDoubleTapAction(getContext());
        final List<String> actions = Arrays.asList(getContext().getResources().getStringArray(
                R.array.fp_double_tap_action_values));
        final int actionIndex = actions.indexOf(Integer.toString(action));
        final String actionName = getContext().getResources().getStringArray(
                R.array.fp_double_tap_action_entries)[actionIndex];
        return getContext().getString(R.string.fp_double_tap_summary_on, actionName);
    }

}
