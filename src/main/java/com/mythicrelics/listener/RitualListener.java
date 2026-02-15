package com.mythicrelics.listener;

import com.mythicrelics.manager.RitualManager;
import org.bukkit.event.Listener;

public class RitualListener implements Listener {
    private final RitualManager ritualManager;

    public RitualListener(RitualManager ritualManager) {
        this.ritualManager = ritualManager;
    }
}
