package com.aabhix.camera.domain;
import java.io.*;
import java.util.*;
/** Strict bounded SDR 3D CUBE importer. Red-fastest ordering; domains are normalized. */
public final class CubeLut {
    public final int size; public final float[] data;
    private CubeLut(int size,float[] data){this.size=size;this.data=data;}
    public static CubeLut tiled64(int width,int height,int[] argb){
        if(width!=512||height!=512||argb==null||argb.length!=512*512)throw new IllegalArgumentException("PNG LUT must be 512×512, 8×8 tiles of 64 pixels, red-fastest RGB");
        float[] values=new float[64*64*64*3];for(int b=0;b<64;b++)for(int g=0;g<64;g++)for(int r=0;r<64;r++){int color=argb[(b/8*64+g)*512+b%8*64+r];if((color>>>24)!=255)throw new IllegalArgumentException("PNG LUT must be fully opaque");int at=3*(r+64*(g+64*b));values[at]=(color>>16&255)/255f;values[at+1]=(color>>8&255)/255f;values[at+2]=(color&255)/255f;}return new CubeLut(64,values);
    }
    public static CubeLut parse(Reader input) throws IOException {
        BufferedReader reader=new BufferedReader(input); int size=0,count=0,lines=0; float[] data=null;
        float[] min={0,0,0},max={1,1,1}; String line;
        while((line=reader.readLine())!=null) {
            if(++lines>280000 || line.length()>4096) throw new IllegalArgumentException("LUT exceeds limits");
            line=line.split("#",2)[0].trim(); if(line.isEmpty() || line.startsWith("TITLE "))continue;
            String[] p=line.split("\\s+");
            if(p[0].equals("LUT_3D_SIZE")) { if(size!=0||p.length!=2)throw new IllegalArgumentException("Invalid size header");size=Integer.parseInt(p[1]);if(size<2||size>64)throw new IllegalArgumentException("LUT size must be 2–64");data=new float[size*size*size*3]; }
            else if(p[0].equals("DOMAIN_MIN")||p[0].equals("DOMAIN_MAX")){if(p.length!=4)throw new IllegalArgumentException("Invalid domain");float[] v=p[0].equals("DOMAIN_MIN")?min:max;for(int i=0;i<3;i++)v[i]=finite(p[i+1]);}
            else {if(data==null||p.length!=3||count+3>data.length)throw new IllegalArgumentException("Unsupported CUBE structure");for(String v:p)data[count++]=finite(v);}
        }
        if(data==null||count!=data.length)throw new IllegalArgumentException("Incomplete LUT");
        for(int i=0;i<3;i++)if(min[i]!=0||max[i]!=1)throw new IllegalArgumentException("Only normalized SDR domains 0–1 are supported");
        return new CubeLut(size,data);
    }
    private static float finite(String s){float f=Float.parseFloat(s);if(!Float.isFinite(f)||f<0||f>1)throw new IllegalArgumentException("LUT values must be finite in 0–1");return f;}
    public float[] sample(float r,float g,float b){
        float[] out=new float[3];sampleInto(r,g,b,out);return out;
    }
    public void sampleInto(float r,float g,float b,float[] out){
        float xPos=clamp(r)*(size-1),yPos=clamp(g)*(size-1),zPos=clamp(b)*(size-1);
        int xLo=(int)xPos,yLo=(int)yPos,zLo=(int)zPos;out[0]=out[1]=out[2]=0;
        for(int dz=0;dz<2;dz++)for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++){
            int x=Math.min(size-1,xLo+dx),y=Math.min(size-1,yLo+dy),z=Math.min(size-1,zLo+dz);
            float w=(dx==0?1-xPos+xLo:xPos-xLo)*(dy==0?1-yPos+yLo:yPos-yLo)*(dz==0?1-zPos+zLo:zPos-zLo);
            int at=3*(x+size*(y+size*z));for(int c=0;c<3;c++)out[c]+=data[at+c]*w;
        }
    }
    private static float clamp(float v){return Math.max(0,Math.min(1,v));}
}
