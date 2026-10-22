package com.aabhix.camera.domain;
import org.junit.Test;
import static org.junit.Assert.*;
public class ProValuesTest {
    @Test public void boundariesAndClampingRespectHardwareRange(){assertEquals(50,ProValues.logarithmic(-2,50,6400));assertEquals(6400,ProValues.logarithmic(1100,50,6400));assertEquals(0,ProValues.progress(20,50,6400));assertEquals(1000,ProValues.progress(999999,50,6400));}
    @Test public void shutterRangeHasAccurateLogarithmicRoundTrip(){for(long ns:new long[]{100000,1000000,8000000,33333333,1000000000}){int position=ProValues.progress(ns,100000,4000000000L);long actual=ProValues.logarithmic(position,100000,4000000000L);assertTrue(Math.abs(actual-ns)/(double)ns<.006);}}
    @Test(expected=IllegalArgumentException.class)public void invalidRangeRejected(){ProValues.logarithmic(500,0,100);}
}
