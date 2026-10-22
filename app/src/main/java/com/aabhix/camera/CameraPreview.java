package com.aabhix.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.*;
import android.view.Surface;
import com.aabhix.camera.domain.*;
import java.nio.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** One GPU transform for the viewfinder and the recorder surface; SDR only. */
public final class CameraPreview extends GLSurfaceView implements GLSurfaceView.Renderer,SurfaceTexture.OnFrameAvailableListener {
    public interface Ready { void onReady(Surface surface); void onFailure(String message); }
    private final Ready ready;
    private final FloatBuffer vertices=ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private SurfaceTexture texture; private Surface cameraSurface; private int program,oes,lutTexture;
    private int width,height; private volatile boolean framePending; private boolean hasFrame;
    private final float[] textureMatrix=new float[16];
    private static final class Grade {final Style style;final float intensity;Grade(Style s,float i){style=s;intensity=i;}}
    private volatile Grade grade=new Grade(Style.identity(),1);private volatile boolean compareOriginal;private Style uploaded;
    private volatile int bufferWidth=1280,bufferHeight=960;private volatile float frameAspect=.75f;
    private volatile int[] viewport={0,0,1,1};private long lastRecordTimestamp,recordClockOffset;private boolean rendererFailed,recordable=true;
    private volatile int[] captureViewport={0,0,1,1};private volatile boolean fullSensorPreview;
    private volatile float displayAspect=.75f;
    private volatile int rotation=90; private volatile boolean mirror;
    private android.opengl.EGLDisplay recordDisplay; private android.opengl.EGLSurface recordSurface;
    private int recordWidth,recordHeight;
    private volatile WatermarkSettings watermark=WatermarkSettings.off();private volatile boolean videoMode;
    private WatermarkSettings uploadedWatermark;private int overlayTexture,overlayProgram,overlayHeight;private boolean uploadedVideo;
    private boolean capturingThumbnail;private volatile FrameOverlay frameOverlay=new FrameOverlay("none");private FrameOverlay uploadedFrame;
    public CameraPreview(Context context,Ready ready){
        super(context);this.ready=ready;setEGLContextClientVersion(2);
        setEGLConfigChooser((egl,display)->{
            javax.microedition.khronos.egl.EGLConfig[] choices=new javax.microedition.khronos.egl.EGLConfig[1];int[] count=new int[1];
            int[] attributes={0x3024,8,0x3023,8,0x3022,8,0x3021,8,0x3040,4,0x3033,4,0x3142,1,0x3038};
            if(!egl.eglChooseConfig(display,attributes,choices,1,count)||count[0]==0){recordable=false;attributes=new int[]{0x3024,8,0x3023,8,0x3022,8,0x3040,4,0x3033,4,0x3038};if(!egl.eglChooseConfig(display,attributes,choices,1,count)||count[0]==0)throw new IllegalStateException("GLES2 surface unavailable");}
            return choices[0];
        });setPreserveEGLContextOnPause(true);
        vertices.put(new float[]{-1,-1,0,0,1,-1,1,0,-1,1,0,1,1,1,1,1}).position(0);
        setRenderer(this);setRenderMode(RENDERMODE_WHEN_DIRTY);
    }
    public void setFrameOverlay(FrameOverlay frame){frameOverlay=frame;requestRender();}
    public void setStyle(Style s,float amount){grade=new Grade(s,Math.max(0,Math.min(1,amount)));requestRender();}
    public void setWatermark(WatermarkSettings settings){watermark=settings;requestRender();}
    public void setVideoMode(boolean video){videoMode=video;requestRender();}
    public void compareOriginal(boolean enabled){compareOriginal=enabled;requestRender();}
    public void configureFrame(int w,int h,float aspect)throws Exception{
        bufferWidth=w;bufferHeight=h;frameAspect=aspect;CountDownLatch done=new CountDownLatch(1);
        queueEvent(()->{if(texture!=null)texture.setDefaultBufferSize(w,h);done.countDown();});
        if(!done.await(2,TimeUnit.SECONDS))throw new IllegalStateException("Preview configuration timeout");requestRender();
    }
    public int[] viewport(){return viewport.clone();}
    public int[] captureViewport(){return captureViewport.clone();}
    public float displayAspect(){return displayAspect;}
    public void setFullSensorPreview(boolean full){fullSensorPreview=full;requestRender();}
    public void diagnosticSnapshot(java.util.function.Consumer<org.json.JSONObject> callback){queueEvent(()->{org.json.JSONObject json=new org.json.JSONObject();try{json.put("has_frame",hasFrame);json.put("surface_texture_matrix",new org.json.JSONArray(textureMatrix));json.put("sensor_display_rotation",rotation);if(hasFrame)json.put("shader_sampling",new org.json.JSONArray(TextureTransform.sampling(textureMatrix,rotation,mirror)));json.put("mirror",mirror);json.put("stream_width",bufferWidth);json.put("stream_height",bufferHeight);json.put("display_width",width);json.put("display_height",height);json.put("capture_aspect",frameAspect);json.put("display_aspect",displayAspect);json.put("full_stream_fit",fullSensorPreview);json.put("viewport",new org.json.JSONArray(viewport));json.put("capture_guide",new org.json.JSONArray(captureViewport));json.put("style",grade.style.name);json.put("style_intensity",grade.intensity);json.put("preview_only_bypass",compareOriginal);json.put("frame_format","Camera2 PRIVATE to external OES texture; display-referred RGB shader");}catch(org.json.JSONException e){/* Finite camera/GPU values are expected. */}callback.accept(json);});}
    public void captureThumbnail(java.util.function.Consumer<android.graphics.Bitmap> callback){queueEvent(()->{
        if(!hasFrame||width<=0||height<=0){callback.accept(null);return;}boolean previous=compareOriginal;capturingThumbnail=true;
        try{compareOriginal=true;int sampleHeight=Math.max(1,Math.min(320,Math.round(160/displayAspect)));draw(160,sampleHeight,false);int[] area=viewport;int w=area[2],h=area[3];if(w<=0||h<=0){callback.accept(null);return;}ByteBuffer rgba=ByteBuffer.allocateDirect(w*h*4);GLES20.glReadPixels(area[0],area[1],w,h,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,rgba);int[] pixels=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int at=(y*w+x)*4;pixels[(h-y-1)*w+x]=0xff000000|((rgba.get(at)&255)<<16)|((rgba.get(at+1)&255)<<8)|(rgba.get(at+2)&255);}android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(pixels,w,h,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Bitmap small=android.graphics.Bitmap.createScaledBitmap(image,160,Math.max(1,Math.round(160f*h/w)),true);if(small!=image)image.recycle();callback.accept(small);
        }catch(RuntimeException|OutOfMemoryError e){callback.accept(null);}finally{capturingThumbnail=false;compareOriginal=previous;draw(width,height,false);requestRender();}
    });}
    public void setCameraOrientation(int degrees,boolean front){rotation=degrees;mirror=front;}
    public void startRecording(Surface output,int w,int h) throws Exception {
        if(!recordable)throw new IllegalStateException("This GPU cannot provide a recordable surface");
        CountDownLatch latch=new CountDownLatch(1);String[] failure={null};
        queueEvent(()->{try{
            recordDisplay=EGL14.eglGetCurrentDisplay();android.opengl.EGLContext context=EGL14.eglGetCurrentContext();
            int[] configId=new int[1];EGL14.eglQueryContext(recordDisplay,context,EGL14.EGL_CONFIG_ID,configId,0);
            android.opengl.EGLConfig[] config=new android.opengl.EGLConfig[1];int[] count=new int[1];
            EGL14.eglChooseConfig(recordDisplay,new int[]{EGL14.EGL_CONFIG_ID,configId[0],EGL14.EGL_NONE},0,config,0,1,count,0);
            recordSurface=EGL14.eglCreateWindowSurface(recordDisplay,config[0],output,new int[]{EGL14.EGL_NONE},0);
            if(recordSurface==EGL14.EGL_NO_SURFACE)throw new IllegalStateException("Recorder GPU surface unsupported");
            recordWidth=w;recordHeight=h;lastRecordTimestamp=0;
        }catch(Exception e){failure[0]=e.getMessage();}finally{latch.countDown();}});
        if(!latch.await(3,TimeUnit.SECONDS))throw new IllegalStateException("GPU recorder timeout");
        if(failure[0]!=null)throw new IllegalStateException(failure[0]);
    }
    public void stopRecording() throws Exception {
        CountDownLatch latch=new CountDownLatch(1);queueEvent(()->{if(recordSurface!=null&&recordSurface!=EGL14.EGL_NO_SURFACE)EGL14.eglDestroySurface(recordDisplay,recordSurface);recordSurface=null;latch.countDown();});
        if(!latch.await(3,TimeUnit.SECONDS))throw new IllegalStateException("GPU stop timeout");
    }
    @Override public void onSurfaceCreated(GL10 gl,EGLConfig config){
        int[] ids=new int[3];GLES20.glGenTextures(3,ids,0);oes=ids[0];lutTexture=ids[1];overlayTexture=ids[2];uploadedWatermark=null;
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,oes);params(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,lutTexture);
        ByteBuffer dummy=ByteBuffer.allocateDirect(4);dummy.put(new byte[]{0,0,0,(byte)255}).position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,1,1,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,dummy);params(GLES20.GL_TEXTURE_2D);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        if(cameraSurface!=null)cameraSurface.release();if(texture!=null)texture.release();
        texture=new SurfaceTexture(oes);texture.setDefaultBufferSize(bufferWidth,bufferHeight);texture.setOnFrameAvailableListener(this);
        cameraSurface=new Surface(texture);uploaded=null;hasFrame=false;rendererFailed=false;
        try{program=link(VERTEX,FRAGMENT);overlayProgram=link(VERTEX,"precision mediump float;uniform sampler2D image;varying vec2 v;void main(){gl_FragColor=texture2D(image,vec2(v.x,1.-v.y));}");ready.onReady(cameraSurface);}catch(Exception e){ready.onFailure(e.getMessage());}
    }
    @Override public void onSurfaceChanged(GL10 gl,int w,int h){width=w;height=h;}
    @Override public void onFrameAvailable(SurfaceTexture source){framePending=true;requestRender();}
    @Override public void onDrawFrame(GL10 gl){
        if(program==0||rendererFailed)return;
        boolean newFrame=framePending;
        if(newFrame){framePending=false;texture.updateTexImage();texture.getTransformMatrix(textureMatrix);hasFrame=true;}
        if(!hasFrame)return;
        draw(width,height,false);
        if(newFrame&&recordSurface!=null&&recordSurface!=EGL14.EGL_NO_SURFACE&&texture.getTimestamp()>lastRecordTimestamp){
            android.opengl.EGLSurface oldDraw=EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW),oldRead=EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
            android.opengl.EGLContext ctx=EGL14.eglGetCurrentContext();
            if(EGL14.eglMakeCurrent(recordDisplay,recordSurface,recordSurface,ctx)){
                draw(recordWidth,recordHeight,true);long sensorTimestamp=texture.getTimestamp();if(lastRecordTimestamp==0)recordClockOffset=System.nanoTime()-sensorTimestamp;lastRecordTimestamp=sensorTimestamp;EGLExt.eglPresentationTimeANDROID(recordDisplay,recordSurface,sensorTimestamp+recordClockOffset);boolean swapped=EGL14.eglSwapBuffers(recordDisplay,recordSurface);int swapError=swapped?EGL14.EGL_SUCCESS:EGL14.eglGetError();
                EGL14.eglMakeCurrent(recordDisplay,oldDraw,oldRead,ctx);
                if(!swapped)ready.onFailure("Encoder surface swap failed: "+swapError);
            }else ready.onFailure("Encoder surface unavailable: "+EGL14.eglGetError());
        }
        int error=GLES20.glGetError();if(error!=GLES20.GL_NO_ERROR){rendererFailed=true;ready.onFailure("OpenGL rendering error: "+error);}
    }
    private void draw(int w,int h,boolean recordingOutput){
        Grade current=grade;Style s=!recordingOutput&&compareOriginal?Style.identity():current.style;float intensity=!recordingOutput&&compareOriginal?1:current.intensity;
        GLES20.glViewport(0,0,w,h);GLES20.glClearColor(0,0,0,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        float sourceAspect=rotation%180==0?bufferWidth/(float)bufferHeight:bufferHeight/(float)bufferWidth;
        boolean showSensor=!recordingOutput&&fullSensorPreview;
        float shownAspect=showSensor?sourceAspect:frameAspect;
        int[] bounds=FrameGeometry.previewBounds(w,h,sourceAspect,frameAspect,showSensor);int[] composition=FrameGeometry.captureBounds(bounds,frameAspect);
        if(!recordingOutput){viewport=bounds;captureViewport=composition;displayAspect=shownAspect;}GLES20.glViewport(bounds[0],bounds[1],bounds[2],bounds[3]);
        GLES20.glUseProgram(program);vertices.position(0);int p=GLES20.glGetAttribLocation(program,"position");GLES20.glVertexAttribPointer(p,2,GLES20.GL_FLOAT,false,16,vertices);GLES20.glEnableVertexAttribArray(p);
        vertices.position(2);int uv=GLES20.glGetAttribLocation(program,"uv");GLES20.glVertexAttribPointer(uv,2,GLES20.GL_FLOAT,false,16,vertices);GLES20.glEnableVertexAttribArray(uv);
        GLES20.glUniformMatrix4fv(uniform("textureMatrix"),1,false,textureMatrix,0);
        int[] sampling=TextureTransform.sampling(textureMatrix,rotation,mirror);
        GLES20.glUniform1i(uniform("rotation"),sampling[0]);GLES20.glUniform1i(uniform("mirror"),sampling[1]);
        float[] crop=FrameGeometry.cropScale(sourceAspect,shownAspect);GLES20.glUniform2f(uniform("cropScale"),crop[0],crop[1]);
        float[] m=ColorEngine.matrix(s,intensity);float[] cm={m[0],m[5],m[10],m[1],m[6],m[11],m[2],m[7],m[12]};
        GLES20.glUniformMatrix3fv(uniform("grade"),1,false,cm,0);GLES20.glUniform3f(uniform("offset"),m[4]/255,m[9]/255,m[14]/255);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,oes);GLES20.glUniform1i(uniform("camera"),0);
        GLES20.glUniform1i(uniform("lut"),1);
        GLES20.glUniform1f(uniform("lutSize"),s.lut==null?0:s.lut.size);GLES20.glUniform1f(uniform("amount"),intensity);
        if(s.lut!=null){GLES20.glActiveTexture(GLES20.GL_TEXTURE1);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,lutTexture);
            if(uploaded!=s){int n=s.lut.size;ByteBuffer bytes=ByteBuffer.allocateDirect(n*n*n*4);
                for(int g=0;g<n;g++)for(int b=0;b<n;b++)for(int r=0;r<n;r++){int at=3*(r+n*(g+n*b));bytes.put((byte)Math.round(s.lut.data[at]*255));bytes.put((byte)Math.round(s.lut.data[at+1]*255));bytes.put((byte)Math.round(s.lut.data[at+2]*255));bytes.put((byte)255);}
                bytes.position(0);GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,n*n,n,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,bytes);params(GLES20.GL_TEXTURE_2D);uploaded=s;
            }GLES20.glUniform1i(uniform("lut"),1);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        GLES20.glViewport(composition[0],composition[1],composition[2],composition[3]);
        drawWatermark(recordingOutput);
    }
    private void drawWatermark(boolean recordingOutput){
        if(capturingThumbnail)return;
        WatermarkSettings settings=watermark;boolean isVideo=recordingOutput||videoMode;
        boolean mark=(isVideo?settings.video:settings.photo)&&!settings.text.isEmpty();FrameOverlay frame=frameOverlay;if(!mark&&!frame.active())return;
        int h=Math.max(1,Math.round(1024/frameAspect));GLES20.glActiveTexture(GLES20.GL_TEXTURE2);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,overlayTexture);
        if(uploadedWatermark!=settings||uploadedVideo!=isVideo||overlayHeight!=h||uploadedFrame!=frame){android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(1024,h,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);frame.draw(canvas,1024,h);if(mark)settings.draw(canvas,1024,h);GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0);bitmap.recycle();params(GLES20.GL_TEXTURE_2D);uploadedWatermark=settings;uploadedFrame=frame;uploadedVideo=isVideo;overlayHeight=h;}
        GLES20.glUseProgram(overlayProgram);int position=GLES20.glGetAttribLocation(overlayProgram,"position"),uv=GLES20.glGetAttribLocation(overlayProgram,"uv");vertices.position(0);GLES20.glVertexAttribPointer(position,2,GLES20.GL_FLOAT,false,16,vertices);GLES20.glEnableVertexAttribArray(position);vertices.position(2);GLES20.glVertexAttribPointer(uv,2,GLES20.GL_FLOAT,false,16,vertices);GLES20.glEnableVertexAttribArray(uv);GLES20.glUniform1i(GLES20.glGetUniformLocation(overlayProgram,"image"),2);GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_ONE,GLES20.GL_ONE_MINUS_SRC_ALPHA);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);GLES20.glDisable(GLES20.GL_BLEND);
    }
    private int uniform(String name){return GLES20.glGetUniformLocation(program,name);}
    private static void params(int target){GLES20.glTexParameteri(target,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(target,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(target,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(target,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);}
    private static int shader(int kind,String source){int id=GLES20.glCreateShader(kind);GLES20.glShaderSource(id,source);GLES20.glCompileShader(id);int[] ok=new int[1];GLES20.glGetShaderiv(id,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(id));return id;}
    private static int link(String v,String f){int p=GLES20.glCreateProgram();GLES20.glAttachShader(p,shader(GLES20.GL_VERTEX_SHADER,v));GLES20.glAttachShader(p,shader(GLES20.GL_FRAGMENT_SHADER,f));GLES20.glLinkProgram(p);int[] ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(p));return p;}
    private static final String VERTEX="attribute vec2 position;attribute vec2 uv;varying vec2 v;void main(){gl_Position=vec4(position,0.,1.);v=uv;}";
    public static String fragmentShader(){return FRAGMENT;}
    private static final String FRAGMENT="#extension GL_OES_EGL_image_external : require\nprecision highp float;uniform samplerExternalOES camera;uniform sampler2D lut;uniform mat4 textureMatrix;uniform vec2 cropScale;uniform mat3 grade;uniform vec3 offset;uniform int rotation;uniform int mirror;uniform float lutSize;uniform float amount;varying vec2 v;void main(){vec2 u=(v-.5)*cropScale+.5;if(mirror==1)u.x=1.-u.x;if(rotation==90)u=vec2(u.y,1.-u.x);else if(rotation==270)u=vec2(1.-u.y,u.x);else if(rotation==180)u=1.-u;u=(textureMatrix*vec4(u,0.,1.)).xy;vec3 c=clamp(grade*texture2D(camera,u).rgb+offset,0.,1.);if(lutSize>1.){float z=c.b*(lutSize-1.);float lo=floor(z);float hi=min(lo+1.,lutSize-1.);vec2 a=vec2((c.r*(lutSize-1.)+lo*lutSize+.5)/(lutSize*lutSize),(c.g*(lutSize-1.)+.5)/lutSize);vec2 b=vec2((c.r*(lutSize-1.)+hi*lutSize+.5)/(lutSize*lutSize),a.y);c=mix(c,mix(texture2D(lut,a).rgb,texture2D(lut,b).rgb,fract(z)),amount);}gl_FragColor=vec4(c,1.);}";
}



