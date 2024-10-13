/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.xiaomiparts.perf;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class PerfModeTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        final PerfModeUtils utils = PerfModeUtils.getInstance(this);
        if (utils.isPerformanceModeOn()) {
            utils.turnOffPerformanceMode();
        } else {
            utils.turnOnPerformanceMode();
        }
        updateTileState();
    }

    private void updateTileState() {
        final Tile tile = getQsTile();
        tile.setState(PerfModeUtils.getInstance(this).isPerformanceModeOn()
                ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
