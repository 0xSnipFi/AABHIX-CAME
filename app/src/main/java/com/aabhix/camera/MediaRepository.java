package com.aabhix.camera;
import android.content.*;
import android.graphics.*;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import com.aabhix.camera.domain.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public final class MediaRepository {
    private final Context context;
    public MediaRepository(Context context){this.context=context;}
    private String name(String suffix){return "Aabhix_"+DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").format(LocalDateTime.now())+suffix;}
    public Uri saveJpeg(byte[] jpeg,Style style,float intensity,boolean preserveOriginal) throws Exception {
        return saveJpeg(jpeg,style,intensity,preserveOriginal,0);
    }
    public Uri saveJpeg(byte[] jpeg,Style style,float intensity,boolean preserveOriginal,float targetAspect) throws Exception {
        return saveJpeg(jpeg,style,intensity,preserveOriginal,targetAspect,WatermarkSettings.off());
    }
    public Uri saveJpeg(byte[] jpeg,Style style,float intensity,boolean preserveOriginal,float targetAspect,WatermarkSettings watermark) throws Exception {
        long started=android.os.SystemClock.elapsedRealtime(),stage=started;org.json.JSONObject timing=new org.json.JSONObject();timing.put("style",style.id);timing.put("photo_watermark",watermark.photo);timing.put("preserve_original",preserveOriginal);
        FrameOverlay frame=FrameOverlay.load(context.getSharedPreferences("settings",0),false);
        int orientation=new ExifInterface(new ByteArrayInputStream(jpeg)).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
        byte[] clean=MediaMetadataPolicy.sanitizeJpeg(jpeg,orientation);
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,bounds);
        boolean swapped=orientation>=5&&orientation<=8;float nativeAspect=swapped?bounds.outHeight/(float)bounds.outWidth:bounds.outWidth/(float)bounds.outHeight;
        boolean crop=targetAspect>0&&Math.abs(nativeAspect-targetAspect)>.003f;
        if((style.id.equals("original")||intensity==0)&&!crop&&!frame.active()&&!(watermark.photo&&!watermark.text.isEmpty()))return write(name(".jpg"),"image/jpeg",clean);
        if(preserveOriginal)write(name("_original.jpg"),"image/jpeg",clean);
        timing.put("metadata_original_ms",android.os.SystemClock.elapsedRealtime()-stage);stage=android.os.SystemClock.elapsedRealtime();
        BitmapFactory.Options decode=new BitmapFactory.Options();decode.inMutable=true;decode.inPreferredColorSpace=ColorSpace.get(ColorSpace.Named.SRGB);
        Bitmap source=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,decode);
        if(source==null)throw new IOException("Unable to decode capture");
        timing.put("decode_ms",android.os.SystemClock.elapsedRealtime()-stage);stage=android.os.SystemClock.elapsedRealtime();
        try{
            if(style.lut==null){Bitmap rendered=PhotoRasterizer.render(source,orientation,crop?targetAspect:0,style,intensity);source.recycle();source=rendered;}
            else {
            Matrix rotate=new Matrix();switch(orientation){case 2:rotate.setScale(-1,1);break;case 3:rotate.setRotate(180);break;case 4:rotate.setScale(1,-1);break;case 5:rotate.setRotate(90);rotate.postScale(-1,1);break;case 6:rotate.setRotate(90);break;case 7:rotate.setRotate(270);rotate.postScale(-1,1);break;case 8:rotate.setRotate(270);break;}
            Bitmap normalized=Bitmap.createBitmap(source,0,0,source.getWidth(),source.getHeight(),rotate,true);
            if(normalized!=source){source.recycle();source=normalized;}
            if(crop){int[] rectangle=FrameGeometry.crop(source.getWidth(),source.getHeight(),targetAspect);Bitmap cropped=Bitmap.createBitmap(source,rectangle[0],rectangle[1],rectangle[2],rectangle[3]);if(cropped!=source){source.recycle();source=cropped;}}
            }
            Bitmap output=source.isMutable()?source:source.copy(Bitmap.Config.ARGB_8888,true);
            try{int w=output.getWidth(),h=output.getHeight();
                timing.put("geometry_ms",android.os.SystemClock.elapsedRealtime()-stage);timing.put("width",w);timing.put("height",h);stage=android.os.SystemClock.elapsedRealtime();
                if(style.lut!=null)PhotoColorProcessor.apply(output,style,intensity);
                timing.put("color_ms",android.os.SystemClock.elapsedRealtime()-stage);stage=android.os.SystemClock.elapsedRealtime();
                frame.draw(new Canvas(output),w,h);
                if(watermark.photo)watermark.draw(new Canvas(output),w,h);
                timing.put("overlay_ms",android.os.SystemClock.elapsedRealtime()-stage);stage=android.os.SystemClock.elapsedRealtime();
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();if(!output.compress(Bitmap.CompressFormat.JPEG,Math.max(70,Math.min(100,context.getSharedPreferences("settings",0).getInt("jpegQuality",96))),bytes))throw new IOException("JPEG export failed");
                timing.put("encode_ms",android.os.SystemClock.elapsedRealtime()-stage);stage=android.os.SystemClock.elapsedRealtime();Uri saved=write(name("_"+style.id+".jpg"),"image/jpeg",MediaMetadataPolicy.sanitizeJpeg(bytes.toByteArray(),1));timing.put("publish_ms",android.os.SystemClock.elapsedRealtime()-stage);timing.put("export_total_ms",android.os.SystemClock.elapsedRealtime()-started);try(FileOutputStream report=context.openFileOutput("last-photo-timing.json",Context.MODE_PRIVATE)){report.write(timing.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(IOException ignored){/* Diagnostics must not turn a saved photo into an export failure. */}return saved;
            }finally{if(output!=source)output.recycle();}
        }finally{source.recycle();}
    }
    public Uri saveVideo(File file) throws IOException {
        try(InputStream input=new FileInputStream(file)){return writeStream(name(".mp4"),"video/mp4",input);}
    }
    private Uri write(String name,String mime,byte[] bytes)throws IOException{try(InputStream in=new ByteArrayInputStream(bytes)){return writeStream(name,mime,in);}}
    private Uri writeStream(String name,String mime,InputStream input)throws IOException{
        ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,name);values.put(MediaStore.MediaColumns.MIME_TYPE,mime);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,(mime.startsWith("video")?"Movies":"Pictures")+"/Aabhix");values.put(MediaStore.MediaColumns.IS_PENDING,1);
        ContentResolver resolver=context.getContentResolver();Uri uri=resolver.insert(mime.startsWith("video")?MediaStore.Video.Media.EXTERNAL_CONTENT_URI:MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        if(uri==null)throw new IOException("MediaStore insertion failed");
        try{try(OutputStream output=resolver.openOutputStream(uri)){if(output==null)throw new IOException("Output unavailable");byte[] buffer=new byte[65536];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);}
            validate(uri,mime);values.clear();values.put(MediaStore.MediaColumns.IS_PENDING,0);if(resolver.update(uri,values,null,null)!=1)throw new IOException("Media publication failed");return uri;}
        catch(Exception e){resolver.delete(uri,null,null);throw e instanceof IOException?(IOException)e:new IOException(e);}
    }
    private void validate(Uri uri,String mime)throws IOException{
        if(mime.equals("image/jpeg")){try(InputStream in=context.getContentResolver().openInputStream(uri)){BitmapFactory.Options size=new BitmapFactory.Options();size.inJustDecodeBounds=true;BitmapFactory.decodeStream(in,null,size);if(size.outWidth<=0||size.outHeight<=0)throw new IOException("Exported JPEG cannot be opened");}}
        else {MediaMetadataRetriever reader=new MediaMetadataRetriever();try{reader.setDataSource(context,uri);String duration=reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);if(duration==null||Long.parseLong(duration)<=0)throw new IOException("Video not finalized");Bitmap frame=reader.getScaledFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,64,64);if(frame==null)throw new IOException("Video cannot be decoded");frame.recycle();}catch(RuntimeException e){throw new IOException("Video validation failed",e);}finally{reader.release();}}
    }
}
