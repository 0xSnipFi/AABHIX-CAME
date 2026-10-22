package com.aabhix.camera.domain;

/** Camera2 numeric constants kept independent of Android for deterministic policy tests. */
public final class CapturePolicy {
    private CapturePolicy(){}
    public static int focusMode(int[] modes,boolean video,boolean manual,boolean tap){
        if(manual)return 0;if(tap&&has(modes,1))return 1;
        if(video&&has(modes,3))return 3;if(has(modes,4))return 4;
        if(has(modes,3))return 3;return has(modes,1)?1:0;
    }
    public static int fpsRange(int[] low,int[] high,int fps){
        if(low.length!=high.length)throw new IllegalArgumentException("FPS ranges mismatch");
        for(int i=0;i<low.length;i++)if(low[i]==fps&&high[i]==fps)return i;
        int selected=-1;for(int i=0;i<low.length;i++)if(low[i]<=fps&&high[i]==fps&&(selected<0||low[i]>low[selected]))selected=i;return selected;
    }
    public static long exposure(long requested,int fps,boolean stableVideo){if(fps<=0||requested<=0)throw new IllegalArgumentException("Invalid exposure");return stableVideo?Math.min(requested,1_000_000_000L/fps):requested;}
    private static boolean has(int[] modes,int value){if(modes!=null)for(int mode:modes)if(mode==value)return true;return false;}
}
