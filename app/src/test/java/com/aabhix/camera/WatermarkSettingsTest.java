package com.aabhix.camera;
import org.junit.Test;
import static org.junit.Assert.*;
public class WatermarkSettingsTest {
    @Test public void defaultsNeverEnableWatermarks(){WatermarkSettings s=WatermarkSettings.off();assertFalse(s.photo);assertFalse(s.video);assertEquals("",s.text);}
    @Test public void photoAndVideoAreIndependent(){WatermarkSettings s=new WatermarkSettings(true,false,"Test",3,.5f,2);assertTrue(s.photo);assertFalse(s.video);assertEquals(.5f,s.opacity,0);}
    @Test(expected=IllegalArgumentException.class)public void rejectsNanSize(){new WatermarkSettings(true,true,"Test",Float.NaN,1,0);}
    @Test(expected=IllegalArgumentException.class)public void rejectsOversizeText(){new WatermarkSettings(true,true,new String(new char[81]),3,1,0);}
    @Test(expected=IllegalArgumentException.class)public void rejectsInvalidOpacity(){new WatermarkSettings(true,true,"Test",3,1.1f,0);}
    @Test(expected=IllegalArgumentException.class)public void rejectsUnknownPosition(){new WatermarkSettings(true,true,"Test",3,1,9);}
}
