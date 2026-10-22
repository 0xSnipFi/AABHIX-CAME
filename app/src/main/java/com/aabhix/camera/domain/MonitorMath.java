package com.aabhix.camera.domain;

import java.util.Locale;

/** Readouts derived from measured frame metadata and RGB samples, not requested settings. */
public final class MonitorMath {
    private MonitorMath() {}
    public static String shutter(Long ns) {
        if (ns == null || ns <= 0) return "—";
        double seconds=ns/1_000_000_000.0;
        return seconds>=1?String.format(Locale.ROOT,"%.1fs",seconds):"1/"+Math.max(1,Math.round(1/seconds));
    }
    public static String timecode(long milliseconds) {
        long seconds=Math.max(0,milliseconds)/1000;
        return String.format(Locale.ROOT,"%02d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60);
    }
    public static int[][] histogram(int[] pixels) {
        int[][] bins=new int[3][64];
        for(int pixel:pixels){bins[0][(pixel>>>16&255)/4]++;bins[1][(pixel>>>8&255)/4]++;bins[2][(pixel&255)/4]++;}
        return bins;
    }
}
