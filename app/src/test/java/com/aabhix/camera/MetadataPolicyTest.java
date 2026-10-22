package com.aabhix.camera;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
public class MetadataPolicyTest {
    private byte[] fixture()throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();o.write(new byte[]{(byte)255,(byte)216});segment(o,225,"Exif private GPS maker");segment(o,237,"IPTC personal");segment(o,254,"comment");segment(o,226,"ICC_PROFILE\0color");o.write(new byte[]{(byte)255,(byte)218,0,2,1,2,3,(byte)255,(byte)217});return o.toByteArray();}
    private void segment(ByteArrayOutputStream o,int marker,String s)throws Exception{byte[] b=s.getBytes("ISO-8859-1");o.write(255);o.write(marker);o.write((b.length+2)>>8);o.write((b.length+2)&255);o.write(b);}
    @Test public void stripsPrivateMetadataAndPreservesScanAndColor()throws Exception{byte[] out=MediaMetadataPolicy.sanitizeJpeg(fixture(),1);String s=new String(out,"ISO-8859-1");assertFalse(s.contains("private"));assertFalse(s.contains("IPTC"));assertFalse(s.contains("comment"));assertTrue(s.contains("ICC_PROFILE"));assertArrayEquals(new byte[]{(byte)255,(byte)218,0,2,1,2,3,(byte)255,(byte)217},java.util.Arrays.copyOfRange(out,out.length-9,out.length));}
    @Test public void insertsMinimalOrientationWithoutDeviceInformation()throws Exception{byte[] out=MediaMetadataPolicy.sanitizeJpeg(fixture(),6);assertEquals(225,out[3]&255);assertEquals(34,out[5]&255);assertEquals(6,out[31]&255);assertFalse(new String(out,"ISO-8859-1").contains("GPS"));}
    @Test(expected=IOException.class)public void rejectsMalformedLength()throws Exception{MediaMetadataPolicy.sanitizeJpeg(new byte[]{(byte)255,(byte)216,(byte)255,(byte)225,0,90},1);}
}
