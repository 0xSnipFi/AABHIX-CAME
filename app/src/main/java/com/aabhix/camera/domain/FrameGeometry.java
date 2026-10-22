package com.aabhix.camera.domain;

/** Platform-independent geometry shared by preview, JPEG crop and focus mapping. */
public final class FrameGeometry {
    private FrameGeometry() {}
    private static void validate(int width,int height,float aspect){if(width<=0||height<=0||!Float.isFinite(aspect)||aspect<=0)throw new IllegalArgumentException("Invalid frame geometry");}
    public static int[] fit(int width,int height,float aspect){
        validate(width,height,aspect);int w=width,h=Math.round(width/aspect);if(h>height){h=height;w=Math.round(height*aspect);}return new int[]{(width-w)/2,(height-h)/2,w,h};
    }
    public static int[] crop(int width,int height,float aspect){
        validate(width,height,aspect);int w=width,h=height;if(width/(float)height>aspect)w=Math.max(1,Math.round(height*aspect));else h=Math.max(1,Math.round(width/aspect));return new int[]{(width-w)/2,(height-h)/2,w,h};
    }
    public static float[] cropScale(float sourceAspect,float targetAspect){if(sourceAspect>targetAspect)return new float[]{targetAspect/sourceAspect,1};return new float[]{1,sourceAspect/targetAspect};}
    public static int rotation(int sensorDegrees,int displayDegrees,boolean front){return (sensorDegrees+(front?displayDegrees:-displayDegrees)+360)%360;}
    public static float orientedAspect(float sensorAspect,int rotation){if(!Float.isFinite(sensorAspect)||sensorAspect<=0||rotation%90!=0)throw new IllegalArgumentException("Invalid orientation");return Math.floorMod(rotation,180)==0?sensorAspect:1/sensorAspect;}
    public static int[] previewBounds(int width,int height,float sourceAspect,float captureAspect,boolean fullSensor){return fit(width,height,fullSensor?sourceAspect:captureAspect);}
    public static int[] captureBounds(int[] previewBounds,float captureAspect){int[] inner=fit(previewBounds[2],previewBounds[3],captureAspect);inner[0]+=previewBounds[0];inner[1]+=previewBounds[1];return inner;}
    public static float[] sensorPoint(float x,float y,int rotation,boolean mirror){x=Math.max(0,Math.min(1,x));y=Math.max(0,Math.min(1,y));if(mirror)x=1-x;switch(rotation){case 90:return new float[]{y,1-x};case 180:return new float[]{1-x,1-y};case 270:return new float[]{1-y,x};default:return new float[]{x,y};}}
}
