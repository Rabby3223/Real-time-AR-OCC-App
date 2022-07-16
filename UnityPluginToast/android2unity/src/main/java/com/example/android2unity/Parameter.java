package com.example.android2unity;

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_AUTO;
import static android.hardware.camera2.CameraMetadata.CONTROL_MODE_AUTO;
import static android.hardware.camera2.CameraMetadata.CONTROL_MODE_USE_SCENE_MODE;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_DISABLED;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Parameter extends Activity implements View.OnClickListener {

    String cameraId;
    StreamConfigurationMap map;
    CameraCharacteristics characteristics = null;

    Boolean highSpeed;
    Size videoSize;
    Range<Integer> frameRate;
    int ControlMode;
    int SceneMode;
    int AWBmode;
    int EffectMode;
    Integer AFmodeON;
    Integer AEmodeON;
    Long ExposureTime;
    Integer Sensitivity;
    Float FocalDistance;
    Float mZoom;
    int recordTime;
    int refInterval;

    Button High_speed_button;
    Button Video_size_button;
    Button Frame_rate_button;
    Button Effect_button;
    Button AEmode_button;
    Button AWBmode_button;
    Button Control_button;
    Button Scene_button;
    Button Submit_button;
    Button ExposureTime_button;
    Button Sensitivity_button;
    Button FocalDistance_button;
    Button AFmode_button;
    Button Zoom_button;
    Button RecordT_button;
    Button RefI_button;

    String[] ControlString = {"OFF","AUTO","USE_SCENE_MODE","OFF_KEEP_STATE"};
    String[] SceneString = {"DISABLED","FACE_PRIORITY", "ACTION","PORTRAIT", "LANDSCAPE","NIGHT", "NIGHT_PORTRAIT", "THEATRE","BEACH","SNOW", "SUNSET", "STEADYPHOTO",
            "FIREWORKS", "SPORTS",  "PARTY","CANDLELIGHT","BARCODE","HIGH_SPEED_VIDEO", "HDR"};
    String[] AWBstring = {"OFF","AUTO","INCANDESCENT","FLUORESCENT","WARM_FLUORESCENT",
            "DAYLIGHT","CLOUDY_DAYLIGHT","TWILIGHT","SHADE"};
    String[] EffectString = {"OFF","MONO","NEGATIVE","SOLARIZE","SEPIA","POSTERIZE","WHITEBOARD","BLACKBOARD","QAUA"};
    String[] AFstring = {"OFF","AUTO","MACRO","CONTINUOUS VIDEO","CONTINUOUS PICTURE","EDOF"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.parameter);

        cameraId = getIntent().getStringExtra("cameraId");
        CameraManager manager = (CameraManager) Parameter.this.getSystemService(Context.CAMERA_SERVICE);
        try {
            characteristics = manager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        map = characteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        High_speed_button = findViewById(R.id.highSpeed);
        High_speed_button.setOnClickListener(this);
        Video_size_button = findViewById(R.id.videoSize);
        Video_size_button.setOnClickListener(this);
        Frame_rate_button = findViewById(R.id.frameRate);
        Frame_rate_button.setOnClickListener(this);
        AWBmode_button = findViewById(R.id.AWBmode);
        AWBmode_button.setOnClickListener(this);
        Effect_button = findViewById(R.id.EffectMode);
        Effect_button.setOnClickListener(this);
        Control_button = findViewById(R.id.ControlMode);
        Control_button.setOnClickListener(this);
        Scene_button = findViewById(R.id.SceneMode);
        Scene_button.setOnClickListener(this);
        Submit_button = findViewById(R.id.submit);
        Submit_button.setOnClickListener(this);
        AEmode_button = findViewById(R.id.AEmode);
        AEmode_button.setOnClickListener(this);
        ExposureTime_button = findViewById(R.id.exposureTime);
        ExposureTime_button.setOnClickListener(this);
        Sensitivity_button = findViewById(R.id.sensitivity);
        Sensitivity_button.setOnClickListener(this);
        FocalDistance_button = findViewById(R.id.focalDistnace);
        FocalDistance_button.setOnClickListener(this);
        AFmode_button = findViewById(R.id.AFmode);
        AFmode_button.setOnClickListener(this);
        Zoom_button = findViewById(R.id.Zoom);
        Zoom_button.setOnClickListener(this);
        RecordT_button = findViewById(R.id.recordT);
        RecordT_button.setOnClickListener(this);
        RefI_button = findViewById(R.id.refI);
        RefI_button.setOnClickListener(this);

        highSpeed = getIntent().getBooleanExtra("mHighSpeed",false);
        videoSize = new Size(getIntent().getIntExtra("mVideoSize_width", 0),
                getIntent().getIntExtra("mVideoSize_height", 0));
        frameRate = new Range(getIntent().getIntExtra("mFrameRate_lower", 0),
                getIntent().getIntExtra("mFrameRate_upper", 0));
        ControlMode = getIntent().getIntExtra("mControlMode",0);
        SceneMode = getIntent().getIntExtra("mSceneMode",0);
        AWBmode = getIntent().getIntExtra("mAWBmode",0);
        EffectMode = getIntent().getIntExtra("mEffectMode",0);
        AEmodeON = getIntent().getIntExtra("mAEmodeON",0);
        AFmodeON = getIntent().getIntExtra("mAFmodeON",0);
        FocalDistance = getIntent().getFloatExtra("mFocalDistance",0);
        ExposureTime = getIntent().getLongExtra("mExposure",0);
        Sensitivity = getIntent().getIntExtra("mSensitivity",0);
        mZoom = getIntent().getFloatExtra("mZoom",0);
        recordTime=  getIntent().getIntExtra("mRecordTime",0);


        ExposureTime_button.setText("Exposure time: "+String.valueOf(getIntent().getLongExtra("mExposure",0)));
        Sensitivity_button.setText("Sensitivity: "+String.valueOf(getIntent().getIntExtra("mSensitivity",0)));
        Frame_rate_button.setText("Frame rate: "+ String.valueOf(frameRate.getUpper()));
        Control_button.setText("Control mode: "+ ControlString[ControlMode]);
        Effect_button.setText("Effect mode: "+EffectString[EffectMode]);
        Scene_button.setText("Scene mode: "+SceneString[SceneMode]);
        Video_size_button.setText("Resolution: "+videoSize);
        FocalDistance_button.setText("Focal distance: "+String.valueOf(FocalDistance));
        Zoom_button.setText("Zoom"+String.valueOf(mZoom));
        RecordT_button.setText("Record Time: "+String.valueOf(recordTime));


        if(highSpeed==false){
            High_speed_button.setText("High speed video: OFF");
        }else{
            High_speed_button.setText("High speed video: ON");
        }
        if(ControlMode==CONTROL_MODE_AUTO || ControlMode==CONTROL_MODE_USE_SCENE_MODE) {
            AEmodeON = CONTROL_AE_MODE_ON;
            AEmode_button.setText("AE mode: ON /");
            AWBmode = CONTROL_AWB_MODE_AUTO;
            AWBmode_button.setText("AWB mode: AUTO /");
            AFmodeON = CONTROL_AF_MODE_CONTINUOUS_PICTURE;
            AFmode_button.setText("AF mode: CONTINUOUS PICTURE /");
        }else {
            if (AEmodeON == CONTROL_AE_MODE_ON) {
                AEmode_button.setText("AE mode: ON");
            } else {
                AEmode_button.setText("AE mode: OFF");
            }
            AWBmode_button.setText("AWB mode: "+AWBstring[AWBmode]);
            AFmode_button.setText("AF mode: "+AFstring[AFmodeON]);
        }
        if(ControlMode==CONTROL_MODE_USE_SCENE_MODE) {
            Scene_button.setText("Scene mode: "+SceneString[SceneMode]);
        }else{
            SceneMode = CONTROL_SCENE_MODE_DISABLED;
            Scene_button.setText("Scene mode: DISABLED /");
        }
    }

    public static String addComma(String str) {
        DecimalFormat decimalFormat = new DecimalFormat(",###");
        return decimalFormat.format(Double.parseDouble(str));
    }

    private RadioOnClick_videoSize radioOnClick_videoSize = new RadioOnClick_videoSize(1);
    Size[] videoSizeList;
    String[] videoSizeString;

    private RadioOnClick_ControlMode radioOnClick_ControlMode = new RadioOnClick_ControlMode(1);
    int[] ControlList;
    String[] ControlShowString;

    private RadioOnClick_frameRate radioOnClick_frameRate = new RadioOnClick_frameRate(1);
    Range<Integer>[] frameRateList;
    String[] frameRateString;

    private RadioOnClick_SceneMode radioOnClick_SceneMode = new RadioOnClick_SceneMode(1);
    int[] SceneList;
    String[] SceneShowString;

    private RadioOnClick_AWBmode radioOnClick_AWBmode = new RadioOnClick_AWBmode(1);
    int[] AWBlist;
    String[] AWBshowString;

    private RadioOnClick_EffectMode radioOnClick_EffectMode = new RadioOnClick_EffectMode(1);
    int[] EffectList;
    String[] EffectShowString;

    private RadioOnClick_AFmode radioOnClick_AFmode = new RadioOnClick_AFmode(1);
    int[] AFlist;
    String[] AFshowString;

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.highSpeed) {
            if (highSpeed == false) {
                highSpeed = true;
                High_speed_button.setText("High speed video: ON");
                frameRate = new Range(0, 120);
                videoSize = map.getHighSpeedVideoSizes()[0];
            } else {
                highSpeed = false;
                High_speed_button.setText("High speed video: OFF");
            }
        } else if (id == R.id.videoSize) {
            if (highSpeed == false) {
                videoSizeList = map.getOutputSizes(MediaRecorder.class);
            } else {
                videoSizeList = map.getHighSpeedVideoSizes();
            }
            videoSizeString = new String[videoSizeList.length];

            for (int i = 0; i < videoSizeList.length; i++) {
                videoSizeString[i] = videoSizeList[i].getWidth() + "x" + videoSizeList[i].getHeight();
            }

            if (null != Parameter.this) {
                Toast.makeText(Parameter.this, "Current resolution： " + videoSize, Toast.LENGTH_LONG).show();
                AlertDialog ad =
                        new AlertDialog.Builder(Parameter.this).setTitle("Set resolution").setSingleChoiceItems(videoSizeString, radioOnClick_videoSize.getIndex(), radioOnClick_videoSize).create();
                ad.show();
            }
        } else if (id == R.id.ControlMode) {
            ControlList = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_MODES);
            ControlShowString = new String[ControlList.length];
            for (int i = 0; i < ControlList.length; i++) {
                ControlShowString[i] = ControlString[ControlList[i]];
            }
            if (null != Parameter.this) {
                Toast.makeText(Parameter.this, "Current Control mode： " + ControlString[ControlMode],
                        Toast.LENGTH_LONG).show();
                AlertDialog ad =
                        new AlertDialog.Builder(Parameter.this).setTitle("Set Control mode").setSingleChoiceItems(ControlShowString, radioOnClick_ControlMode.getIndex(), radioOnClick_ControlMode).create();
                ad.show();
            }
        } else if (id == R.id.SceneMode) {
            if (ControlMode != CONTROL_MODE_USE_SCENE_MODE) {
                Toast.makeText(Parameter.this, "Current Scene mode cannot be changed", Toast.LENGTH_LONG).show();
            } else {
                SceneList = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
                SceneShowString = new String[SceneList.length];
                for (int i = 0; i < SceneList.length; i++) {
                    SceneShowString[i] = SceneString[SceneList[i]];
                }
                if (null != Parameter.this) {
                    Toast.makeText(Parameter.this, "Current Scene mode： " + SceneString[SceneMode],
                            Toast.LENGTH_LONG).show();
                    AlertDialog ad =
                            new AlertDialog.Builder(Parameter.this).setTitle("Set Scene mode").setSingleChoiceItems(SceneShowString, radioOnClick_SceneMode.getIndex(), radioOnClick_SceneMode).create();
                    ad.show();
                }
            }
        } else if (id == R.id.EffectMode) {
            EffectList = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
            EffectShowString = new String[EffectList.length];
            for (int i = 0; i < EffectList.length; i++) {
                EffectShowString[i] = EffectString[EffectList[i]];
            }
            if (null != Parameter.this) {
                Toast.makeText(Parameter.this, "Current Effect mode： " + EffectString[EffectMode], Toast.LENGTH_LONG).show();
                AlertDialog ad =
                        new AlertDialog.Builder(Parameter.this).setTitle("Set Effect mode").setSingleChoiceItems(EffectShowString, radioOnClick_EffectMode.getIndex(), radioOnClick_EffectMode).create();
                ad.show();
            }
        } else if (id == R.id.AWBmode) {
            if (ControlMode == CONTROL_MODE_AUTO || ControlMode == CONTROL_MODE_USE_SCENE_MODE) {
                Toast.makeText(Parameter.this, "Current AWB mode cannot be changed", Toast.LENGTH_LONG).show();
            } else {
                AWBlist = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
                AWBshowString = new String[AWBlist.length];
                for (int i = 0; i < AWBlist.length; i++) {
                    AWBshowString[i] = AWBstring[AWBlist[i]];
                }
                if (null != Parameter.this) {
                    Toast.makeText(Parameter.this, "Current AWB mode： " + AWBstring[AWBmode], Toast.LENGTH_LONG).show();
                    AlertDialog ad =
                            new AlertDialog.Builder(Parameter.this).setTitle("Set AWB mode").setSingleChoiceItems(AWBshowString, radioOnClick_AWBmode.getIndex(), radioOnClick_AWBmode).create();
                    ad.show();
                }
            }
        } else if (id == R.id.AEmode) {
            if (ControlMode == CONTROL_MODE_AUTO || ControlMode == CONTROL_MODE_USE_SCENE_MODE) {
                Toast.makeText(Parameter.this, "Current AE mode cannot be changed", Toast.LENGTH_LONG).show();
            } else {
                if (AEmodeON == CONTROL_AE_MODE_ON) {
                    AEmodeON = CONTROL_AE_MODE_OFF;
                    AEmode_button.setText("AE mode: OFF");
                    Toast.makeText(Parameter.this, "Exposure time and sensitivity cannot be changed",
                            Toast.LENGTH_LONG);
                } else {
                    AEmodeON = CONTROL_AE_MODE_ON;
                    AEmode_button.setText("AE mode: ON");
                }
            }
        } else if (id == R.id.exposureTime) {
            if (AEmodeON == CONTROL_AE_MODE_ON) {
                Toast.makeText(Parameter.this, "Current exposure time cannot be changed", Toast.LENGTH_LONG).show();
            } else {
                String temp1 =
                        "Exposure time " + "   min: " + addComma(characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE).getLower().toString()) + "ns    max: " + addComma(characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE).getUpper().toString()) + "ns";
                final EditText edit = new EditText(this);
                AlertDialog ad =
                        new AlertDialog.Builder(Parameter.this).setTitle(temp1).setView(edit).setPositiveButton(
                                "Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ExposureTime = Long.valueOf(edit.getText().toString());
                        ExposureTime_button.setText("Exposure time: " + String.valueOf(ExposureTime));
                        dialog.dismiss();
                    }
                }).create();

                ad.show();
            }
        } else if (id == R.id.sensitivity) {
            if (AEmodeON == CONTROL_AE_MODE_ON) {
                Toast.makeText(Parameter.this, "Current exposure time cannot be changed", Toast.LENGTH_LONG).show();
            } else {
                String temp2 =
                        "Sensitivity " + "   min: " + addComma(characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE).getLower().toString()) + "    max: " + addComma(characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE).getUpper().toString());
                final EditText edit = new EditText(this);
                AlertDialog ad =
                        new AlertDialog.Builder(Parameter.this).setTitle(temp2).setView(edit).setPositiveButton(
                                "Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Sensitivity = Integer.valueOf(edit.getText().toString());
                        Sensitivity_button.setText("Sensitivity: " + String.valueOf(Sensitivity));
                        dialog.dismiss();
                    }
                }).create();

                ad.show();
            }
        } else if (id == R.id.frameRate) {
            frameRateList = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            List<Integer> listTemp = new ArrayList();
            for (int i = 0; i < frameRateList.length; i++) {
                if (!listTemp.contains(frameRateList[i].getUpper())) listTemp.add(frameRateList[i].getUpper());
            }
            frameRateString = new String[listTemp.size()];
            for (int i = 0; i < listTemp.size(); i++) {
                frameRateString[i] = listTemp.get(i).toString();
            }
            Toast.makeText(Parameter.this, "Current frame rate： " + frameRate.getUpper().toString(),
                    Toast.LENGTH_LONG).show();
            if (null != Parameter.this && AEmodeON == CONTROL_AE_MODE_ON) {

                AlertDialog ad =
                        new AlertDialog.Builder(Parameter.this).setTitle("Set frame rate").setSingleChoiceItems(frameRateString, radioOnClick_frameRate.getIndex(), radioOnClick_frameRate).create();
                ad.show();
            } else {
                final EditText edit = new EditText(this);
                AlertDialog ad =
                        new AlertDialog.Builder(Parameter.this).setTitle("Set frame rate").setView(edit).setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        frameRate = new Range(0, Integer.valueOf(edit.getText().toString()));
                        Frame_rate_button.setText("Frame rate: " + String.valueOf(frameRate.getUpper()));
                        dialog.dismiss();
                    }
                }).create();

                ad.show();
            }
        } else if (id == R.id.AFmode) {
            if (ControlMode == CONTROL_MODE_AUTO || ControlMode == CONTROL_MODE_USE_SCENE_MODE) {
                Toast.makeText(Parameter.this, "Current AF mode cannot be changed", Toast.LENGTH_LONG).show();
            } else {
                AFlist = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
                AFshowString = new String[AFlist.length];
                for (int i = 0; i < AFlist.length; i++) {
                    AFshowString[i] = AFstring[AFlist[i]];
                }
                if (null != Parameter.this) {
                    Toast.makeText(Parameter.this, "Current AF mode： " + AFstring[AFmodeON], Toast.LENGTH_LONG).show();
                    AlertDialog ad =
                            new AlertDialog.Builder(Parameter.this).setTitle("Set AF mode").setSingleChoiceItems(AFshowString, radioOnClick_AFmode.getIndex(), radioOnClick_AFmode).create();
                    ad.show();
                }
            }
        } else if (id == R.id.focalDistnace) {
            String temp2 =
                    "Focal distance " + "   min: " + addComma(characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE).toString());
            final EditText edit = new EditText(this);
            AlertDialog ad = new AlertDialog.Builder(Parameter.this).setTitle(temp2).setView(edit).setPositiveButton(
                    "Submit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FocalDistance = Float.valueOf(edit.getText().toString());
                    FocalDistance_button.setText("Focal distance: " + String.valueOf(FocalDistance));
                    dialog.dismiss();
                }
            }).create();

            ad.show();
        } else if (id == R.id.Zoom) {
            String temp2 =
                    "Zoom " + "   max: " + addComma(characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM).toString());
            final EditText edit = new EditText(this);
            AlertDialog ad = new AlertDialog.Builder(Parameter.this).setTitle(temp2).setView(edit).setPositiveButton(
                    "Submit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mZoom = Float.valueOf(edit.getText().toString());
                    Zoom_button.setText("Zoom: " + String.valueOf(mZoom));
                    dialog.dismiss();
                }
            }).create();

            ad.show();
        } else if (id == R.id.recordT) {
            String temp2 = "Record Time (s)";
            final EditText edit = new EditText(this);
            AlertDialog ad = new AlertDialog.Builder(Parameter.this).setTitle(temp2).setView(edit).setPositiveButton(
                    "Submit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    recordTime = Integer.valueOf(edit.getText().toString());
                    RecordT_button.setText("Record Time: " + String.valueOf(recordTime));
                    dialog.dismiss();
                }
            }).create();

            ad.show();
        } else if (id == R.id.refI) {
            String temp2 = "Ref Interval (s)";
            final EditText edit = new EditText(this);
            AlertDialog ad = new AlertDialog.Builder(Parameter.this).setTitle(temp2).setView(edit).setPositiveButton(
                    "Submit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    refInterval = Integer.valueOf(edit.getText().toString());
                    RefI_button.setText("Ref Interval: " + String.valueOf(refInterval));
                    dialog.dismiss();
                }
            }).create();

            ad.show();
        } else if (id == R.id.submit) {
            Intent intent = new Intent();

            intent.putExtra("mHighSpeed", highSpeed);
            intent.putExtra("mVideoSize_width", videoSize.getWidth());
            intent.putExtra("mVideoSize_height", videoSize.getHeight());
            intent.putExtra("mFrameRate_lower", frameRate.getLower());
            intent.putExtra("mFrameRate_upper", frameRate.getUpper());
            intent.putExtra("mAWBmode", AWBmode);
            intent.putExtra("mEffectMode", EffectMode);
            intent.putExtra("mExposure", ExposureTime);
            intent.putExtra("mSensitivity", Sensitivity);
            intent.putExtra("mAEmodeON", AEmodeON);
            intent.putExtra("mControlMode", ControlMode);
            intent.putExtra("mSceneMode", SceneMode);
            intent.putExtra("mFocalDistance", FocalDistance);
            intent.putExtra("mAFmodeON", AFmodeON);
            intent.putExtra("mZoom", mZoom);
            intent.putExtra("mRecordTime", recordTime);
            intent.putExtra("mRefInterval", refInterval);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            Log.d("HERE", "Stop");
            Parameter.this.finish();
            Log.d("HERE", "Stop2");
        }
    }

    class RadioOnClick_videoSize implements DialogInterface.OnClickListener{
        private int index;

        public RadioOnClick_videoSize(int index){
            this.index = index;
        }
        public void setIndex(int index){
            this.index=index;
        }
        public int getIndex(){
            return index;
        }

        public void onClick(DialogInterface dialog, int whichButton){ ;
            setIndex(whichButton);
            videoSize = videoSizeList[index];
            Video_size_button.setText("Resolution: "+videoSize);
            Toast.makeText(Parameter.this, "您已经选择了： " + videoSizeString[index], Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }
    }

    class RadioOnClick_ControlMode implements DialogInterface.OnClickListener{
        private int index;

        public RadioOnClick_ControlMode(int index){
            this.index = index;
        }
        public void setIndex(int index){
            this.index=index;
        }
        public int getIndex(){
            return index;
        }

        public void onClick(DialogInterface dialog, int whichButton){
            setIndex(whichButton);
            ControlMode = ControlList[index];
            Control_button.setText("Control Mode: "+ ControlString[ControlMode]);
            if(ControlMode==CONTROL_MODE_AUTO || ControlMode==CONTROL_MODE_USE_SCENE_MODE) {
                AEmodeON = CONTROL_AE_MODE_ON;
                AEmode_button.setText("AE mode: ON /");
                AWBmode = CONTROL_AWB_MODE_AUTO;
                AWBmode_button.setText("AWB mode: AUTO /");
                AFmodeON= CONTROL_AF_MODE_CONTINUOUS_PICTURE;
                AFmode_button.setText("AF mode: CONTINUOUS PICTURE /");
            }else{
                if (AEmodeON == CONTROL_AE_MODE_ON) {
                    AEmode_button.setText("AE mode: ON");
                }else {
                    AEmode_button.setText("AE mode: OFF");
                }
                AWBmode_button.setText("AWB mode status: "+AWBstring[AWBmode]);
                AFmode_button.setText("AF mode: "+AFstring[AFmodeON]);

            }
            if(ControlMode==CONTROL_MODE_USE_SCENE_MODE) {
                Scene_button.setText("Scene mode: "+SceneString[SceneMode]);
            }else{
                SceneMode = CONTROL_SCENE_MODE_DISABLED;
                Scene_button.setText("Scene mode: DISABLED /");
            }


            Toast.makeText(Parameter.this, "您已经选择了： " + ControlString[ControlMode], Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }
    }

    class RadioOnClick_frameRate implements DialogInterface.OnClickListener{
        private int index;

        public RadioOnClick_frameRate(int index){
            this.index = index;
        }
        public void setIndex(int index){
            this.index=index;
        }
        public int getIndex(){
            return index;
        }

        public void onClick(DialogInterface dialog, int whichButton){
            setIndex(whichButton);
            Integer fpsTemp = Integer.valueOf(frameRateString[index]);
            frameRate = new Range(fpsTemp,fpsTemp);
            for(Range<Integer> range:frameRateList) {
                if (range.getUpper() == fpsTemp && range.getLower() < frameRate.getLower()) {
                    frameRate = new Range(range.getLower(), fpsTemp);
                }
            }
            Frame_rate_button.setText("Frame rate: "+ String.valueOf(frameRate.getUpper()));


            Toast.makeText(Parameter.this, "您已经选择了： " + frameRateString[index], Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }
    }

    class RadioOnClick_AWBmode implements DialogInterface.OnClickListener{
        private int index;

        public RadioOnClick_AWBmode(int index){
            this.index = index;
        }
        public void setIndex(int index){
            this.index=index;
        }
        public int getIndex(){
            return index;
        }

        public void onClick(DialogInterface dialog, int whichButton){
            setIndex(whichButton);
            AWBmode = AWBlist[index];
            AWBmode_button.setText("AWB mode: "+AWBstring[AWBmode]);
            Toast.makeText(Parameter.this, "您已经选择了： " + AWBstring[AWBmode], Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }
    }
    class RadioOnClick_AFmode implements DialogInterface.OnClickListener{
        private int index;

        public RadioOnClick_AFmode(int index){
            this.index = index;
        }
        public void setIndex(int index){
            this.index=index;
        }
        public int getIndex(){
            return index;
        }

        public void onClick(DialogInterface dialog, int whichButton){
            setIndex(whichButton);
            AFmodeON = AFlist[index];
            AFmode_button.setText("AF mode: "+AFstring[AFmodeON]);
            Toast.makeText(Parameter.this, "您已经选择了： " + AFstring[AFmodeON], Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }
    }

    class RadioOnClick_EffectMode implements DialogInterface.OnClickListener{
        private int index;

        public RadioOnClick_EffectMode(int index){
            this.index = index;
        }
        public void setIndex(int index){
            this.index=index;
        }
        public int getIndex(){
            return index;
        }

        public void onClick(DialogInterface dialog, int whichButton){
            setIndex(whichButton);
            EffectMode = EffectList[index];
            Effect_button.setText("Effect mode: "+EffectString[EffectMode]);
            Toast.makeText(Parameter.this, "您已经选择了： " + EffectString[EffectMode], Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }
    }
    class RadioOnClick_SceneMode implements DialogInterface.OnClickListener{
        private int index;

        public RadioOnClick_SceneMode(int index){
            this.index = index;
        }
        public void setIndex(int index){
            this.index=index;
        }
        public int getIndex(){
            return index;
        }

        public void onClick(DialogInterface dialog, int whichButton){
            setIndex(whichButton);
            SceneMode = SceneList[index];
            Scene_button.setText("Scene mode: "+SceneString[SceneMode]);
            Toast.makeText(Parameter.this, "您已经选择了： " + SceneString[SceneMode], Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }
    }


}