package com.aabhix.camera.domain;

import java.util.ArrayList;
import java.util.List;

/** Original display-referred SDR/sRGB grades. No proprietary LUTs or camera profiles. */
public final class Style {
    public final String id, name, category;
    public final float saturation, contrast, warmth, tint, fade, exposure;
    public final CubeLut lut;
    public Style(String id,String name,String category,float saturation,float contrast,float warmth,float tint,float fade,float exposure) {
        this(id,name,category,saturation,contrast,warmth,tint,fade,exposure,null);
    }
    public Style(String id,String name,String category,float saturation,float contrast,float warmth,float tint,float fade,float exposure,CubeLut lut) {
        this.id=id; this.name=name; this.category=category; this.saturation=saturation; this.contrast=contrast;
        this.warmth=warmth; this.tint=tint; this.fade=fade; this.exposure=exposure; this.lut=lut;
    }
    public static Style identity(){ return new Style("original","Original","Natural",1,1,0,0,0,0); }
    public static List<Style> builtins(){
        List<Style> all=new ArrayList<>(); all.add(identity()); add(all,"document-mono","Document Mono","Utility",0,1.3f,0,0,0,.03f);
        add(all,"daylight","Daylight","Natural",1.02f,1.02f,.01f,0,0,0);
        add(all,"meadow","Meadow","Natural",1.14f,1.04f,.015f,-.01f,0,0);
        add(all,"porcelain","Porcelain","Natural",.92f,.96f,.015f,.005f,.01f,.02f);
        add(all,"studio","Studio Neutral","Studio",.96f,.98f,0,0,.01f,0);
        add(all,"silk","Silk Tones","Studio",.88f,.91f,.02f,.01f,.025f,.01f);
        add(all,"highland","Highland","Studio",1.06f,1.08f,-.01f,0,.015f,0);
        add(all,"cloud","Cloud Soft","Studio",.93f,.87f,0,0,.035f,.01f);
        add(all,"street","Street Journal","Street",.78f,1.12f,.015f,-.005f,.02f,0);
        add(all,"concrete","Concrete","Street",.68f,1.08f,-.025f,.005f,.03f,0);
        add(all,"document","Document","Street",.9f,1.05f,.005f,0,.01f,0);
        add(all,"silver","Silver Street","Monochrome",0,1.16f,0,0,.01f,0);
        add(all,"graphite","Graphite","Monochrome",0,1.32f,0,0,0,-.01f);
        add(all,"pearl","Pearl Mono","Monochrome",0,.9f,0,0,.035f,.02f);
        add(all,"noir","Noir Paper","Monochrome",0,1.45f,0,0,.02f,-.02f);
        add(all,"amber","Amber Portrait","Mobile",1.03f,.97f,.035f,.005f,.01f,.01f);
        add(all,"pop","Color Pop","Mobile",1.32f,1.12f,0,0,0,0);
        add(all,"urban","Urban Blue","Mobile",1.08f,1.08f,-.045f,.005f,.01f,0);
        add(all,"botanic","Botanic","Mobile",1.23f,1.06f,.005f,-.02f,0,0);
        add(all,"social","Everyday Bright","Mobile",1.15f,1.03f,.02f,0,.005f,.025f);
        add(all,"dusk","Dusk Glow","Mobile",.9f,.94f,-.025f,.02f,.025f,.025f);
        add(all,"copper","Copper Roll","Film",.83f,1.08f,.055f,.01f,.04f,0);
        add(all,"faded","Faded Days","Film",.72f,.88f,.025f,.005f,.075f,0);
        add(all,"rose","Rose Archive","Film",.88f,.95f,.02f,.035f,.04f,0);
        add(all,"mint","Mint Archive","Film",.82f,.94f,-.015f,-.025f,.045f,0);
        add(all,"gold","Golden Hour","Film",1.04f,1.02f,.065f,.005f,.025f,0);
        add(all,"retro","Retro Postcard","Film",.94f,1.12f,.04f,.015f,.045f,0);
        add(all,"ember","Ember Frame","Cinema",.87f,1.16f,.045f,0,.02f,-.01f);
        add(all,"harbor","Harbor Frame","Cinema",.8f,1.13f,-.04f,-.005f,.02f,0);
        add(all,"velvet","Velvet Frame","Cinema",.85f,1.05f,.02f,.025f,.035f,-.01f);
        add(all,"quiet","Quiet Frame","Cinema",.65f,.94f,-.015f,.01f,.045f,0);
        add(all,"clear","Clear Motion","Video",1.04f,1.04f,0,0,0,0);
        add(all,"warm-motion","Warm Motion","Video",1.02f,1.02f,.025f,0,.01f,0);
        add(all,"cool-motion","Cool Motion","Video",.95f,1.06f,-.025f,0,.015f,0);
        add(all,"muted-motion","Muted Motion","Video",.75f,1.04f,.01f,0,.03f,0);
        return all;
    }
    private static void add(List<Style> all,String id,String name,String category,float s,float c,float w,float t,float f,float e){all.add(new Style(id,name,category,s,c,w,t,f,e));}
}
