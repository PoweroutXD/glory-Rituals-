package com.mythicrelics.util;

public final class TimeUtil {
    private TimeUtil() {}

    public static String formatCooldown(long millisLeft) {
        long totalSeconds = Math.max(0L, millisLeft / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + ":" + String.format("%02d", seconds);
    }
}
