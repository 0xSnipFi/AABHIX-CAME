package com.aabhix.camera.domain;

/** Log scales provide useful resolution across a camera's actual ISO/exposure range. */
public final class ProValues {
    private ProValues(){}
    public static long logarithmic(int progress,long minimum,long maximum){if(minimum<=0||maximum<minimum)throw new IllegalArgumentException("Invalid manual range");if(progress<=0)return minimum;if(progress>=1000)return maximum;return Math.max(minimum,Math.min(maximum,Math.round(Math.exp(Math.log(minimum)+(Math.log(maximum)-Math.log(minimum))*progress/1000.0))));}
    public static int progress(long value,long minimum,long maximum){if(minimum<=0||maximum<minimum)throw new IllegalArgumentException("Invalid manual range");if(minimum==maximum)return 0;return (int)Math.max(0,Math.min(1000,Math.round((Math.log(Math.max(minimum,Math.min(maximum,value)))-Math.log(minimum))/(Math.log(maximum)-Math.log(minimum))*1000)));}
}
