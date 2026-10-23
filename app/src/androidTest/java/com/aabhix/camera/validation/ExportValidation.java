package com.aabhix.camera.validation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.SystemClock;
import com.aabhix.camera.MediaMetadataPolicy;
import com.aabhix.camera.MediaRepository;
import com.aabhix.camera.domain.CubeLut;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import org.json.*;

/** Uses generated pixels only; deletes exactly the test's own MediaStore rows. */
final class ExportValidation {
    static JSONObject run(Context context)throws Exception {
        MediaRepository repository=new MediaRepository(context);
        Method stream=MediaRepository.class.getDeclaredMethod("writeBitmap",String.class,Bitmap.class,int.class);
        Method legacy=MediaRepository.class.getDeclaredMethod("write",String.class,String.class,byte[].class);
        stream.setAccessible(true);legacy.setAccessible(true);
        Bitmap bitmap=Bitmap.createBitmap(4000,3000,Bitmap.Config.ARGB_8888);
        int[] row=new int[4000];Random random=new Random(11);
        for(int y=0;y<3000;y++){for(int x=0;x<4000;x++)row[x]=0xff000000|random.nextInt(0x1000000);bitmap.setPixels(row,0,4000,0,y,4000,1);}
        List<Uri> created=new ArrayList<>();JSONArray samples=new JSONArray();
        try {
            for(int pass=0;pass<2;pass++) {
                long start=SystemClock.elapsedRealtime();ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                if(!bitmap.compress(Bitmap.CompressFormat.JPEG,96,bytes))throw new IOException("Baseline compression failed");
                byte[] encoded=MediaMetadataPolicy.sanitizeJpeg(bytes.toByteArray(),1);
                Uri before=(Uri)legacy.invoke(repository,"Aabhix_validation_legacy_"+System.nanoTime()+".jpg","image/jpeg",encoded);created.add(before);
                long baseline=SystemClock.elapsedRealtime()-start;start=SystemClock.elapsedRealtime();
                Uri after=(Uri)stream.invoke(repository,"Aabhix_validation_stream_"+System.nanoTime()+".jpg",bitmap,96);created.add(after);
                long improved=SystemClock.elapsedRealtime()-start;
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=8;
                Bitmap a,b;try(InputStream in=context.getContentResolver().openInputStream(before)){a=BitmapFactory.decodeStream(in,null,options);}
                try(InputStream in=context.getContentResolver().openInputStream(after)){b=BitmapFactory.decodeStream(in,null,options);}
                if(a==null||b==null)throw new IOException("Published JPEG decode failed");
                try{if(!a.sameAs(b))throw new IOException("Stream export changes decoded pixels");}finally{a.recycle();b.recycle();}
                try(InputStream in=context.getContentResolver().openInputStream(after)){
                    ExifInterface exif=new ExifInterface(in);
                    if(exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH,0)!=4000||exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH,0)!=3000)throw new IOException("Export size mismatch");
                    if(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)!=null||exif.getAttribute(ExifInterface.TAG_MAKE)!=null)throw new IOException("Unexpected source metadata");
                }
                samples.put(new JSONObject().put("legacy_ms",baseline).put("stream_ms",improved).put("jpeg_bytes",encoded.length));
            }
            return new JSONObject().put("passed",true).put("pixels",12000000).put("samples",samples).put("lut_sampling",benchmarkLut()).put("scope","Synthetic encode/publication only; not shutter or LUT latency");
        }finally{bitmap.recycle();for(Uri uri:created)context.getContentResolver().delete(uri,null,null);}
    }
    private static void legacy(CubeLut lut,float r,float g,float b,float[] out){
        float xPos=clamp(r)*(lut.size-1),yPos=clamp(g)*(lut.size-1),zPos=clamp(b)*(lut.size-1);
        int xLo=(int)xPos,yLo=(int)yPos,zLo=(int)zPos;out[0]=out[1]=out[2]=0;
        for(int dz=0;dz<2;dz++)for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++){
            int x=Math.min(lut.size-1,xLo+dx),y=Math.min(lut.size-1,yLo+dy),z=Math.min(lut.size-1,zLo+dz);
            float w=(dx==0?1-xPos+xLo:xPos-xLo)*(dy==0?1-yPos+yLo:yPos-yLo)*(dz==0?1-zPos+zLo:zPos-zLo);
            int at=3*(x+lut.size*(y+lut.size*z));for(int c=0;c<3;c++)out[c]+=lut.data[at+c]*w;
        }
    }
    private static float clamp(float v){return Math.max(0,Math.min(1,v));}
    private static JSONObject benchmarkLut()throws Exception {
        int[] colors=new int[512*512];Random random=new Random(11);for(int i=0;i<colors.length;i++)colors[i]=0xff000000|random.nextInt(0x1000000);
        CubeLut lut=CubeLut.tiled64(512,512,colors);float[] input=new float[12288],out=new float[3];for(int i=0;i<input.length;i++)input[i]=random.nextFloat();
        for(int i=0;i<10000;i++){int j=i%4096*3;legacy(lut,input[j],input[j+1],input[j+2],out);lut.sampleInto(input[j],input[j+1],input[j+2],out);}
        long start=SystemClock.elapsedRealtimeNanos();double baselineSum=0;
        for(int i=0;i<1000000;i++){int j=i%4096*3;legacy(lut,input[j],input[j+1],input[j+2],out);baselineSum+=out[0]+out[1]+out[2];}
        double baselineMs=(SystemClock.elapsedRealtimeNanos()-start)/1000000.0;start=SystemClock.elapsedRealtimeNanos();double optimizedSum=0;
        for(int i=0;i<1000000;i++){int j=i%4096*3;lut.sampleInto(input[j],input[j+1],input[j+2],out);optimizedSum+=out[0]+out[1]+out[2];}
        double optimizedMs=(SystemClock.elapsedRealtimeNanos()-start)/1000000.0;
        if(Math.abs(baselineSum-optimizedSum)>.001)throw new IOException("LUT sampler mismatch");
        return new JSONObject().put("samples",1000000).put("legacy_ms",baselineMs).put("optimized_ms",optimizedMs).put("sum_error",Math.abs(baselineSum-optimizedSum));
    }
}
