package com.aabhix.camera.domain;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonitorMathTest {
    @Test public void shutterUsesMeasuredNanoseconds(){assertEquals("1/125",MonitorMath.shutter(8000000L));assertEquals("2.0s",MonitorMath.shutter(2000000000L));assertEquals("—",MonitorMath.shutter(null));assertEquals("—",MonitorMath.shutter(0L));}
    @Test public void elapsedTimeDoesNotInventFrameTimecode(){assertEquals("01:01:01",MonitorMath.timecode(3661999));assertEquals("00:00:00",MonitorMath.timecode(-2));}
    @Test public void rgbHistogramPreservesChannelCounts(){int[][] b=MonitorMath.histogram(new int[]{0xff000000,0xffffffff,0xffff0000});assertEquals(2,b[0][63]);assertEquals(1,b[1][63]);assertEquals(2,b[1][0]);for(int[] channel:b){int n=0;for(int count:channel)n+=count;assertEquals(3,n);}}
}
