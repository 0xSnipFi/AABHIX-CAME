package com.aabhix.camera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.params.MeteringRectangle;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.util.*;
import android.view.Surface;
import com.aabhix.camera.domain.Style;
import com.aabhix.camera.domain.RecordingState;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public final class CameraController {
    public interface Events{void status(String message);void ready(String capabilities);void saved(Uri uri);void recording(boolean active);default void recordingState(RecordingState state){}default void videoFormat(int edge,int fps){}}
    private final Activity activity;private final CameraPreview preview;private final Events events;
    private final CameraManager manager;private final HandlerThread thread=new HandlerThread("AabhixCamera");private final Handler handler;
    private CameraDevice camera;private volatile CameraCaptureSession session;private CaptureRequest.Builder repeating;private ImageReader reader;
    private volatile CameraCharacteristics characteristics;private Surface previewSurface;private String cameraId;private boolean front,opening;private volatile boolean closed=true,busy,recording,exporting;private volatile long captureGeneration;
    private MediaRecorder recorder;private File recordingFile;private long recordStarted;
    private volatile int compensation;private volatile float zoom=1;private volatile float focus=-1;
    private Style captureStyle;private float captureIntensity;private boolean preserveOriginal=true;
    private WatermarkSettings captureWatermark=WatermarkSettings.off();
    private volatile long lifecycleGeneration;private volatile float photoRatio=4f/3,videoRatio=16f/9;private volatile boolean videoMode;
    private volatile float captureAspect,currentAspect=.75f;private volatile int streamWidth=1280,streamHeight=960,relativeRotation=90;
    private volatile int videoShortEdge=720,videoFps=30;private volatile boolean finalizing;
    private volatile int photoWidth,photoHeight;
    private boolean framingPending;private volatile boolean reconfiguring;private volatile String recorderDiagnostic="idle",rendererDiagnostic="none";
    private MeteringRectangle[] tapRegions;private long focusGeneration;private volatile boolean torch;
    private volatile TotalCaptureResult latestResult;
    private final java.util.concurrent.atomic.AtomicBoolean capturePending=new java.util.concurrent.atomic.AtomicBoolean();
    private volatile int manualIso,whiteBalance=CaptureRequest.CONTROL_AWB_MODE_AUTO;private volatile long manualExposure=8000000L;
    private volatile boolean exposureLock,wbLock,lockWbOnRecord;
    private volatile int antiBanding=CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO;
    private volatile String framingStage="idle";
    private int displayDegrees;
    private String requestedCameraId;
    private volatile RecordingState recordingState=RecordingState.IDLE;
    private final java.util.concurrent.ConcurrentLinkedQueue<RecordingState> recordingTrace=new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final java.util.concurrent.atomic.AtomicBoolean recordingPending=new java.util.concurrent.atomic.AtomicBoolean();
    private void state(RecordingState value){recordingState=value;recordingTrace.add(value);while(recordingTrace.size()>16)recordingTrace.poll();events.recordingState(value);}
    public RecordingState recordingState(){return recordingState;}
    public java.util.List<RecordingState> recordingTrace(){return new java.util.ArrayList<>(recordingTrace);}
    public String activeCameraId(){return cameraId;}
    public boolean lightEnabled(){return torch;}
    public void runtimeReport(java.util.function.Consumer<String> callback){handler.post(()->{org.json.JSONObject json=new org.json.JSONObject();try{json.put("camera_id",cameraId);json.put("sensor_orientation",characteristics==null?null:characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));json.put("display_degrees",displayDegrees);json.put("jpeg_width",photoWidth);json.put("jpeg_height",photoHeight);json.put("requested_digital_zoom",zoom);json.put("ev_steps",compensation);json.put("ev_stops",compensation*evStep());json.put("manual_exposure",manualIso>0);json.put("recording_state",recordingState.name());json.put("session_diagnostic",diagnostic());TotalCaptureResult result=latestResult;if(result!=null){json.put("iso",result.get(CaptureResult.SENSOR_SENSITIVITY));json.put("exposure_ns",result.get(CaptureResult.SENSOR_EXPOSURE_TIME));json.put("focus_diopters",result.get(CaptureResult.LENS_FOCUS_DISTANCE));json.put("ae_mode",result.get(CaptureResult.CONTROL_AE_MODE));json.put("ae_state",result.get(CaptureResult.CONTROL_AE_STATE));json.put("awb_mode",result.get(CaptureResult.CONTROL_AWB_MODE));json.put("awb_state",result.get(CaptureResult.CONTROL_AWB_STATE));json.put("ae_lock",result.get(CaptureResult.CONTROL_AE_LOCK));json.put("awb_lock",result.get(CaptureResult.CONTROL_AWB_LOCK));json.put("crop_region",String.valueOf(result.get(CaptureResult.SCALER_CROP_REGION)));if(Build.VERSION.SDK_INT>=31)json.put("rotate_and_crop",result.get(CaptureResult.SCALER_ROTATE_AND_CROP));}preview.diagnosticSnapshot(texture->{try{json.put("renderer",texture);String output=json.toString(2);activity.runOnUiThread(()->callback.accept(output));}catch(org.json.JSONException ignored){events.status("Diagnostics could not be formatted");}});}catch(org.json.JSONException e){events.status("Diagnostics unavailable: "+e.getMessage());}});}
    public String diagnostic(){return "framing="+framingStage+" generation="+lifecycleGeneration+" session="+(session!=null)+" busy="+busy+" export="+exporting+" pending="+capturePending.get()+" closed="+closed+" recorder="+recorderDiagnostic+" renderer="+rendererDiagnostic;}
    public CameraController(Activity activity,CameraPreview preview,Events events){this.activity=activity;this.preview=preview;this.events=events;requestedCameraId=activity.getSharedPreferences("settings",0).getString("cameraId",null);manager=activity.getSystemService(CameraManager.class);thread.start();handler=new Handler(thread.getLooper());}
    public void open(Surface surface){handler.post(()->{if(previewSurface!=surface&&camera!=null)closeInternal();previewSurface=surface;closed=false;openInternal();});}
    @SuppressWarnings("MissingPermission") private void openInternal(){
        if(closed||opening||camera!=null||previewSurface==null||activity.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
        try{
            cameraId=null;
            for(String id:manager.getCameraIdList()){Integer facing=manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);if(requestedCameraId!=null?requestedCameraId.equals(id):facing!=null&&facing==(front?0:1)){cameraId=id;front=facing!=null&&facing==CameraCharacteristics.LENS_FACING_FRONT;break;}}
            if(cameraId==null&&requestedCameraId!=null){requestedCameraId=null;front=false;for(String id:manager.getCameraIdList())if(Integer.valueOf(CameraCharacteristics.LENS_FACING_BACK).equals(manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING))){cameraId=id;break;}events.status("Saved camera unavailable; using the default rear camera");}
            if(cameraId==null)throw new IllegalStateException("Requested camera unavailable");
            characteristics=manager.getCameraCharacteristics(cameraId);Integer orientation=characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            displayDegrees=activity.getWindowManager().getDefaultDisplay().getRotation()*90;relativeRotation=com.aabhix.camera.domain.FrameGeometry.rotation(orientation==null?90:orientation,displayDegrees,front);
            preview.setCameraOrientation(relativeRotation,front);
            StreamConfigurationMap map=characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);if(map==null)throw new IllegalStateException("No camera stream configuration");
            // Keep sensor detail. An exact 3:2 match may be only 720x480; crop the full JPEG instead.
            Size photo=chooseSize(map.getOutputSizes(ImageFormat.JPEG),0,16000000);
            photoWidth=photo.getWidth();photoHeight=photo.getHeight();
            configureGraphics();
            reader=ImageReader.newInstance(photo.getWidth(),photo.getHeight(),ImageFormat.JPEG,2);
            reader.setOnImageAvailableListener(r->{try(Image image=r.acquireNextImage()){if(image==null||r!=reader||closed)return;ByteBuffer b=image.getPlanes()[0].getBuffer();byte[] bytes=new byte[b.remaining()];b.get(bytes);
                Style style=captureStyle;float amount=captureIntensity,aspect=captureAspect;boolean preserve=preserveOriginal;WatermarkSettings watermark=captureWatermark;long generation=captureGeneration;exporting=true;events.status("Processing photo · "+style.name+" · full resolution");
                new Thread(()->{File recovery=null;try{File dir=new File(activity.getFilesDir(),"recovery");dir.mkdirs();recovery=File.createTempFile("photo-", ".jpg",dir);try(FileOutputStream output=new FileOutputStream(recovery)){output.write(bytes);}events.saved(new MediaRepository(activity).saveJpeg(bytes,style,amount,preserve,aspect,watermark));recovery.delete();}catch(Exception|OutOfMemoryError e){events.status("Photo export failed; capture retained in recovery: "+e.getMessage());}finally{exporting=false;if(generation==captureGeneration)busy=false;}},"AabhixExport").start();}catch(Exception e){busy=false;events.status(e.getMessage());}},handler);
            opening=true;long generation=++lifecycleGeneration;manager.openCamera(cameraId,new CameraDevice.StateCallback(){
                public void onOpened(CameraDevice device){if(generation!=lifecycleGeneration||closed){device.close();return;}opening=false;camera=device;try{framingPending=false;configureGraphics();startPreview();}catch(Exception e){closeInternal();events.status("Pending framing failed: "+e.getMessage());}}
                public void onDisconnected(CameraDevice device){device.close();if(camera!=device)return;closeInternal();events.status("Camera disconnected");}
                public void onError(CameraDevice device,int error){device.close();if(generation!=lifecycleGeneration&&camera!=device)return;closeInternal();events.status("Camera error "+error);}
            },handler);
        }catch(Exception e){opening=false;reconfiguring=false;android.util.Log.e("AabhixCamera","Open failed",e);events.status("Camera unavailable: "+e.getMessage());}
    }
    private void startPreview(){try{
        android.util.Log.i("AabhixCamera","Configuring session generation="+lifecycleGeneration+" stream="+streamWidth+"x"+streamHeight+" aspect="+currentAspect);
        final long generation=lifecycleGeneration;final CameraDevice owner=camera;
        camera.createCaptureSession(Arrays.asList(previewSurface,reader.getSurface()),new CameraCaptureSession.StateCallback(){
            public void onConfigured(CameraCaptureSession s){if(closed||camera!=owner||generation!=lifecycleGeneration){s.close();return;}session=s;reconfiguring=false;try{repeating=camera.createCaptureRequest(videoMode?CameraDevice.TEMPLATE_RECORD:CameraDevice.TEMPLATE_PREVIEW);repeating.addTarget(previewSurface);updateRequest();events.ready(profile());}catch(Exception e){reconfiguring=false;events.status(e.getMessage());}}
            public void onConfigureFailed(CameraCaptureSession s){if(generation!=lifecycleGeneration||camera!=owner){s.close();return;}reconfiguring=false;android.util.Log.e("AabhixCamera","Session configuration failed");events.status("Camera stream combination unsupported");}
        },handler);
    }catch(Exception e){reconfiguring=false;events.status(e.getMessage());}}
    public void capture(Style style,float intensity,boolean preserve){if(!capturePending.compareAndSet(false,true)){events.status("Camera is busy");return;}handler.post(()->{
        if(camera==null||session==null||busy||exporting||recording||finalizing){capturePending.set(false);events.status("Camera is busy");return;}busy=true;capturePending.set(false);long generation=++captureGeneration;captureStyle=style;captureIntensity=intensity;preserveOriginal=preserve;captureAspect=currentAspect;captureWatermark=WatermarkSettings.load(activity.getSharedPreferences("settings",0));
        try{CaptureRequest.Builder request=camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);request.addTarget(reader.getSurface());configure(request);
            request.set(CaptureRequest.JPEG_ORIENTATION,relativeRotation);request.set(CaptureRequest.JPEG_QUALITY,(byte)Math.max(70,Math.min(100,activity.getSharedPreferences("settings",0).getInt("jpegQuality",96))));
            session.capture(request.build(),new CameraCaptureSession.CaptureCallback(){@Override public void onCaptureFailed(CameraCaptureSession s,CaptureRequest r,CaptureFailure f){if(generation==captureGeneration)busy=false;events.status("Capture failed: "+f.getReason());}},handler);
            handler.postDelayed(()->{if(generation==captureGeneration&&busy&&!exporting){busy=false;events.status("Camera did not return a JPEG; please retry");}},10000);
        }catch(Exception e){busy=false;events.status(e.getMessage());}
    });}
    private void configure(CaptureRequest.Builder builder){
        builder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        if(Build.VERSION.SDK_INT>=31){int[] rotations=characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_ROTATE_AND_CROP_MODES);if(rotations!=null)for(int mode:rotations)if(mode==CaptureRequest.SCALER_ROTATE_AND_CROP_NONE){builder.set(CaptureRequest.SCALER_ROTATE_AND_CROP,mode);break;}}
        builder.set(CaptureRequest.CONTROL_AWB_MODE,whiteBalance);
        if(hasExposureLock())builder.set(CaptureRequest.CONTROL_AE_LOCK,exposureLock&&manualIso==0);
        if(hasWhiteBalanceLock())builder.set(CaptureRequest.CONTROL_AWB_LOCK,wbLock||(recording&&lockWbOnRecord));
        for(int mode:antiBandingModes())if(mode==antiBanding){builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,antiBanding);break;}
        if(manualIso>0&&hasManualSensor()){
            Range<Integer> sensitivity=characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);Range<Long> exposure=characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            if(sensitivity!=null&&exposure!=null){long time=exposure.clamp(com.aabhix.camera.domain.CapturePolicy.exposure(manualExposure,videoFps,videoMode&&activity.getSharedPreferences("settings",0).getBoolean("stableVideo",true)));builder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF);builder.set(CaptureRequest.SENSOR_SENSITIVITY,sensitivity.clamp(manualIso));builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME,time);Long maxFrame=characteristics.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION);builder.set(CaptureRequest.SENSOR_FRAME_DURATION,Math.min(maxFrame==null?Long.MAX_VALUE:maxFrame,Math.max(time,1_000_000_000L/(videoMode?videoFps:30))));}
        }else{builder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);builder.set(CaptureRequest.SENSOR_SENSITIVITY,null);builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME,null);builder.set(CaptureRequest.SENSOR_FRAME_DURATION,null);}
        int[] modes=characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        builder.set(CaptureRequest.CONTROL_AF_MODE,com.aabhix.camera.domain.CapturePolicy.focusMode(modes,videoMode,focus>=0,tapRegions!=null));
        if(activity.getSharedPreferences("settings",0).getBoolean("stabilization",true)){int[] optical=characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);boolean ois=false;if(optical!=null)for(int mode:optical)if(mode==1){builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,1);ois=true;}int[] electronic=characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);if(electronic!=null)for(int mode:electronic)if(mode==(videoMode&&!ois?1:0))builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,mode);}
        else {int[] optical=characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);if(optical!=null)for(int mode:optical)if(mode==0)builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,0);int[] electronic=characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);if(electronic!=null)for(int mode:electronic)if(mode==0)builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,0);}
        builder.set(CaptureRequest.FLASH_MODE,torch&&hasFlash()?CaptureRequest.FLASH_MODE_TORCH:CaptureRequest.FLASH_MODE_OFF);
        Integer maxAF=characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF),maxAE=characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        if(maxAF!=null&&maxAF>0)builder.set(CaptureRequest.CONTROL_AF_REGIONS,tapRegions);if(maxAE!=null&&maxAE>0)builder.set(CaptureRequest.CONTROL_AE_REGIONS,tapRegions);
        Float distance=characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);if(focus>=0&&distance!=null&&distance>0)builder.set(CaptureRequest.LENS_FOCUS_DISTANCE,focus*distance);
        Range<Integer> ev=characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);if(ev!=null)builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,Math.max(ev.getLower(),Math.min(ev.getUpper(),compensation)));
        android.graphics.Rect active=characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);Float max=characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        if(active!=null&&max!=null){float z=Math.max(1,Math.min(max,zoom));int w=(int)(active.width()/z),h=(int)(active.height()/z);int x=active.left+(active.width()-w)/2,y=active.top+(active.height()-h)/2;builder.set(CaptureRequest.SCALER_CROP_REGION,new android.graphics.Rect(x,y,x+w,y+h));}
        Range<Integer>[] ranges=characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);if(videoMode&&ranges!=null){int[] low=new int[ranges.length],high=new int[ranges.length];for(int i=0;i<ranges.length;i++){low[i]=ranges[i].getLower();high[i]=ranges[i].getUpper();}int index=com.aabhix.camera.domain.CapturePolicy.fpsRange(low,high,videoFps);if(index>=0)builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,ranges[index]);}
    }
    private void updateRequest(){try{if(session!=null&&repeating!=null){configure(repeating);session.setRepeatingRequest(repeating.build(),new CameraCaptureSession.CaptureCallback(){@Override public void onCaptureCompleted(CameraCaptureSession s,CaptureRequest request,TotalCaptureResult result){latestResult=result;}},handler);}}catch(Exception e){events.status(e.getMessage());}}
    public void controls(int ev,float zoomValue,float focusValue){handler.post(()->{compensation=ev;zoom=zoomValue;focus=focusValue;updateRequest();});}
    public void refreshSettings(){handler.post(this::updateRequest);}
    public void light(boolean enabled){handler.post(()->{if(!hasFlash()){events.status("This camera has no flash unit");return;}torch=enabled;updateRequest();});}
    public boolean hasFlash(){return characteristics!=null&&Boolean.TRUE.equals(characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE));}
    public boolean canControlStabilization(){if(characteristics==null)return false;for(android.hardware.camera2.CameraCharacteristics.Key<int[]> key:java.util.Arrays.asList(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION,CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)){int[] modes=characteristics.get(key);boolean off=false,on=false;if(modes!=null)for(int mode:modes){off|=mode==0;on|=mode==1;}if(off&&on)return true;}return false;}
    public boolean hasManualSensor(){if(characteristics==null)return false;int[] capabilities=characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);if(capabilities!=null)for(int capability:capabilities)if(capability==CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)return true;return false;}
    public int manualIso(){return manualIso;}
    public Range<Integer> isoRange(){return characteristics==null?null:characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);}
    public Range<Long> usableExposureRange(){Range<Long> range=exposureRange();if(range==null)return null;long upper=videoMode&&activity.getSharedPreferences("settings",0).getBoolean("stableVideo",true)?Math.min(range.getUpper(),1_000_000_000L/videoFps):range.getUpper();return new Range<>(range.getLower(),Math.max(range.getLower(),upper));}
    public Range<Long> exposureRange(){return characteristics==null?null:characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);}
    public boolean hasExposureLock(){return characteristics!=null&&Boolean.TRUE.equals(characteristics.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE));}
    public boolean hasWhiteBalanceLock(){return characteristics!=null&&Boolean.TRUE.equals(characteristics.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE));}
    public boolean exposureLocked(){return exposureLock;}
    public boolean whiteBalanceLocked(){return wbLock;}
    public int[] antiBandingModes(){int[] modes=characteristics==null?null:characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES);return modes==null?new int[0]:modes.clone();}
    public int antiBanding(){return antiBanding;}
    public void advancedControls(boolean ae,boolean awb,boolean recordLock,int flicker){
        boolean valid=false;for(int value:antiBandingModes())if(value==flicker)valid=true;
        if(!valid&&antiBandingModes().length>0)throw new IllegalArgumentException("Anti-flicker mode unavailable on this camera");
        if(ae&&!hasExposureLock()||awb&&!hasWhiteBalanceLock()||recordLock&&!hasWhiteBalanceLock())throw new IllegalArgumentException("Lock unavailable on this camera");
        handler.post(()->{exposureLock=ae;wbLock=awb;lockWbOnRecord=recordLock;antiBanding=flicker;updateRequest();});
    }
    public String sensorRanges(){return characteristics==null?"Camera is opening": "ISO "+characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)+" · shutter ns "+characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);}
    public long recordingElapsed(){return recording?SystemClock.elapsedRealtime()-recordStarted:0;}
    public long manualExposure(){return manualExposure;}
    public int whiteBalance(){return whiteBalance;}
    public int[] whiteBalanceModes(){int[] modes=characteristics==null?null:characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);return modes==null?new int[]{1}:modes.clone();}
    public void proControls(boolean manual,int iso,long exposure,int wb){
        if(manual&&!hasManualSensor())throw new IllegalArgumentException("Manual sensor controls unavailable on this camera");
        if(manual){Range<Integer> isoRange=characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);Range<Long> times=characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);if(isoRange==null||times==null||!isoRange.contains(iso)||!times.contains(exposure))throw new IllegalArgumentException("ISO or shutter is outside the supported range");}
        boolean supported=false;for(int mode:whiteBalanceModes())if(mode==wb&&mode!=0)supported=true;if(!supported)throw new IllegalArgumentException("White balance mode unsupported");
        handler.post(()->{manualIso=manual?iso:0;manualExposure=com.aabhix.camera.domain.CapturePolicy.exposure(exposure,videoFps,videoMode&&activity.getSharedPreferences("settings",0).getBoolean("stableVideo",true));whiteBalance=wb;updateRequest();});
    }
    public void tapFocus(float x,float y){handler.post(()->{
        if(camera==null||session==null||repeating==null||characteristics==null)return;
        try{android.graphics.Rect active=characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);if(active==null)return;
            android.graphics.Rect zoomCrop=repeating.get(CaptureRequest.SCALER_CROP_REGION);if(zoomCrop==null)zoomCrop=active;
            float sourceAspect=relativeRotation%180==0?streamWidth/(float)streamHeight:streamHeight/(float)streamWidth;
            float[] crop=com.aabhix.camera.domain.FrameGeometry.cropScale(sourceAspect,preview.displayAspect());
            float[] point=com.aabhix.camera.domain.FrameGeometry.sensorPoint((x-.5f)*crop[0]+.5f,(y-.5f)*crop[1]+.5f,relativeRotation,front);
            int[] visible=com.aabhix.camera.domain.FrameGeometry.crop(zoomCrop.width(),zoomCrop.height(),streamWidth/(float)streamHeight);
            int cx=zoomCrop.left+visible[0]+Math.round(point[0]*visible[2]),cy=zoomCrop.top+visible[1]+Math.round(point[1]*visible[3]);int half=Math.max(16,Math.min(visible[2],visible[3])/14);
            android.graphics.Rect area=new android.graphics.Rect(Math.max(active.left,cx-half),Math.max(active.top,cy-half),Math.min(active.right,cx+half),Math.min(active.bottom,cy+half));tapRegions=new MeteringRectangle[]{new MeteringRectangle(area,MeteringRectangle.METERING_WEIGHT_MAX)};focus=-1;configure(repeating);
            if(hasManualFocus()){repeating.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);session.capture(repeating.build(),null,handler);repeating.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_IDLE);}updateRequest();
            long token=++focusGeneration;handler.postDelayed(()->{if(token==focusGeneration){tapRegions=null;updateRequest();}},4000);
        }catch(Exception e){events.status("Focus unavailable: "+e.getMessage());}
    });}
    public void switchCamera(){handler.post(()->{if(recording||busy||exporting||finalizing){events.status("Wait for the current capture");return;}closeInternal();front=!front;requestedCameraId=null;cameraId=null;compensation=0;zoom=1;focus=-1;manualIso=0;torch=false;exposureLock=false;wbLock=false;whiteBalance=CaptureRequest.CONTROL_AWB_MODE_AUTO;closed=false;openInternal();});}
    public void cameraChoices(java.util.function.Consumer<java.util.List<CameraCatalog.Choice>> callback){handler.post(()->{try{java.util.List<CameraCatalog.Choice> choices=CameraCatalog.cameras(manager);activity.runOnUiThread(()->callback.accept(choices));}catch(Exception e){events.status("Camera list unavailable: "+e.getMessage());}});}
    public void selectCamera(String id){if(isBusy()){events.status("Finish capture before selecting a lens");return;}handler.post(()->{if(busy||exporting||recording||finalizing)return;try{boolean found=false;for(String publicId:manager.getCameraIdList())if(id.equals(publicId))found=true;if(!found)throw new IllegalArgumentException("Camera ID is not public");closeInternal();requestedCameraId=id;cameraId=null;compensation=0;zoom=1;focus=-1;manualIso=0;torch=false;exposureLock=false;wbLock=false;whiteBalance=CaptureRequest.CONTROL_AWB_MODE_AUTO;closed=false;openInternal();}catch(Exception e){events.status("Camera selection failed: "+e.getMessage());}});}
    public void capabilityReport(java.util.function.Consumer<String> callback){handler.post(()->{try{if(cameraId==null)throw new IllegalStateException("Camera is opening");String json=CameraCatalog.report(manager,cameraId).toString(2);try(FileOutputStream output=activity.openFileOutput("camera-capabilities.json",Activity.MODE_PRIVATE)){output.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));}activity.runOnUiThread(()->callback.accept(json));}catch(Exception e){events.status("Capability report unavailable: "+e.getMessage());}});}
    public void setFraming(boolean video,float ratio,int shortEdge,int fps){reconfiguring=true;handler.post(()->{
        framingStage="handler entered "+ratio;
        if(recording||busy||exporting||finalizing){android.util.Log.w("AabhixCamera","Framing rejected: recording="+recording+" busy="+busy+" exporting="+exporting+" finalizing="+finalizing+" capturePending="+capturePending.get());reconfiguring=false;events.status("Finish capture before changing framing");return;}videoMode=video;if(video)videoRatio=ratio;else photoRatio=ratio;videoShortEdge=shortEdge;videoFps=fps;
        if(opening&&camera==null){framingPending=true;return;}if(camera==null)reconfiguring=false;
        if(camera!=null){try{
            CameraCaptureSession previous=session;session=null;repeating=null;lifecycleGeneration+=2;framingStage="closing previous";if(previous!=null){previous.stopRepeating();previous.close();}
            framingStage="configuring graphics";configureGraphics();framingStage="starting preview";startPreview();framingStage="waiting for session";
        }catch(Exception e){reconfiguring=false;android.util.Log.e("AabhixCamera","Reconfiguration failed",e);events.status("Framing unavailable: "+e.getMessage());}}
    });}
    private void configureGraphics()throws Exception{

        Integer sensor=characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);relativeRotation=com.aabhix.camera.domain.FrameGeometry.rotation(sensor==null?90:sensor,displayDegrees,front);preview.setCameraOrientation(relativeRotation,front);
        if(videoMode&&!offersVideo(videoRatio,videoShortEdge,videoFps)){videoShortEdge=720;videoFps=30;videoRatio=16f/9;events.videoFormat(videoShortEdge,videoFps);events.status("Selected profile is unavailable on this lens · restored 720p30");}
        preview.setVideoMode(videoMode);
        StreamConfigurationMap map=characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);Size stream=videoMode?chooseVideoStream(map,videoRatio,videoShortEdge,videoFps):chooseSize(map.getOutputSizes(SurfaceTexture.class),photoWidth/(float)photoHeight,2073600);streamWidth=stream.getWidth();streamHeight=stream.getHeight();
        float landscape=videoMode?videoRatio:photoRatio==0?photoWidth/(float)photoHeight:photoRatio;currentAspect=com.aabhix.camera.domain.FrameGeometry.orientedAspect(landscape,relativeRotation);preview.configureFrame(streamWidth,streamHeight,currentAspect);
    }
    public void displayRotation(int degrees){handler.post(()->{if(closed||recording||degrees==displayDegrees)return;if(busy||exporting||finalizing||reconfiguring){handler.postDelayed(()->displayRotation(activity.getWindowManager().getDefaultDisplay().getRotation()*90),100);return;}displayDegrees=degrees;if(camera!=null&&characteristics!=null){try{reconfiguring=true;CameraCaptureSession previous=session;session=null;repeating=null;lifecycleGeneration+=2;if(previous!=null){previous.stopRepeating();previous.close();}configureGraphics();startPreview();}catch(Exception e){reconfiguring=false;events.status("Rotation unavailable: "+e.getMessage());}}});}
    public void rendererFailed(String message){rendererDiagnostic=message;handler.post(()->{if(recording)stopVideo();events.status(message);});}
    public void toggleRecording(boolean audio){if(!recording&&!recordingPending.compareAndSet(false,true)){events.status("Recorder is preparing");return;}handler.post(()->{if(recording){stopVideo();return;}if(camera==null||session==null||busy||exporting||finalizing){recordingPending.set(false);events.recording(false);events.status("Camera not ready");return;}
        try{
            state(RecordingState.PREPARING);int actualDisplay=activity.getWindowManager().getDefaultDisplay().getRotation()*90;if(actualDisplay!=displayDegrees){recordingPending.set(false);state(RecordingState.IDLE);events.recording(false);displayRotation(actualDisplay);events.status("Orientation changed · wait for preview, then tap record");return;}int shortEdge=videoShortEdge;float ratio=Math.max(currentAspect,1/currentAspect);int longEdge=Math.round(shortEdge*ratio/2)*2;
            int videoWidth=currentAspect>1?longEdge:shortEdge,videoHeight=currentAspect>1?shortEdge:longEdge;
            if(!offersVideo(videoRatio,videoShortEdge,videoFps))throw new IllegalStateException("Current lens cannot supply the selected video profile");ensureEncoder(videoWidth,videoHeight,videoFps);
            File recovery=new File(activity.getFilesDir(),"recovery");recovery.mkdirs();recordingFile=File.createTempFile("video-", ".mp4",recovery);recorder=new MediaRecorder();
            boolean mic=audio&&activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
            if(mic)recorder.setAudioSource(MediaRecorder.AudioSource.MIC);recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setOutputFile(recordingFile.getAbsolutePath());recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);recorder.setVideoSize(videoWidth,videoHeight);recorder.setVideoFrameRate(videoFps);recorder.setVideoEncodingBitRate(shortEdge>=1080?16000000:8000000);
            if(mic){recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);recorder.setAudioSamplingRate(48000);recorder.setAudioEncodingBitRate(128000);}
            recorder.setMaxDuration(600000);recorder.setMaxFileSize(512L*1024*1024);recorder.setOnInfoListener((r,what,extra)->handler.post(()->handleRecorderInfo(r,what,extra)));
            recorder.setOnErrorListener((r,what,extra)->handler.post(()->{if(r!=recorder)return;recorderDiagnostic="error "+what+"/"+extra;if(recording)stopVideo();events.status("Encoder error "+what+"/"+extra+"; recovery file preserved");}));
            recorder.prepare();recorder.start();preview.startRecording(recorder.getSurface(),videoWidth,videoHeight);recorderDiagnostic="started mic="+mic;recording=true;recordingPending.set(false);state(RecordingState.RECORDING);recordStarted=SystemClock.elapsedRealtime();updateRequest();events.recording(true);events.status("Recording "+videoWidth+"×"+videoHeight+" SDR · baked style"+(mic?" · microphone":" · silent"));
        }catch(Exception e){recordingPending.set(false);state(RecordingState.ERROR);releaseRecorder();events.recording(false);events.status("Video unavailable: "+e.getMessage());}
    });}
    private void handleRecorderInfo(MediaRecorder owner,int what,int extra){recorderDiagnostic="info "+what+"/"+extra;android.util.Log.i("AabhixCamera","Recorder info "+what+"/"+extra);if(owner==recorder&&recording&&(what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED||what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED))stopVideo();}
    private void stopVideo(){if(!recording)return;recording=false;finalizing=true;state(RecordingState.FINALIZING);
        try{preview.stopRecording();recorder.stop();File ready=recordingFile;recorder.release();recorder=null;recordingFile=null;
            if(SystemClock.elapsedRealtime()-recordStarted<500){finalizing=false;state(RecordingState.ERROR);events.status("Clip too short; retained in recovery. Record at least one second.");}
            else new Thread(()->{try{events.saved(new MediaRepository(activity).saveVideo(ready));ready.delete();state(RecordingState.SAVED);}catch(Exception e){state(RecordingState.ERROR);events.status("Video export failed; clip retained for recovery: "+ready.getName());}finally{finalizing=false;}},"AabhixVideoExport").start();
        }catch(Exception e){releaseRecorder();finalizing=false;state(RecordingState.ERROR);events.status("Recording could not be finalized: "+e.getMessage());}finally{if(recorder!=null||recordingFile!=null)finalizing=false;updateRequest();events.recording(false);}
    }
    private void releaseRecorder(){try{preview.stopRecording();}catch(Exception ignored){}if(recorder!=null){try{recorder.reset();recorder.release();}catch(Exception ignored){}recorder=null;}recordingFile=null;}
    public String profile(){
        if(characteristics==null)return "Camera unavailable";
        return "Camera "+cameraId+" · "+(front?"front":"rear")+"\nHardware level: "+characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)+"\nISO: "+characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)+"\nExposure ns: "+characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)+"\nEV steps: "+characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)+"\nFocus diopters: "+characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)+"\nDigital zoom: "+characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)+"\nCapabilities: "+Arrays.toString(characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES))+"\nJPEG: "+photoWidth+"×"+photoHeight+"\nGPU preview: "+streamWidth+"×"+streamHeight+" sensor stream\nOutput aspect: "+currentAspect+"; video H.264 SDR / requested "+videoFps+" fps\nNo hardware stabilization or HDR claim; actual clip must be tested.";
    }
    public int evMin(){Range<Integer> r=characteristics==null?null:characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);return r==null?0:r.getLower();}
    public int evMax(){Range<Integer> r=characteristics==null?null:characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);return r==null?0:r.getUpper();}
    public float zoomMax(){Float v=characteristics==null?null:characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);return v==null?1:v;}
    public boolean hasManualFocus(){Float v=characteristics==null?null:characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);return v!=null&&v>0;}
    public int currentEv(){return compensation;}
    public float evStep(){android.util.Rational step=characteristics==null?null:characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);return step==null?0:step.floatValue();}
    public float currentZoom(){return zoom;}
    public float currentFocus(){return focus;}
    public boolean isBusy(){return reconfiguring||opening||busy||exporting||recording||finalizing||capturePending.get()||recordingPending.get();}
    public TotalCaptureResult latestResult(){return latestResult;}
    public float currentAspect(){return currentAspect;}
    private static Size chooseVideoStream(StreamConfigurationMap map,float ratio,int edge,int fps){
        if(map==null)throw new IllegalArgumentException("No camera stream map");int longEdge=Math.round(edge*Math.max(ratio,1/ratio)/2)*2;Size best=null;double score=Double.MAX_VALUE;Size[] sizes=map.getOutputSizes(SurfaceTexture.class);if(sizes!=null)for(Size size:sizes){if(size.getWidth()<longEdge||size.getHeight()<edge)continue;long duration=map.getOutputMinFrameDuration(SurfaceTexture.class,size);if(duration<=0||duration>1_000_000_000L/fps+1000000)continue;double candidate=Math.abs(Math.log(size.getWidth()/(double)size.getHeight()/ratio))*100+Math.abs(size.getWidth()*(double)size.getHeight()-edge*(double)longEdge)/(edge*(double)longEdge);if(candidate<score){score=candidate;best=size;}}if(best==null)throw new IllegalArgumentException("No camera stream supports requested dimensions/rate");return best;
    }
    public boolean offersVideo(float ratio,int edge,int fps){
        CameraCharacteristics info=characteristics;if(info==null||!supportsVideo(ratio,edge,fps))return false;
        try{Range<Integer>[] ranges=info.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);boolean rate=false;if(ranges!=null)for(Range<Integer> range:ranges)if(range.getLower()<=fps&&range.getUpper()==fps)rate=true;if(!rate)return false;chooseVideoStream(info.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP),ratio,edge,fps);return true;}catch(RuntimeException failure){return false;}
    }
    public boolean offersLandscapeVideo(float ratio,int edge,int fps){CameraCharacteristics info=characteristics;if(info==null)return false;try{ensureEncoder(Math.round(edge*Math.max(ratio,1/ratio)/2)*2,edge,fps);Range<Integer>[] ranges=info.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);boolean rate=false;if(ranges!=null)for(Range<Integer> range:ranges)if(range.getLower()<=fps&&range.getUpper()==fps)rate=true;if(!rate)return false;chooseVideoStream(info.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP),ratio,edge,fps);return true;}catch(RuntimeException failure){return false;}}
    public boolean supportsVideo(float ratio,int shortEdge,int fps){try{float oriented=com.aabhix.camera.domain.FrameGeometry.orientedAspect(ratio,relativeRotation);int longEdge=Math.round(shortEdge*Math.max(oriented,1/oriented)/2)*2;ensureEncoder(oriented>1?longEdge:shortEdge,oriented>1?shortEdge:longEdge,fps);return true;}catch(RuntimeException e){return false;}}
    public void close(){closed=true;java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);handler.post(()->{try{closeInternal();}finally{done.countDown();}});try{done.await(5,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
    public void closeAsync(Runnable done){closed=true;handler.post(()->{try{closeInternal();}finally{done.run();}});}
    private void closeInternal(){reconfiguring=false;lifecycleGeneration++;opening=false;captureGeneration++;focusGeneration++;tapRegions=null;latestResult=null;busy=false;if(recording)stopVideo();if(session!=null){session.close();session=null;}repeating=null;if(camera!=null){camera.close();camera=null;}if(reader!=null){reader.close();reader=null;}}
    private static Size chooseSize(Size[] sizes,float ratio,long maxPixels){
        if(sizes==null||sizes.length==0)throw new IllegalStateException("No supported output sizes");Size best=null;double bestScore=Double.MAX_VALUE;
        for(Size size:sizes){long pixels=(long)size.getWidth()*size.getHeight();double score=(pixels>maxPixels?1000:0)+(ratio==0?0:Math.abs(Math.log((size.getWidth()/(double)size.getHeight())/ratio)))*100-pixels/(double)maxPixels;
            if(score<bestScore){best=size;bestScore=score;}}
        return best;
    }
    private static void ensureEncoder(int width,int height,int fps){
        for(MediaCodecInfo info:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos())if(info.isEncoder())for(String mime:info.getSupportedTypes())if(mime.equalsIgnoreCase("video/avc")){
            MediaCodecInfo.VideoCapabilities caps=info.getCapabilitiesForType(mime).getVideoCapabilities();if(caps!=null&&caps.areSizeAndRateSupported(width,height,fps))return;
        }throw new IllegalStateException("H.264 encoder does not support "+width+"×"+height+" @ "+fps);
    }
    public void destroy(){closed=true;handler.post(()->{closeInternal();thread.quitSafely();});}
}






