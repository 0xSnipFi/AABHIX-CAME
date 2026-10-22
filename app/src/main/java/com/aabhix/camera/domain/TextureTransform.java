package com.aabhix.camera.domain;

/** Compensates the producer's cardinal transform before applying sensor/display framing.
 * SurfaceTexture includes buffer rotation/reflection plus the GL-to-buffer vertical flip.
 * The shader rotation parameter is clockwise in UV sampling coordinates.
 */
public final class TextureTransform {
    private TextureTransform(){}
    public static int[] sampling(float[] matrix,int relativeRotation,boolean desiredMirror){
        if(matrix==null||matrix.length!=16||relativeRotation%90!=0)throw new IllegalArgumentException("Invalid texture transform");
        float a=matrix[0],b=matrix[4],c=-matrix[1],d=-matrix[5];double determinant=(double)a*d-(double)b*c;
        if(!Float.isFinite(a)||!Float.isFinite(b)||!Float.isFinite(c)||!Float.isFinite(d)||Math.abs(determinant)<1e-8)throw new IllegalArgumentException("Degenerate texture transform");
        boolean reflected=determinant<0;double angle=Math.toDegrees(Math.atan2(reflected?-c:c,reflected?-a:a));int producer=Math.floorMod((int)Math.round(angle/90)*90,360);
        int correction=Math.floorMod(reflected?relativeRotation-producer:producer-relativeRotation,360);
        return new int[]{correction,(desiredMirror^reflected)?1:0};
    }
}
