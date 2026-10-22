package com.aabhix.camera;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Small, sampled RGB histogram. Viewfinder only; never baked into media. */
public final class MonitorOverlay extends View {
    private int[][] bins;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public MonitorOverlay(Context context){super(context);setContentDescription("RGB histogram of ungraded preview samples");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    public void histogram(int[][] value){bins=value;invalidate();}
    @Override protected void onDraw(Canvas canvas){if(bins==null)return;float d=getResources().getDisplayMetrics().density,w=128*d,h=60*d,left=10*d,top=getHeight()-h-62*d;paint.setColor(0xbb080808);canvas.drawRoundRect(left,top,left+w,top+h,6*d,6*d,paint);int[] colors={0xbaff5555,0xba55ff77,0xba5599ff};int max=1;for(int[] channel:bins)for(int value:channel)max=Math.max(max,value);for(int c=0;c<3;c++){Path path=new Path();path.moveTo(left,top+h-12*d);for(int i=0;i<64;i++)path.lineTo(left+i*w/63,top+h-12*d-bins[c][i]*(h-18*d)/max);paint.setColor(colors[c]);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);canvas.drawPath(path,paint);}paint.setStyle(Paint.Style.FILL);paint.setTextSize(8*d);paint.setColor(Color.WHITE);canvas.drawText("RGB · sampled / ungraded",left+4*d,top+h-3*d,paint);}
}
