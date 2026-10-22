package com.aabhix.camera;
import android.content.SharedPreferences;import android.graphics.*;
/** Optional matte is baked into copies; the original photo remains untouched. */
public final class FrameOverlay {
    public final String kind;
    public FrameOverlay(String kind){this.kind=kind==null?"none":kind;}
    public static FrameOverlay load(SharedPreferences preferences,boolean video){return new FrameOverlay(preferences.getString(video?"videoFrame":"photoFrame","none"));}
    public boolean active(){return kind.equals("cinema239")||kind.equals("cinema235")||kind.equals("cinema185")||kind.equals("white")||kind.equals("black");}
    public void draw(Canvas canvas,int width,int height){if(!active())return;Paint paint=new Paint();paint.setColor(kind.equals("white")?Color.WHITE:Color.BLACK);if(kind.equals("white")||kind.equals("black")){float edge=Math.min(width,height)*.035f;canvas.drawRect(0,0,width,edge,paint);canvas.drawRect(0,height-edge,width,height,paint);canvas.drawRect(0,0,edge,height,paint);canvas.drawRect(width-edge,0,width,height,paint);return;}float aspect=kind.equals("cinema239")?2.39f:kind.equals("cinema235")?2.35f:1.85f;int[] area=com.aabhix.camera.domain.FrameGeometry.fit(width,height,aspect);canvas.drawRect(0,0,width,area[1],paint);canvas.drawRect(0,area[1]+area[3],width,height,paint);canvas.drawRect(0,0,area[0],height,paint);canvas.drawRect(area[0]+area[2],0,width,height,paint);}
}
