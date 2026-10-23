package com.aabhix.camera.domain;

import java.io.StringReader;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class CubeSamplingTest {
    @Test public void optimizedSamplerMatchesReferenceIncludingBoundaries()throws Exception {
        Random random=new Random(11);
        for(int size:new int[]{2,17,33}) {
            StringBuilder cube=new StringBuilder("LUT_3D_SIZE "+size+"\n");
            for(int i=0;i<size*size*size;i++)cube.append(random.nextFloat()).append(' ').append(random.nextFloat()).append(' ').append(random.nextFloat()).append('\n');
            CubeLut lut=CubeLut.parse(new StringReader(cube.toString()));
            for(int i=0;i<10000;i++) {
                float r=i<3?i-1:random.nextFloat(),g=i<3?i-1:random.nextFloat(),b=i<3?i-1:random.nextFloat();
                float[] expected=reference(lut,r,g,b),actual=lut.sample(r,g,b);
                assertArrayEquals(expected,actual,0.0000001f);
            }
        }
    }
    private static float[] reference(CubeLut lut,float r,float g,float b) {
        float xp=Math.max(0,Math.min(1,r))*(lut.size-1),yp=Math.max(0,Math.min(1,g))*(lut.size-1),zp=Math.max(0,Math.min(1,b))*(lut.size-1);
        int xl=(int)xp,yl=(int)yp,zl=(int)zp;float[] result=new float[3];
        for(int dz=0;dz<2;dz++)for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++) {
            int x=Math.min(lut.size-1,xl+dx),y=Math.min(lut.size-1,yl+dy),z=Math.min(lut.size-1,zl+dz);
            float w=(dx==0?1-xp+xl:xp-xl)*(dy==0?1-yp+yl:yp-yl)*(dz==0?1-zp+zl:zp-zl);
            int at=3*(x+lut.size*(y+lut.size*z));for(int c=0;c<3;c++)result[c]+=lut.data[at+c]*w;
        }
        return result;
    }
}
