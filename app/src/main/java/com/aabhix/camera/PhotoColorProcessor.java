package com.aabhix.camera;

import android.graphics.*;
import com.aabhix.camera.domain.*;

/** Bounded-memory strip processing; never reduces capture resolution. */
public final class PhotoColorProcessor {
    private PhotoColorProcessor(){}
    public static void apply(Bitmap image,Style style,float intensity){
        if(intensity==0||style.id.equals("original"))return;
        int width=image.getWidth(),height=image.getHeight(),strip=Math.min(128,height);
        int[] pixels=new int[width*strip];float[] matrix=ColorEngine.matrix(style,intensity),scratch=new float[3];
        if(style.lut!=null){for(int y=0;y<height;y+=strip){int rows=Math.min(strip,height-y);image.getPixels(pixels,0,width,0,y,width,rows);for(int i=0;i<width*rows;i++)pixels[i]=ColorEngine.apply(pixels[i],matrix,style.lut,intensity,scratch);image.setPixels(pixels,0,width,0,y,width,rows);}return;}
        Bitmap band=Bitmap.createBitmap(width,strip,Bitmap.Config.ARGB_8888);
        try{Canvas canvas=new Canvas(band);Paint paint=new Paint();paint.setColorFilter(new ColorMatrixColorFilter(matrix));paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            for(int y=0;y<height;y+=strip){int rows=Math.min(strip,height-y);canvas.drawBitmap(image,new Rect(0,y,width,y+rows),new Rect(0,0,width,rows),paint);band.getPixels(pixels,0,width,0,0,width,rows);image.setPixels(pixels,0,width,0,y,width,rows);}
        }finally{band.recycle();}
    }
}
