package com.aabhix.camera.domain;
import org.junit.Test;import static org.junit.Assert.*;
public class CapturePolicyTest {
    @Test public void videoUsesVideoAf(){assertEquals(3,CapturePolicy.focusMode(new int[]{0,1,3,4},true,false,false));assertEquals(4,CapturePolicy.focusMode(new int[]{0,1,3,4},false,false,false));}
    @Test public void tapAndManualRespectCapabilities(){assertEquals(1,CapturePolicy.focusMode(new int[]{0,1,3},true,false,true));assertEquals(0,CapturePolicy.focusMode(new int[]{0},true,false,true));assertEquals(0,CapturePolicy.focusMode(new int[]{0,3},true,true,false));}
    @Test public void fixedRateBeatsLowLightRange(){assertEquals(1,CapturePolicy.fpsRange(new int[]{5,30,15},new int[]{30,30,30},30));assertEquals(1,CapturePolicy.fpsRange(new int[]{5,15},new int[]{30,30},30));}
    @Test public void stableVideoCapsExposure(){assertEquals(33333333L,CapturePolicy.exposure(400000000,30,true));assertEquals(400000000L,CapturePolicy.exposure(400000000,30,false));}
}
