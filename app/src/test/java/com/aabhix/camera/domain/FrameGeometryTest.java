package com.aabhix.camera.domain;
import org.junit.Test;
import static org.junit.Assert.*;
public class FrameGeometryTest {
    @Test public void fourByThreeFitsWithoutStretching(){assertArrayEquals(new int[]{0,0,900,1200},FrameGeometry.fit(900,1200,.75f));}
    @Test public void squareFitsCentered(){assertArrayEquals(new int[]{0,150,900,900},FrameGeometry.fit(900,1200,1));}
    @Test public void portraitSixteenNineCropsFourByThreeSymmetrically(){assertArrayEquals(new int[]{432,0,2592,4608},FrameGeometry.crop(3456,4608,9f/16));}
    @Test public void noCropForMatchingRatio(){assertArrayEquals(new int[]{0,0,3456,4608},FrameGeometry.crop(3456,4608,.75f));}
    @Test public void uvCropMatchesPixelCrop(){float[] uv=FrameGeometry.cropScale(.75f,9f/16);assertArrayEquals(new float[]{.75f,1},uv,.0001f);}
    @Test public void rotationAccountsForDisplayAndFacing(){assertEquals(0,FrameGeometry.rotation(90,90,false));assertEquals(180,FrameGeometry.rotation(90,90,true));assertEquals(270,FrameGeometry.rotation(90,180,false));}
    @Test public void highResolutionThreeByTwoCropKeepsDetail(){assertArrayEquals(new int[]{192,0,3072,4608},FrameGeometry.crop(3456,4608,2f/3));}
    @Test public void tapFocusUsesSensorRotationAndFrontMirror(){assertArrayEquals(new float[]{.2f,.7f},FrameGeometry.sensorPoint(.3f,.2f,90,false),.0001f);assertArrayEquals(new float[]{.2f,.3f},FrameGeometry.sensorPoint(.3f,.2f,90,true),.0001f);}
    @Test(expected=IllegalArgumentException.class)public void rejectsInvalidDimensions(){FrameGeometry.crop(0,100,1);}
    @Test public void outputAspectFollowsSensorRotationRatherThanWindowShape(){assertEquals(.75f,FrameGeometry.orientedAspect(4f/3,90),.0001f);assertEquals(4f/3,FrameGeometry.orientedAspect(4f/3,0),.0001f);assertEquals(.75f,FrameGeometry.orientedAspect(4f/3,270),.0001f);assertEquals(4f/3,FrameGeometry.orientedAspect(4f/3,180),.0001f);}
    @Test public void sensorFitShowsWholeSensorWithExplicitCaptureGuide(){int[] full=FrameGeometry.previewBounds(1080,1920,.75f,9f/16,true);assertArrayEquals(new int[]{0,240,1080,1440},full);int[] capture=FrameGeometry.captureBounds(full,9f/16);assertArrayEquals(new int[]{135,240,810,1440},capture);}
    @Test public void compositionFitUsesSelectedCropWithoutStretch(){int[] selected=FrameGeometry.previewBounds(1080,1920,.75f,9f/16,false);assertArrayEquals(new int[]{0,0,1080,1920},selected);assertArrayEquals(selected,FrameGeometry.captureBounds(selected,9f/16));}
    @Test public void landscapeSensorFitAndCaptureGuidesHaveCorrectRatio(){int[] full=FrameGeometry.previewBounds(1400,800,4f/3,16f/9,true);int[] capture=FrameGeometry.captureBounds(full,16f/9);assertEquals(4f/3,full[2]/(float)full[3],.002);assertEquals(16f/9,capture[2]/(float)capture[3],.003);assertTrue(capture[0]>=full[0]);assertTrue(capture[1]>=full[1]);assertTrue(capture[1]+capture[3]<=full[1]+full[3]);}
    @Test public void bothFrontSensorMountsCoverAllDisplayRotations(){for(int sensor:new int[]{90,270})for(int display:new int[]{0,90,180,270})for(boolean front:new boolean[]{false,true}){int rotation=FrameGeometry.rotation(sensor,display,front);float a=FrameGeometry.orientedAspect(16f/9,rotation);assertEquals(display%180==0?9f/16:16f/9,a,.0001f);}}
}
