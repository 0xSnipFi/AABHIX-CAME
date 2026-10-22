package com.aabhix.camera.domain;
import org.junit.Test;
import static org.junit.Assert.*;

public class TextureTransformTest {
    @Test public void measuredVivoPortraitDoesNotReceiveAnotherSensorRotation(){float[] measured={0,-1,0,0,-1,0,0,0,0,0,1,0,1,1,0,1};assertArrayEquals(new int[]{0,0},TextureTransform.sampling(measured,90,false));assertArrayEquals(new int[]{90,0},TextureTransform.sampling(measured,0,false));}
    @Test public void allProducerRotationsAndMirrorsComposeToTheDesiredSensorOrientation(){for(int producer:new int[]{0,90,180,270})for(boolean flipped:new boolean[]{false,true})for(int desired:new int[]{0,90,180,270})for(boolean mirror:new boolean[]{false,true}){float[] matrix=producer(producer,flipped);int[] correction=TextureTransform.sampling(matrix,desired,mirror);for(float[] point:new float[][]{{.1f,.2f},{.8f,.3f},{.4f,.9f}}){float[] actual=map(matrix,point[0],point[1],correction[0],correction[1]==1);float x=mirror?1-point[0]:point[0],y=point[1];double angle=Math.toRadians(desired);float expectedX=(float)(Math.cos(angle)*(x-.5)-Math.sin(angle)*(y-.5)+.5),expectedY=(float)(Math.sin(angle)*(x-.5)+Math.cos(angle)*(y-.5)+.5);assertArrayEquals(new float[]{expectedX,1-expectedY},actual,.00001f);}}}
    @Test public void cropScaleAndOffsetDoNotChangeDetectedProducerRotation(){float[] matrix=producer(270,true);for(int i:new int[]{0,4})matrix[i]*=.8f;for(int i:new int[]{1,5})matrix[i]*=.9f;matrix[12]=.1f;matrix[13]=.2f;assertArrayEquals(TextureTransform.sampling(producer(270,true),90,true),TextureTransform.sampling(matrix,90,true));}
    @Test(expected=IllegalArgumentException.class)public void degenerateMatrixIsRejected(){TextureTransform.sampling(new float[16],90,false);}
    public static float[] producer(int degrees,boolean mirror){double angle=Math.toRadians(degrees);float c=Math.round(Math.cos(angle)),s=Math.round(Math.sin(angle)),h=mirror?-1:1;float a=c*h,b=-s,d=-c;return new float[]{a,-s*h,0,0,b,d,0,0,0,0,1,0,.5f-.5f*(a+b),.5f+.5f*(s*h+c),0,1};}
    private static float[] map(float[] m,float x,float y,int shaderRotation,boolean mirror){if(mirror)x=1-x;float a=x,b=y;switch(shaderRotation){case 90:a=y;b=1-x;break;case 180:a=1-x;b=1-y;break;case 270:a=1-y;b=x;break;}return new float[]{m[0]*a+m[4]*b+m[12],m[1]*a+m[5]*b+m[13]};}
}
