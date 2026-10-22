package com.aabhix.camera.domain;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.StringReader;
public class ColorEngineTest {
    @Test public void identityAndZeroIntensityPreservePixels() {
        int pixel = 0xff4388de;
        assertEquals(pixel, ColorEngine.apply(pixel, Style.identity(), 1));
        for (Style s : Style.builtins()) assertEquals(pixel, ColorEngine.apply(pixel, s, 0));
    }
    @Test public void monochromeHasEqualChannelsAndOpaqueAlpha() {
        int p=ColorEngine.apply(0xffea4521,Style.builtins().stream().filter(s->s.id.equals("silver")).findFirst().get(),1);
        assertEquals((p>>16)&255,(p>>8)&255); assertEquals(p&255,(p>>8)&255); assertEquals(255,p>>>24);
    }
    @Test public void allStylesBoundedAndDeterministic() {
        assertTrue(Style.builtins().size()>=30);
        java.util.Set<String> ids=new java.util.HashSet<>();
        for(Style s:Style.builtins()) {
            assertTrue(ids.add(s.id));
            for(int rgb:new int[]{0xff000000,0xffffffff,0xffff0000,0xff00ff00,0xff0000ff,0xff326591}) {
                int a=ColorEngine.apply(rgb,s,1); assertEquals(255,a>>>24); assertEquals(a,ColorEngine.apply(rgb,s,1));
            }
        }
    }
    @Test public void cubeIdentityUsesRedFastestAndTrilinearInterpolation() throws Exception {
        CubeLut lut=CubeLut.parse(new StringReader("LUT_3D_SIZE 2\n0 0 0\n1 0 0\n0 1 0\n1 1 0\n0 0 1\n1 0 1\n0 1 1\n1 1 1\n"));
        float[] out=lut.sample(.2f,.4f,.8f); assertArrayEquals(new float[]{.2f,.4f,.8f},out,.00001f);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsIncompleteCube() throws Exception { CubeLut.parse(new StringReader("LUT_3D_SIZE 64\n0 0 0\n")); }
    @Test(expected=IllegalArgumentException.class) public void rejectsUnboundedCube() throws Exception { CubeLut.parse(new StringReader("LUT_3D_SIZE 999999\n")); }
    @Test(expected=IllegalArgumentException.class) public void rejectsNanCube() throws Exception { CubeLut.parse(new StringReader("LUT_3D_SIZE 2\nNaN 0 0\n")); }
}
