package com.aabhix.camera;
import android.graphics.*;import com.aabhix.camera.domain.*;
/** Fuses lossless right-angle geometry and affine color into one native raster pass. */
public final class PhotoRasterizer {
    private PhotoRasterizer(){}
    public static Matrix orientation(int orientation){Matrix matrix=new Matrix();switch(orientation){case 2:matrix.setScale(-1,1);break;case 3:matrix.setRotate(180);break;case 4:matrix.setScale(1,-1);break;case 5:matrix.setRotate(90);matrix.postScale(-1,1);break;case 6:matrix.setRotate(90);break;case 7:matrix.setRotate(270);matrix.postScale(-1,1);break;case 8:matrix.setRotate(270);break;}return matrix;}
    public static Bitmap render(Bitmap source,int orientation,float aspect,Style style,float intensity){
        if(style.lut!=null)throw new IllegalArgumentException("LUT requires its separate sampling path");Matrix transform=orientation(orientation);RectF bounds=new RectF(0,0,source.getWidth(),source.getHeight());transform.mapRect(bounds);int width=Math.round(bounds.width()),height=Math.round(bounds.height());transform.postTranslate(-bounds.left,-bounds.top);int[] crop=aspect>0?FrameGeometry.crop(width,height,aspect):new int[]{0,0,width,height};transform.postTranslate(-crop[0],-crop[1]);Bitmap output=Bitmap.createBitmap(crop[2],crop[3],Bitmap.Config.ARGB_8888);try{Paint paint=new Paint();if(!style.id.equals("original")&&intensity!=0)paint.setColorFilter(new ColorMatrixColorFilter(ColorEngine.matrix(style,intensity)));new Canvas(output).drawBitmap(source,transform,paint);return output;}catch(RuntimeException|Error e){output.recycle();throw e;}
    }
}
