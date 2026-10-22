package com.aabhix.camera;

import android.app.*;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

/** One panel for all sensor controls. Applies to the active photo or video pipeline. */
public final class ProControlsDialog {
    private ProControlsDialog(){}
    public static void show(Activity activity,CameraController camera){
        LinearLayout form=new LinearLayout(activity);form.setOrientation(LinearLayout.VERTICAL);int pad=(int)(18*activity.getResources().getDisplayMetrics().density);form.setPadding(pad,pad,pad,pad);
        TextView ranges=label(activity,camera.sensorRanges());form.addView(ranges);
        CheckBox manual=check(activity,"Manual exposure · ISO + shutter",camera.manualIso()>0);manual.setEnabled(camera.hasManualSensor());form.addView(manual);
        EditText iso=number(activity,"ISO",String.valueOf(camera.manualIso()>0?camera.manualIso():200),false);form.addView(label(activity,"ISO"));form.addView(iso);
        EditText shutter=number(activity,"Shutter milliseconds",String.valueOf(camera.manualExposure()/1_000_000.0),true);form.addView(label(activity,"Shutter · milliseconds (8 = 1/125 s)"));form.addView(shutter);
        TextView evValue=label(activity,"EV compensation · "+camera.currentEv()+" camera steps");form.addView(evValue);SeekBar ev=new SeekBar(activity);ev.setMax(camera.evMax()-camera.evMin());ev.setProgress(camera.currentEv()-camera.evMin());form.addView(ev);ev.setOnSeekBarChangeListener(seek(p->evValue.setText("EV compensation · "+(camera.evMin()+p)+" camera steps")));
        CheckBox focus=check(activity,"Manual focus",camera.currentFocus()>=0);focus.setEnabled(camera.hasManualFocus());form.addView(focus);SeekBar distance=new SeekBar(activity);distance.setMax(100);distance.setProgress(Math.round(Math.max(0,camera.currentFocus())*100));distance.setEnabled(focus.isChecked());form.addView(distance);form.addView(label(activity,"Infinity ← focus distance → near"));focus.setOnCheckedChangeListener((v,on)->distance.setEnabled(on));
        form.addView(label(activity,"Digital zoom"));SeekBar zoom=new SeekBar(activity);zoom.setMax(100);zoom.setProgress(camera.zoomMax()>1?Math.round((camera.currentZoom()-1)/(camera.zoomMax()-1)*100):0);zoom.setEnabled(camera.zoomMax()>1);form.addView(zoom);
        ArrayList<Integer> wbModes=new ArrayList<>();ArrayList<String> wbNames=new ArrayList<>();String[] labels={"Off","Auto","Incandescent","Fluorescent","Warm fluorescent","Daylight","Cloudy","Twilight","Shade"};for(int mode:camera.whiteBalanceModes())if(mode>0&&mode<labels.length){wbModes.add(mode);wbNames.add(labels[mode]);}form.addView(label(activity,"White balance"));Spinner wb=spinner(activity,wbNames);wb.setSelection(Math.max(0,wbModes.indexOf(camera.whiteBalance())));form.addView(wb);
        CheckBox ae=check(activity,"Lock automatic exposure",camera.exposureLocked());ae.setEnabled(camera.hasExposureLock()&&!manual.isChecked());form.addView(ae);CheckBox awb=check(activity,"Lock white balance",camera.whiteBalanceLocked());awb.setEnabled(camera.hasWhiteBalanceLock());form.addView(awb);
        Runnable enabled=()->{iso.setEnabled(manual.isChecked()&&manual.isEnabled());shutter.setEnabled(manual.isChecked()&&manual.isEnabled());ev.setEnabled(!manual.isChecked()&&camera.evMax()>camera.evMin());ae.setEnabled(!manual.isChecked()&&camera.hasExposureLock());};manual.setOnCheckedChangeListener((v,on)->enabled.run());enabled.run();
        TextView error=label(activity,"");error.setTextColor(0xffff7777);form.addView(error);form.addView(label(activity,"Values apply to the active camera. Long exposures lower the delivered frame rate. EV works with automatic exposure. Fixed aperture and unsupported controls cannot be changed."));
        ScrollView scroll=new ScrollView(activity);scroll.addView(form);AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Pro · exposure / focus / white balance").setView(scroll).setPositiveButton("Apply",null).setNegativeButton("Close",null).setNeutralButton("Auto reset",(d,w)->{camera.controls(0,1,-1);camera.proControls(false,200,8000000,1);camera.advancedControls(false,false,activity.getSharedPreferences("settings",0).getBoolean("recordWbLock",false)&&camera.hasWhiteBalanceLock(),camera.antiBanding());}).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{boolean isManual=manual.isEnabled()&&manual.isChecked();double ms=isManual?Double.parseDouble(shutter.getText().toString()):8;if(!Double.isFinite(ms)||ms<=0||ms>Long.MAX_VALUE/1_000_000.0)throw new IllegalArgumentException("Enter a valid shutter time");int sensitivity=isManual?Integer.parseInt(iso.getText().toString()):200;if(wbModes.isEmpty())throw new IllegalArgumentException("White balance unavailable");camera.proControls(isManual,sensitivity,(long)(ms*1_000_000),wbModes.get(wb.getSelectedItemPosition()));camera.controls(camera.evMin()+ev.getProgress(),1+(camera.zoomMax()-1)*zoom.getProgress()/100f,focus.isEnabled()&&focus.isChecked()?distance.getProgress()/100f:-1);camera.advancedControls(!isManual&&ae.isEnabled()&&ae.isChecked(),awb.isEnabled()&&awb.isChecked(),activity.getSharedPreferences("settings",0).getBoolean("recordWbLock",false)&&camera.hasWhiteBalanceLock(),camera.antiBanding());dialog.dismiss();}catch(Exception failure){error.setText(failure.getMessage());}}));dialog.show();
    }
    private static TextView label(Activity a,String value){TextView t=new TextView(a);t.setText(value);t.setTextColor(Color.LTGRAY);t.setTextSize(13);t.setPadding(0,8,0,4);return t;}
    private static CheckBox check(Activity a,String label,boolean value){CheckBox c=new CheckBox(a);c.setText(label);c.setChecked(value);return c;}
    private static EditText number(Activity a,String hint,String value,boolean decimal){EditText e=new EditText(a);e.setHint(hint);e.setText(value);e.setSingleLine();e.setInputType(InputType.TYPE_CLASS_NUMBER|(decimal?InputType.TYPE_NUMBER_FLAG_DECIMAL:0));return e;}
    private static Spinner spinner(Activity a,List<String> names){Spinner s=new Spinner(a);s.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,names));return s;}
    private interface Change{void value(int n);}
    private static SeekBar.OnSeekBarChangeListener seek(Change c){return new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){c.value(p);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};}
}
