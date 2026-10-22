package com.aabhix.camera;
import java.io.*;
import java.nio.*;
/** Lossless JPEG metadata minimization; keeps ICC color data and minimal orientation. */
public final class MediaMetadataPolicy {
    private MediaMetadataPolicy(){}
    public static byte[] sanitizeJpeg(byte[] input,int orientation) throws IOException {
        if(input.length<4||(input[0]&255)!=255||(input[1]&255)!=216)throw new IOException("Invalid JPEG");
        ByteArrayOutputStream out=new ByteArrayOutputStream(input.length);out.write(input,0,2);
        if(orientation>1&&orientation<=8){
            ByteBuffer ex=ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
            ex.put(new byte[]{69,120,105,102,0,0,77,77,0,42,0,0,0,8,0,1});
            ex.putShort((short)0x112).putShort((short)3).putInt(1).putShort((short)orientation).putShort((short)0).putInt(0);
            out.write(255);out.write(225);out.write(0);out.write(34);out.write(ex.array());
        }
        int p=2;
        while(p<input.length){
            if((input[p]&255)!=255)throw new IOException("Malformed JPEG segment");
            int start=p++;while(p<input.length&&(input[p]&255)==255)p++;
            if(p>=input.length)throw new IOException("Truncated JPEG");int marker=input[p++]&255;
            if(marker==218||marker==217){out.write(input,start,input.length-start);return out.toByteArray();}
            if(marker==1||(marker>=208&&marker<=215)){out.write(input,start,p-start);continue;}
            if(p+2>input.length)throw new IOException("Truncated JPEG length");int len=((input[p]&255)<<8)|(input[p+1]&255);
            if(len<2||p+len>input.length)throw new IOException("Invalid JPEG length");
            boolean app=marker>=224&&marker<=239;
            boolean icc=marker==226&&len>=14&&new String(input,p+2,12,java.nio.charset.StandardCharsets.ISO_8859_1).equals("ICC_PROFILE\0");
            if((!app&&marker!=254)||marker==224||icc)out.write(input,start,p+len-start);
            p+=len;
        }
        throw new IOException("JPEG scan missing");
    }
}
