package com.aabhix.camera;

import android.content.SharedPreferences;
import android.graphics.*;

/** Opt-in, original text overlay shared by JPEG and GPU video compositing. */
public final class WatermarkSettings {
    public final boolean photo,video;public final String text;public final float size,opacity;public final int position;
    public WatermarkSettings(boolean photo,boolean video,String text,float size,float opacity,int position){
        if(text==null||text.length()>80||!Float.isFinite(size)||size<1||size>10||!Float.isFinite(opacity)||opacity<0||opacity>1||position<0||position>3)throw new IllegalArgumentException("Invalid watermark settings");
        this.photo=photo;this.video=video;this.text=text;this.size=size;this.opacity=opacity;this.position=position;
    }
    public static WatermarkSettings off(){return new WatermarkSettings(false,false,"",3,1,0);}
    public static WatermarkSettings load(SharedPreferences prefs){return new WatermarkSettings(prefs.getBoolean("watermarkPhoto",false),prefs.getBoolean("watermarkVideo",false),prefs.getString("watermarkText",""),prefs.getFloat("watermarkSize",3),prefs.getFloat("watermarkOpacity",1),prefs.getInt("watermarkPosition",0));}
    public void draw(Canvas canvas,int width,int height){
        if(text.isEmpty()||opacity==0)return;Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(width*size/100);float margin=width*.04f;
        float measured=paint.measureText(text);if(measured>width-2*margin)paint.setTextSize(paint.getTextSize()*(width-2*margin)/measured);
        paint.setColor(Color.WHITE);paint.setAlpha(Math.round(opacity*255));paint.setShadowLayer(Math.max(1,width/500f),0,1,0xcc000000);
        float x=position==0||position==2?width-margin-paint.measureText(text):margin;
        float y=position<2?height-margin-paint.getFontMetrics().descent:margin-paint.getFontMetrics().ascent;
        canvas.drawText(text,x,y,paint);
    }
}
