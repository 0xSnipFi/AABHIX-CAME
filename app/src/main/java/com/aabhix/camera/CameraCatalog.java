package com.aabhix.camera;

import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.*;
import android.graphics.ImageFormat;
import android.util.Size;
import org.json.*;
import java.util.*;

/** Public camera IDs only. Reported capabilities are not validated recording combinations. */
public final class CameraCatalog {
    private CameraCatalog(){}
    public static final class Choice {
        public final String id,label;
        Choice(String id,String label){this.id=id;this.label=label;}
    }
    public static List<Choice> cameras(CameraManager manager)throws CameraAccessException{
        List<Choice> choices=new ArrayList<>();for(String id:manager.getCameraIdList()){CameraCharacteristics c=manager.getCameraCharacteristics(id);Integer face=c.get(CameraCharacteristics.LENS_FACING);String facing=face==null?"unknown":face==CameraCharacteristics.LENS_FACING_FRONT?"Front":face==CameraCharacteristics.LENS_FACING_BACK?"Rear":"External";choices.add(new Choice(id,facing+" · ID "+id+" · "+Arrays.toString(c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS))+" mm"));}return choices;
    }
    private static JSONArray array(Object value)throws JSONException{JSONArray result=new JSONArray();if(value!=null)for(int i=0;i<java.lang.reflect.Array.getLength(value);i++)result.put(java.lang.reflect.Array.get(value,i));return result;}
    private static JSONArray sizes(Size[] values)throws JSONException{JSONArray result=new JSONArray();if(values!=null)for(Size size:values)result.put(size.getWidth()+"x"+size.getHeight());return result;}
    public static JSONObject report(CameraManager manager,String id)throws Exception{
        CameraCharacteristics c=manager.getCameraCharacteristics(id);JSONObject result=new JSONObject();result.put("scope","Advertised public capabilities; not session/encoder combination validation");result.put("id",id);result.put("hardware_level",c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL));result.put("facing",c.get(CameraCharacteristics.LENS_FACING));result.put("sensor_orientation",c.get(CameraCharacteristics.SENSOR_ORIENTATION));result.put("physical_ids",new JSONArray(new ArrayList<>(c.getPhysicalCameraIds())));result.put("focal_lengths_mm",array(c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)));result.put("apertures",array(c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)));result.put("capabilities",array(c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)));result.put("ae_fps_ranges",Arrays.toString(c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)));result.put("iso_range",String.valueOf(c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)));result.put("exposure_ns_range",String.valueOf(c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)));result.put("digital_zoom_max",c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM));result.put("flash",c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE));result.put("af_modes",array(c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)));result.put("focus_diopters_max",c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE));result.put("awb_modes",array(c.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)));result.put("ois_modes",array(c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)));result.put("video_stabilization_modes",array(c.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)));result.put("noise_reduction_modes",array(c.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)));result.put("edge_modes",array(c.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES)));
        StreamConfigurationMap map=c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);if(map!=null){result.put("output_formats",array(map.getOutputFormats()));result.put("jpeg_sizes",sizes(map.getOutputSizes(ImageFormat.JPEG)));result.put("raw_sizes",sizes(map.getOutputSizes(ImageFormat.RAW_SENSOR)));result.put("recorder_sizes",sizes(map.getOutputSizes(MediaRecorder.class)));}
        if(android.os.Build.VERSION.SDK_INT>=33){android.hardware.camera2.params.DynamicRangeProfiles profiles=c.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES);if(profiles!=null)result.put("dynamic_range_profiles",new JSONArray(new ArrayList<>(profiles.getSupportedProfiles())));}
        JSONArray encoders=new JSONArray();for(MediaCodecInfo encoder:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos())if(encoder.isEncoder())for(String mime:encoder.getSupportedTypes())if(mime.equalsIgnoreCase("video/avc")||mime.equalsIgnoreCase("video/hevc")){try{MediaCodecInfo.VideoCapabilities video=encoder.getCapabilitiesForType(mime).getVideoCapabilities();JSONObject entry=new JSONObject();entry.put("name",encoder.getName());entry.put("mime",mime);entry.put("hardware",encoder.isHardwareAccelerated());entry.put("width_range",String.valueOf(video.getSupportedWidths()));entry.put("height_range",String.valueOf(video.getSupportedHeights()));entry.put("fps_range",String.valueOf(video.getSupportedFrameRates()));encoders.put(entry);}catch(IllegalArgumentException ignored){/* A codec can advertise a type but reject querying it. */}}result.put("advertised_encoders",encoders);return result;
    }
}
