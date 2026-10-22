package com.aabhix.camera.domain;
import org.junit.Test;
import static org.junit.Assert.*;
public class PngLutTest {
    @Test public void tiledIdentityMapsAllChannels(){int[] pixels=new int[512*512];for(int b=0;b<64;b++)for(int g=0;g<64;g++)for(int r=0;r<64;r++)pixels[(b/8*64+g)*512+b%8*64+r]=0xff000000|Math.round(r*255f/63)<<16|Math.round(g*255f/63)<<8|Math.round(b*255f/63);CubeLut lut=CubeLut.tiled64(512,512,pixels);assertArrayEquals(new float[]{.2f,.4f,.8f},lut.sample(.2f,.4f,.8f),.004f);assertArrayEquals(new float[]{1,0,0},lut.sample(1,0,0),0);}
    @Test(expected=IllegalArgumentException.class)public void rejectsOtherLayouts(){CubeLut.tiled64(256,256,new int[256*256]);}
    @Test(expected=IllegalArgumentException.class)public void rejectsTransparency(){CubeLut.tiled64(512,512,new int[512*512]);}
}
