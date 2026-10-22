package com.aabhix.camera;
import android.content.*;
import com.aabhix.camera.domain.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PresetStore {
    public final List<Style> styles=new java.util.concurrent.CopyOnWriteArrayList<>(Style.builtins());
    private final Context context; private final android.content.SharedPreferences preferences;
    public PresetStore(Context context){this.context=context;preferences=context.getSharedPreferences("styles",0);
        File dir=new File(context.getFilesDir(),"presets");dir.mkdirs();File[] files=dir.listFiles();if(files!=null)for(File file:files){try{if(file.getName().endsWith(".cube")){File title=new File(dir,file.getName()+".title");String label=title.isFile()?new String(java.nio.file.Files.readAllBytes(title.toPath()),StandardCharsets.UTF_8):file.getName().replace(".cube","");try(Reader r=new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8)){styles.add(new Style(file.getName(),label,"Imported",1,1,0,0,0,0,CubeLut.parse(r)));}}
            else if(file.getName().endsWith(".png")){try(InputStream stream=new FileInputStream(file)){File title=new File(dir,file.getName()+".title");String label=title.isFile()?new String(java.nio.file.Files.readAllBytes(title.toPath()),StandardCharsets.UTF_8):file.getName();styles.add(png(label,stream,file.getName()));}}
            else if(file.getName().endsWith(".json")){styles.add(fromJson(new JSONObject(new String(java.nio.file.Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8)),file.getName()));}
        }catch(Exception ignored){/* Invalid files are not offered as working presets. */}}
    }
    public Set<String> favorites(){return new HashSet<>(preferences.getStringSet("favorites",Collections.emptySet()));}
    public void toggleFavorite(Style style){Set<String> f=favorites();if(!f.add(style.id))f.remove(style.id);preferences.edit().putStringSet("favorites",f).apply();}
    public List<String> recent(){String v=preferences.getString("recent","");return new ArrayList<>(v.isEmpty()?Collections.emptyList():Arrays.asList(v.split(",")));}
    public void used(Style style){List<String> ids=recent();ids.remove(style.id);ids.add(0,style.id);if(ids.size()>20)ids=ids.subList(0,20);preferences.edit().putString("recent",String.join(",",ids)).apply();}
    public Set<String> collections(){return new TreeSet<>(preferences.getStringSet("collections",Collections.emptySet()));}
    public Set<String> collection(String name){return new HashSet<>(preferences.getStringSet("collection:"+name,Collections.emptySet()));}
    public void addCollection(String name,Style style){name=name.trim();if(name.isEmpty()||name.length()>50)throw new IllegalArgumentException("Collection name: 1–50 characters");Set<String> names=collections();names.add(name);Set<String> ids=collection(name);ids.add(style.id);preferences.edit().putStringSet("collections",names).putStringSet("collection:"+name,ids).apply();}
    public Style importPreset(String name,InputStream stream) throws Exception {
        String safe=name.replaceAll("[^A-Za-z0-9._ -]","_");if(safe.length()>80)safe=safe.substring(safe.length()-80);
        if(safe.toLowerCase(Locale.ROOT).endsWith(".png"))return importPng(safe,stream);
        boolean cube=safe.toLowerCase(Locale.ROOT).endsWith(".cube"),json=safe.toLowerCase(Locale.ROOT).endsWith(".json");
        if(!cube&&!json)throw new IllegalArgumentException("Supported: SDR 3D .cube, 512×512 tiled .png or Aabhix v1 .json");
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int n;while((n=stream.read(chunk))!=-1){if(out.size()+n>12*1024*1024)throw new IllegalArgumentException("Import is larger than 12 MB");out.write(chunk,0,n);}
        String content=out.toString(StandardCharsets.UTF_8.name());String id="user-"+UUID.randomUUID()+ (cube?".cube":".json");
        Style style=cube?new Style(id,safe.substring(0,safe.length()-5),"Imported",1,1,0,0,0,0,CubeLut.parse(new StringReader(content))):fromJson(new JSONObject(content),id);
        File target=new File(new File(context.getFilesDir(),"presets"),id);try(OutputStream output=new FileOutputStream(target)){output.write(out.toByteArray());}if(cube){try(OutputStream title=new FileOutputStream(new File(target.getParentFile(),id+".title"))){title.write(style.name.getBytes(StandardCharsets.UTF_8));}}styles.add(style);return style;
    }
    private static Style png(String name,InputStream input,String id)throws IOException{
        ByteArrayOutputStream data=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int read;while((read=input.read(chunk))!=-1){if(data.size()+read>12*1024*1024)throw new IOException("PNG exceeds12 MB");data.write(chunk,0,read);}byte[] bytes=data.toByteArray();android.graphics.BitmapFactory.Options bounds=new android.graphics.BitmapFactory.Options();bounds.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);if(bounds.outWidth!=512||bounds.outHeight!=512)throw new IOException("Expected512×512 tiled RGB LUT; ordinary photos are not LUTs");android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inScaled=false;options.inPreferredConfig=android.graphics.Bitmap.Config.ARGB_8888;options.inPremultiplied=false;android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap==null)throw new IOException("PNG decode failed");try{int[] pixels=new int[512*512];bitmap.getPixels(pixels,0,512,0,0,512,512);return new Style(id,name.replaceFirst("(?i)\\.png$",""),"Imported",1,1,0,0,0,0,CubeLut.tiled64(512,512,pixels));}finally{bitmap.recycle();}
    }
    private Style importPng(String name,InputStream stream)throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int n;while((n=stream.read(chunk))!=-1){if(bytes.size()+n>12*1024*1024)throw new IOException("PNG exceeds12 MB");bytes.write(chunk,0,n);}byte[] data=bytes.toByteArray();String id="user-"+UUID.randomUUID()+".png";Style style=png(name,new ByteArrayInputStream(data),id);File dir=new File(context.getFilesDir(),"presets");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,id))){out.write(data);}try(OutputStream out=new FileOutputStream(new File(dir,id+".title"))){out.write(style.name.getBytes(StandardCharsets.UTF_8));}styles.add(style);return style;
    }    public Style saveCustom(String name,float saturation,float contrast,float warmth,float tint,float fade,float exposure)throws Exception{
        JSONObject json=new JSONObject();json.put("format","aabhix-style");json.put("version",1);json.put("name",name);json.put("saturation",saturation);json.put("contrast",contrast);json.put("warmth",warmth);json.put("tint",tint);json.put("fade",fade);json.put("exposure",exposure);
        return importPreset("custom.json",new ByteArrayInputStream(json.toString(2).getBytes(StandardCharsets.UTF_8)));
    }
    public static Style fromJson(JSONObject json,String id)throws Exception{
        if(!json.optString("format").equals("aabhix-style")||json.optInt("version")!=1)throw new IllegalArgumentException("Expected Aabhix style format version 1");
        String name=json.getString("name").trim();if(name.isEmpty()||name.length()>80)throw new IllegalArgumentException("Name must be 1–80 characters");
        return new Style(id,name,"Custom",value(json,"saturation",1,0,2),value(json,"contrast",1,.1f,2),value(json,"warmth",0,-.2f,.2f),value(json,"tint",0,-.2f,.2f),value(json,"fade",0,0,.3f),value(json,"exposure",0,-.3f,.3f));
    }
    private static float value(JSONObject json,String key,float fallback,float min,float max){double v=json.optDouble(key,fallback);if(!Double.isFinite(v)||v<min||v>max)throw new IllegalArgumentException("Invalid "+key);return(float)v;}
}
