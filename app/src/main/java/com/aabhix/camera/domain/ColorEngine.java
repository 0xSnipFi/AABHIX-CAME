package com.aabhix.camera.domain;

/** Shared affine sRGB transform: GPU and CPU use identical coefficients. */
public final class ColorEngine {
    private ColorEngine(){}
    public static float[] matrix(Style style,float intensity) {
        float a=Math.max(0,Math.min(1,intensity)); float s=1+(style.saturation-1)*a;
        float c=1+(style.contrast-1)*a; float f=style.fade*a;
        float scale=c*(1-f); float[] l={.2126f,.7152f,.0722f}; float[] m=new float[20];
        for(int r=0;r<3;r++) { for(int k=0;k<3;k++) m[r*5+k]=scale*((1-s)*l[k]+(r==k?s:0));
            m[r*5+4]=( .5f*(1-c)*(1-f)+f+style.exposure*a + (r==0?style.warmth*a:r==2?-style.warmth*a:style.tint*a))*255;
        }
        m[18]=1; return m;
    }
    public static int apply(int pixel,Style style,float intensity) {return apply(pixel,matrix(style,intensity),style.lut,intensity);}
    public static int apply(int pixel,float[] m,CubeLut lut,float intensity) {
        return apply(pixel,m,lut,intensity,lut==null?null:new float[3]);
    }
    public static int apply(int pixel,float[] m,CubeLut lut,float intensity,float[] scratch) {
        float r=(pixel>>16)&255,g=(pixel>>8)&255,b=pixel&255;
        float rr=clamp((m[0]*r+m[1]*g+m[2]*b+m[4])/255);
        float gg=clamp((m[5]*r+m[6]*g+m[7]*b+m[9])/255);
        float bb=clamp((m[10]*r+m[11]*g+m[12]*b+m[14])/255);
        if(lut!=null && intensity>0){lut.sampleInto(rr,gg,bb,scratch);float a=clamp(intensity);rr=rr*(1-a)+scratch[0]*a;gg=gg*(1-a)+scratch[1]*a;bb=bb*(1-a)+scratch[2]*a;}
        return (pixel&0xff000000)|(Math.round(clamp(rr)*255)<<16)|(Math.round(clamp(gg)*255)<<8)|Math.round(clamp(bb)*255);
    }
    private static float clamp(float x){return Math.max(0,Math.min(1,x));}
}
