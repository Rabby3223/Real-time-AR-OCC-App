/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android2unity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_AUTO;
import static android.hardware.camera2.CameraMetadata.CONTROL_EFFECT_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.CONTROL_MODE_AUTO;
import static android.hardware.camera2.CameraMetadata.CONTROL_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_DISABLED;
import static com.example.android2unity.GenericGF.QR_CODE_FIELD_256;

import com.unity3d.player.UnityPlayer;

public class Camera2VideoFragment extends Fragment implements View.OnClickListener{

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    private static final String TAG = "Camera2VideoFragment";
    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int AUTO_EXPOSURE = 0;
    private static final int MANUAL_EXPOSURE = 1;
    private int expMode = MANUAL_EXPOSURE;

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * Button to record video
     */
    private Button mButtonVideo;
    private ImageButton mButtonSelect;
    private Button mProcessing;
    private Button mText;

    /**
     * A reference to the opened {@link android.hardware.camera2.CameraDevice}.
     */
    private CameraDevice mCameraDevice;


    /**
     * A reference to the current {@link android.hardware.camera2.CameraCaptureSession} for
     * preview.
     */
    private CameraCaptureSession mPreviewSession;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            try {
                parameter_init();
                openCamera(width, height);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * The {@link android.util.Size} of video recording.
     */


    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;

    /**
     * Whether the app is recording video now
     */
    private boolean mIsRecordingVideo;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);


    /**
     * Start the camera preview.
     */
    private boolean mHighSpeed = false;
    private Range<Integer>[] availableFpsRange;
    private Range<Integer> mFrameRate;
    private Integer mAEmodeON = CONTROL_AE_MODE_OFF;
    private int mAWBmode = CONTROL_AWB_MODE_AUTO;
    private int mEffectMode = CONTROL_EFFECT_MODE_OFF;
    private int mControlMode = CONTROL_MODE_OFF;
    private int mAFmodeON = CONTROL_AF_MODE_CONTINUOUS_PICTURE;
    private float mFocalDistance=0;
    private int mSceneMode = CONTROL_SCENE_MODE_DISABLED;
    private ImageReader mImageReader;


    /** PARAMETER SETTING
     *
     */
    //perspective transform: kernal size, coloumn step size, hello-lit.cpp
    private int kernal_size = 10;
    private int step = 20;
    //camera:
    private String cameraId="2";//huawei:0, oneplus:2,xiaomi:3
    private Size mVideoSize = new Size(1280,960);//huawei 1280 960, oneplus 1280,960,xiaomi:1920 1080
    private int recordTime = 60;
    private int refInterval = 60;
    private int mExposure_image = 2000000;
    private int mSensitivity_image = 200;
    private int mExposure_video = 40000;//huawei 50000,oneplus 50000,xiaomi 87000
    private int mSensitivity_video =1600;//huawei 3500,oneplus 1600,xiaomi 1000
    private long mExposure = mExposure_image;
    private Integer mSensitivity = mSensitivity_image;
    //communication;
    private float symbol_rate =30f;//

    private double frame_rate = 30;//huawei 25; onplus 30
    private float sampling_ratio = 3.525f;//oneplus: 3.525,huawei: 2.2857,xiaomi:4.42
    private float turning_point = 60;//huawei:0,oneplus:86
    //text_or_file;
    private int text_or_file = 1;//text:1, file:0
    private int pre_rs =1-text_or_file;


    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.release();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };



    private Integer mSensorOrientation;
    private String mNextVideoAbsolutePath;
    private CaptureRequest.Builder mPreviewBuilder;

    public static Camera2VideoFragment newInstance() {
        return new Camera2VideoFragment();
    }

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera2_video, container, false);
    }
    private ScrollView mScrollView;
    private TextView mTextView;
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = view.findViewById(R.id.texture);
        mButtonVideo = (Button) view.findViewById(R.id.video);
        mButtonSelect = (ImageButton) view.findViewById(R.id.camera);
        mProcessing = (Button) view.findViewById(R.id.processing);
        mText = (Button) view.findViewById(R.id.text);
        mScrollView = (ScrollView) view.findViewById(R.id.scrollview);
        mTextView = (TextView) view.findViewById(R.id.text_view2);
        mButtonVideo.setOnClickListener(this);
        mButtonSelect.setOnClickListener(this);
        mProcessing.setOnClickListener(this);
        view.findViewById(R.id.parameter).setOnClickListener(this);
        //mProcessing.setText(stringFromJNI());
        mText.setOnClickListener(this);
        view.findViewById(R.id.text).setOnClickListener(this);
        //mTextView.setText("");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("onResume","HERE");
        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            try {
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }


    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        mIsRecordingVideo = false;
        mButtonVideo.setText("Record");
        //mMediaRecorder.stop();

        //mMediaRecorder.reset();
        super.onPause();
    }

    private int mIsprocessing = 0;
    private RadioOnClick radioOnClick = new RadioOnClick(1);

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.video) {
            /*
            if (mIsRecordingVideo) {
                stopRecordingVideo();
            } else {
                startRecordingVideo();
            }

             */
        } else if (id == R.id.processing) {

            if (mIsprocessing == 0) {
                //packet_num_current = 0;
                //ss = new StringBuffer("");
                mIsprocessing = 1;
                scaling_done = 0;
                expMode = MANUAL_EXPOSURE;
                mExposure = mExposure_image;
                mSensitivity = mSensitivity_image;
                //refPoint_state = REFPOINT_START;
                handler_finished = 0;
                mProcessing.setText("Stop");
                Log.d("expMode", "processing click");
                myHandler.sendEmptyMessage(0x123);
            } else {
                mIsprocessing = 0;
                mProcessing.setText("Processing");
                handler_finished = 0;
                //refPoint_state = REFPOINT_START;
                //homo_pre = new float[]{1, 0, 0, 0, 1, 0, 0, 0, 1};
                //equal = new float[]{-0.0344262282406877f, -0.0242516605645861f, 0.0781810073375937f, -0
                // .107613566887167f, 0.0360030615282260f, -0.0949134563267548f, 0.344318916743703f, 0
                // .939099313553164f, -0.0233891837299242f, -0.197346346038834f};
                //arrlist.clear();
            }

        } else if (id == R.id.camera) {
            Activity activity = getActivity();
            if (null != activity) {
                Toast.makeText(activity, "当前相机： " + cameraDescribe[Integer.valueOf(cameraId)], Toast.LENGTH_LONG).show();
                AlertDialog ad =
                        new AlertDialog.Builder(activity).setTitle("选择相机").setSingleChoiceItems(cameraDescribe,
                                radioOnClick.getIndex(), radioOnClick).create();
                ad.show();
            }
        } else if (id == R.id.parameter) {
            closePreviewSession();
            Log.d("closePreview","Close");
            Activity mActivity = getActivity();
            Intent intent = new Intent(mActivity, Parameter.class);

            intent.putExtra("mHighSpeed", mHighSpeed);
            intent.putExtra("mVideoSize_width", mVideoSize.getWidth());
            intent.putExtra("mVideoSize_height", mVideoSize.getHeight());
            intent.putExtra("mFrameRate_lower", mFrameRate.getLower());
            intent.putExtra("mFrameRate_upper", mFrameRate.getUpper());
            intent.putExtra("mControlMode", mPreviewBuilder.get(CaptureRequest.CONTROL_MODE));
            intent.putExtra("mSceneMode", mPreviewBuilder.get(CaptureRequest.CONTROL_SCENE_MODE));
            intent.putExtra("mAWBmode", mPreviewBuilder.get(CaptureRequest.CONTROL_AWB_MODE));
            intent.putExtra("mEffectMode", mPreviewBuilder.get(CaptureRequest.CONTROL_EFFECT_MODE));
            intent.putExtra("mAEmodeON", mPreviewBuilder.get(CaptureRequest.CONTROL_AE_MODE));
            intent.putExtra("mExposure", mPreviewBuilder.get(CaptureRequest.SENSOR_EXPOSURE_TIME));
            intent.putExtra("mSensitivity", mPreviewBuilder.get(CaptureRequest.SENSOR_SENSITIVITY));
            intent.putExtra("cameraId", cameraId);
            intent.putExtra("mFocalDistance", mPreviewBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE));
            intent.putExtra("mAFmodeON", mPreviewBuilder.get(CaptureRequest.CONTROL_AF_MODE));
            intent.putExtra("mZoom", mPreviewBuilder.get(CaptureRequest.SCALER_CROP_REGION));
            intent.putExtra("mRecordTime", recordTime);
            intent.putExtra("mRefInterval", refInterval);
            startActivityForResult(intent, 1);
        } else if (id == R.id.text) {
            closePreviewSession();
            if(ss.toString()==null){
                SendMessageToUnity("hello from camera");
            }else{
                int n1 = ss.toString().indexOf("%");
                int n2 = ss.toString().indexOf("%",n1+1);
                SendMessageToUnity(ss.toString().substring(0,n1));
                SendMessageToUnity(ss.toString().substring(n1+1,n2));
                SendMessageToUnity(ss.toString().substring(n2+1,ss.length()));
            }
            Log.d("click_text",ss.toString());

            getActivity().finish();
            Log.d("startActivity","yes");


            /*
            Activity activity = getActivity();
            Intent intent = new Intent(activity, text.class);
            int a = 1;
            Log.d("codecode", String.valueOf(net_pack_num_bit));



            if(loading==false){
                intent.putExtra("String",ss.toString());
                intent.putExtra("loading",0);
            }else{
                intent.putExtra("loading",1);
            }


            startActivityForResult(intent, 1);

             */
        }
    }
    public void SendMessageToUnity(String str){
        UnityPlayer.UnitySendMessage("Canvas","ReceiveMessage",str);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case 1:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    mVideoSize = new Size(data.getIntExtra("mVideoSize_width",0),
                            data.getIntExtra("mVideoSize_height",0));
                    Log.d("size",mVideoSize.toString());

                    mHighSpeed = data.getBooleanExtra("mHighSpeed",false);
                    mFrameRate = new Range(data.getIntExtra("mFrameRate_lower",0),
                            data.getIntExtra("mFrameRate_upper",0));
                    Log.d("framerate",mFrameRate.toString());
                    mAEmodeON = data.getIntExtra("mAEmodeON",0);
                    mExposure = data.getLongExtra("mExposure",0);
                    mSensitivity = data.getIntExtra("mSensitivity",0);
                    mAWBmode = data.getIntExtra("mAWBmode",0);
                    mEffectMode = data.getIntExtra("mEffectMode",0);
                    mControlMode = data.getIntExtra("mControlMode",0);
                    mSceneMode = data.getIntExtra("mSceneMode",0);
                    mFocalDistance = data.getFloatExtra("mFocalDistance",0);
                    mAFmodeON = data.getIntExtra("mAFmodeON",0);
                    recordTime = data.getIntExtra("mRecordTime",0);
                    refInterval = data.getIntExtra("mRefInterval",0);


                }
                Log.d("onActivity","HERE");

                break;

        }
    }


    class RadioOnClick implements DialogInterface.OnClickListener{
        private int index;

        public RadioOnClick(int index){
            this.index = index;
        }
        public void setIndex(int index){
            this.index=index;
        }
        public int getIndex(){
            return index;
        }

        public void onClick(DialogInterface dialog, int whichButton){
            Activity activity = getActivity();
            setIndex(whichButton);
            cameraId = cameraList[index];

            closeCamera();
            stopBackgroundThread();
            startBackgroundThread();
            try {
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            Toast.makeText(activity, "您已经选择了： " + cameraDescribe[index], Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Gets whether you should show UI with rationale for requesting permissions.
     *
     * @param permissions The permissions your app wants to request.
     * @return Whether you can show permission rationale UI.
     */
    private boolean shouldShowRequestPermissionRationale(String[] permissions) {
        Fragment parent = getParentFragment();
        if (parent==null) {
            Log.d("parenttest","null");
        }
        for (String permission : permissions) {
            if (parent.shouldShowRequestPermissionRationale(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requests permissions needed for recording video.
     */
    private void requestVideoPermissions() {

        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);

        } else {
            Fragment parent = getParentFragment();
            parent.requestPermissions( VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        ErrorDialog.newInstance(getString(R.string.permission_request))
                                .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                        break;
                    }
                }
            } else {
                ErrorDialog.newInstance(getString(R.string.permission_request))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(getActivity(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    @SuppressWarnings("MissingPermission")

    String[] cameraList = {"0","1","2"};;
    String[] cameraDescribe = {"Back Cam 1","Front Cam", "Back Cam 2"};
    private void parameter_init() throws CameraAccessException {
        Range<Integer>[] mVideoFps;
        // Choose the sizes for camera preview and video recording
        final Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap map = characteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        //mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));

        Log.d("videosize",mVideoSize.getWidth()+" "+mVideoSize.getHeight());


    }

    private void openCamera(int width, int height) throws CameraAccessException {
        /*
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {

            requestVideoPermissions();

            return;
        }
         */
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.d(TAG, "tryAcquire");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }

            Size[] resolution_range_high_speed = map.getHighSpeedVideoSizes();
            for (Size size:resolution_range_high_speed){
                Log.d("resolution_high",size.getWidth()+" "+size.getHeight());
            }



            //mVideoFps = map.getHighSpeedVideoFpsRangesFor(mVideoSize);
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, mVideoSize);
            mPreviewSize = new Size(mVideoSize.getWidth(),mVideoSize.getHeight());
            //availableFpsRange = map.getHighSpeedVideoFpsRangesFor(mVideoSize);
            //for (Range range : availableFpsRange) {
            //    Log.d("availableFpsRange",range.getLower()+" "+range.getUpper());
            // }
            float[] maperture = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
            for (float aperture:maperture) {
                Log.d("aperture",String.valueOf(aperture));
            }
            float[] mfilter_densities = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FILTER_DENSITIES);
            for (float filter_densities:mfilter_densities) {
                Log.d("filter_densities",String.valueOf(filter_densities));
            }
            float[] mfocal_length = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            for (float focal_length:mfocal_length) {
                Log.d("focal_length",String.valueOf(focal_length));
            }
            float mfocal_distance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
            Log.d("focal_distance",String.valueOf(mfocal_distance));
            float mfocus_distance_calibration = characteristics.get(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION);
            Log.d("focus_calibration",String.valueOf(mfocus_distance_calibration));



            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();
            manager.openCamera(cameraId, mStateCallback, null);

        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }catch (SecurityException e){
            Log.d("SecurityException","e");
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private int OOK_pack_length = (int) Math.floor(1/frame_rate/(1/symbol_rate/1000)/3);
    private int net_pack_length = OOK_pack_length-16;
    private int signal_indicator = 0;
    private int code_indicator = -1;
    private int net_pack_length_t = (int) Math.floor((OOK_pack_length-20)/4)*3;
    private int pack_to = 5;
    private int int_length = (int) Math.floor((net_pack_length_t)*pack_to/8);
    private int codeword_num = 1;//(int) Math.ceil( int_length/255 );
    private int codeword_length = (int) Math.floor( int_length/codeword_num);
    private int symbol_length = (codeword_length - (int) Math.round((double)codeword_length*0.93))%2==0? (int) Math.round((double)codeword_length*0.93):(int) Math.round((double)codeword_length*0.93)-1;
    private int twoS = codeword_length-symbol_length;
    private int net_pack_num_bit = -1;
    private int total_symbol = -1;
    private StringBuffer ss= new StringBuffer(" ");
    private boolean loading = true;
    private boolean done = false;
    private int packet_num_current = 0;
    private int ttttttt = 0;
    private long startTime_total = System.currentTimeMillis();
    private float[] mat_mean = new float[mVideoSize.getHeight()];
    private int mat_step = 30;
    private int handler_finished = 0;
    private int scaling_done = 0;
    private float[] equal = {-0.0344262282406877f,-0.0242516605645861f,0.0781810073375937f,-0.107613566887167f,0.0360030615282260f,-0.0949134563267548f,0.344318916743703f,0.939099313553164f,-0.0233891837299242f,-0.197346346038834f};
    private int kk=0;
    private int total_pack_num = -1;
    private Map<Integer,byte[]> signal_frame_head_map = new ConcurrentHashMap<>();
    private Map<Integer,byte[]> decoded_map = new ConcurrentHashMap<>();
    private Map<Integer,byte[]> code_map = new ConcurrentHashMap<>();
    private Map<Integer,byte[]> received_map = new ConcurrentHashMap<>();
    private Map<Integer,Integer> arrlist = new ConcurrentHashMap<>();

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image mImage = reader.acquireLatestImage();

            if(ttttttt<100){
                ttttttt = ttttttt+1;
            }else if(ttttttt==100){
                ttttttt = 101;
                myHandler.sendEmptyMessage(0x124);
            }
            if(mIsprocessing==1 && !(mImage ==null)) {

                int width = mImage.getWidth();
                int height = mImage.getHeight();
                Image.Plane mPlane = mImage.getPlanes()[0];
                ByteBuffer buffer = mPlane.getBuffer();
                byte[] bytes = new byte[width * height];
                buffer.get(bytes, 0, width * height);


                mImage.close();
                if (scaling_done==0 && handler_finished==1) {
                    handler_finished = 0;
                    scaling_done=1;
                    long startTime = System.currentTimeMillis();
                    for(int i = 0;i<height;i++){
                        mat_mean[i] = 0;
                        for(int j=0;j<width;j = j+mat_step){
                            mat_mean[i] = mat_mean[i]+(bytes[(i*width)+j]&0xFF)/(width/mat_step+1);
                        }
                        //Log.d("mat_mean",String.valueOf(i)+" "+mat_mean[i]);
                    }

                    long endTime = System.currentTimeMillis();
                    expMode = MANUAL_EXPOSURE;
                    mControlMode = CONTROL_MODE_OFF;
                    mAEmodeON = CONTROL_AE_MODE_OFF;
                    mExposure = mExposure_video;
                    mSensitivity = mSensitivity_video;
                    Log.d("expMode",String.valueOf(endTime-startTime));
                    Log.d("expMode", "image available callback");
                    myHandler.sendEmptyMessage(0x123);
                    myHandler.sendEmptyMessageDelayed(0x126, 33);
                }
                if(scaling_done == 1){
                    cachedThreadPool.execute(new MyThread2(mat_mean, bytes, equal, kk, height,symbol_rate,sampling_ratio));
                    kk = kk+1;
                    if(kk%(refInterval*frame_rate)==0){
                        myHandler.sendEmptyMessage(0x124);
                        ttttttt = 0;
                    }


                    Set<Integer> keys = signal_frame_head_map.keySet();
                    Iterator<Integer> iterator1 = keys.iterator();
                    ArrayList<Integer> signal_frame_head_delete = new ArrayList<>();
                    ArrayList<Integer> decoded_delete = new ArrayList<>();
                    boolean search = true;
                    while(search) {
                        while (iterator1.hasNext()) {
                            int kk_temp = iterator1.next();
                            Log.d("hashmap_key", String.valueOf(kk) + ": " + String.valueOf(kk_temp));

                            if (decoded_map.containsKey(kk_temp + 1)) {
                                byte[] value_temp = signal_frame_head_map.get(kk_temp);
                                if (value_temp.length==1) {
                                    byte[] decode_input = decoded_map.get(kk_temp + 1);
                                    decoded_delete.add(kk_temp + 1);
                                    signal_frame_head_delete.add(kk_temp);
                                    packet_reconstruction(decode_input, null, OOK_pack_length, net_pack_length, 0, 0, 0, kk_temp + 1);
                                } else {
                                    byte[] decode_input = decoded_map.get(kk_temp + 1);
                                    byte[] signal_frame_head = signal_frame_head_map.get(kk_temp);
                                    int length_0 = (signal_frame_head[0] & 0xFF);
                                    int length_1 = (signal_frame_head[1] & 0xFF);
                                    int signal_frame_head_length =  length_0*256+length_1;
                                    int header_index = (int) (signal_frame_head[2] & 0xFF);
                                    int frame_index = (int) (signal_frame_head[3] & 0xFF);
                                    byte[] signal_frame_head_input = new byte[OOK_pack_length];
                                    decoded_delete.add(kk_temp + 1);
                                    signal_frame_head_delete.add(kk_temp);
                                    System.arraycopy(signal_frame_head, 4, signal_frame_head_input, 0, signal_frame_head_length);
                                    packet_reconstruction(decode_input, signal_frame_head_input, OOK_pack_length, net_pack_length, signal_frame_head_length, header_index, frame_index, kk_temp + 1);

                                }
                            }
                        }
                        if(decoded_delete.size()==0){
                            search=false;
                        }else{
                            for(int i=0;i<decoded_delete.size();i++){
                                decoded_map.remove(decoded_delete.get(i));
                            }
                            for(int i=0;i<signal_frame_head_delete.size();i++){
                                signal_frame_head_map.remove(signal_frame_head_delete.get(i));
                            }
                            keys = signal_frame_head_map.keySet();
                            iterator1 = keys.iterator();
                            signal_frame_head_delete = new ArrayList<>();
                            decoded_delete = new ArrayList<>();
                        }
                    }
                    keys = decoded_map.keySet();
                    iterator1 = keys.iterator();
                    decoded_delete = new ArrayList<>();
                    while (iterator1.hasNext()) {
                        int kk_temp = iterator1.next();
                        if (kk - kk_temp > 10 && !decoded_map.containsKey(kk_temp - 1) && !signal_frame_head_map.containsKey(kk_temp - 1)) {
                            byte[] decode_input = decoded_map.get(kk_temp);
                            decoded_delete.add(kk_temp);
                            packet_reconstruction(decode_input, null, OOK_pack_length, net_pack_length, 0, 0, 0, kk_temp + 1);

                        }
                    }
                    for(int i=0;i<decoded_delete.size();i++){
                        decoded_map.remove(decoded_delete.get(i));
                    }

                    if(pre_rs==0) {
                        keys = decoded_map.keySet();
                        iterator1 = keys.iterator();
                        decoded_delete = new ArrayList<>();
                        while (iterator1.hasNext()) {
                            int kk_temp = iterator1.next();
                            if (kk - kk_temp > 3) {
                                byte[] decode_input = decoded_map.get(kk_temp);
                                decoded_delete.add(kk_temp);
                                packet_reconstruction(decode_input, null, OOK_pack_length, net_pack_length, 0, 0, 0, kk_temp);
                            }
                        }
                        for (int i = 0; i < decoded_delete.size(); i++) {
                            decoded_map.remove(decoded_delete.get(i));
                        }
                        keys = code_map.keySet();
                        iterator1 = keys.iterator();
                        decoded_delete = new ArrayList<>();
                        search = true;
                        byte[] decoded = new byte[pack_to * OOK_pack_length];
                        int seg = 0;
                        while (search) {
                            while (iterator1.hasNext()) {

                                int kk_temp = iterator1.next();
                                int a = 1;
                                if ((kk_temp + 1) % pack_to == 0) {
                                    for (int i = 1; i < pack_to; i++) {
                                        if (!code_map.containsKey(kk_temp - i)) {
                                            a = 0;
                                        }
                                    }

                                    if (a == 1) {
                                        for (int i = 0; i < pack_to; i++) {
                                            byte[] value_temp = code_map.get(kk_temp - ((pack_to - 1) - i));
                                            System.arraycopy(value_temp, 0, decoded, i * OOK_pack_length, OOK_pack_length);
                                            decoded_delete.add(kk_temp - ((pack_to - 1) - i));
                                        }
                                        seg = (kk_temp + 1) / pack_to;
                                        String fileName = "decode_" + String.valueOf((kk_temp + 1) / pack_to) + ".csv";
                                        save_decode(decoded, fileName, seg);
                                    }
                                }
                                a = 1;
                                if ((kk_temp+1) == net_pack_num_bit && (net_pack_num_bit % pack_to != 0)) {
                                    int temp = net_pack_num_bit - (int) Math.floor(net_pack_num_bit / pack_to) * pack_to;
                                    for (int i = 1; i < temp; i++) {
                                        if (!code_map.containsKey(kk_temp - i)) {
                                            a = 0;
                                        }
                                    }

                                    if (a == 1) {
                                        for (int i = 0; i < temp; i++) {

                                            byte[] value_temp = code_map.get(kk_temp - ((temp - 1) - i));
                                            System.arraycopy(value_temp, 0, decoded, i * OOK_pack_length, OOK_pack_length);
                                            decoded_delete.add(kk_temp - ((temp - 1) - i));
                                            if (kk_temp - ((temp - 1) - i) == 0) {
                                                seg = 1;
                                            }
                                        }
                                        byte[] comp = new byte[OOK_pack_length * (pack_to - temp)];
                                        System.arraycopy(comp, 0, decoded, temp * OOK_pack_length, (pack_to - temp) * OOK_pack_length);
                                        String fileName = "decode_" + String.valueOf(Math.ceil(kk_temp + 1) / pack_to) + ".csv";
                                        seg = (int)Math.ceil(((double)kk_temp + 1.0) / (double)pack_to);
                                        save_decode(decoded, fileName, seg);
                                    }
                                }
                            }
                            if(decoded_delete.size()==0){
                                search=false;
                            }else{
                                for(int i=0;i<decoded_delete.size();i++){
                                    code_map.remove(decoded_delete.get(i));
                                }
                                keys = signal_frame_head_map.keySet();
                                iterator1 = keys.iterator();
                                signal_frame_head_delete = new ArrayList<>();
                                decoded_delete = new ArrayList<>();
                            }
                        }
                        int packet_num = 0;

                        if(net_pack_num_bit!=-1){
                            int a=1;
                            seg = (int)Math.ceil(((double)net_pack_num_bit) / (double)pack_to);
                            for(int i=1;i<seg+1;i++){
                                if(!received_map.containsKey(i)){
                                    a=0;
                                }
                            }

                            StringBuffer stringBuffer = new StringBuffer();
                            int LL = total_symbol;
                            for(int i=1;i<Math.ceil((double)net_pack_num_bit/(double)pack_to)+1;i++){
                                if(received_map.containsKey(i)) {
                                    byte[] value_temp = received_map.get(i);
                                    /*
                                    for(int ii=0;ii<value_temp.length;ii++){
                                        Log.d("byte",i+"  "+ii+"  "+(value_temp[ii]&0xFF));
                                    }

                                     */
                                    try {
                                        String ttt = new String(value_temp, "UTF-8");
                                        stringBuffer.append(ttt);
                                        if(i==1){

                                            LL = value_temp[1]&0xFF;
                                            packet_num = value_temp[2]&0xFF;// packet number
                                            Log.d("bytebyte",(value_temp[0]&0xFF)+" "+LL+" "+packet_num+" "+total_pack_num);

                                        }

                                        /*
                                        if(!arrlist.contains(i)){
                                            mTextView.setText(ttt);
                                            Log.d("codecode", ttt);
                                            arrlist.add(i);
                                        }

                                         */
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }else{
                                    for(int j= 0;j<symbol_length;j++)
                                    {
                                        stringBuffer.append(" ");//这里是空格
                                    }
                                }
                            }
                            /*
                            int lastIndex = 0;
                            while(lastIndex!=-1){
                                lastIndex = stringBuffer.lastIndexOf("%");
                                if(lastIndex!=-1){
                                    stringBuffer.deleteCharAt(lastIndex);
                                    stringBuffer.insert(lastIndex,"\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
                                    LL = LL+15;
                                }

                            }

                             */
                            String str = stringBuffer.toString();
                            Log.d("string",str);
                            loading = false;
                            String ss_temp = str.substring(4,4+(LL));//LL-1


                            if(a==1) {
                                if(!arrlist.containsKey(packet_num)) {

                                    int insert_index = 0;
                                    keys = arrlist.keySet();
                                    iterator1 = keys.iterator();
                                    while (iterator1.hasNext()) {
                                        int kk_temp = iterator1.next();
                                        if(packet_num>kk_temp){
                                            insert_index = insert_index + arrlist.get(kk_temp);
                                        }
                                    }

                                    Log.d("total_symbol",ss_temp);
                                    try{
                                        ss.insert(insert_index,ss_temp);
                                        arrlist.put(packet_num,LL);//LL-1

                                        long endTime = System.currentTimeMillis(); //结束时间
                                        long runTime = endTime - startTime_total;
                                        remove_hash();
                                        packet_num_current = packet_num;
                                        Log.d("packet_num",String.valueOf(packet_num));
                                    }catch(Exception e){
                                        Log.d("outofindex","here");

                                    }


                                }

                                if(total_pack_num!=-1){
                                    done = true;
                                    for(int i=1;i<total_pack_num+1;i++){
                                        if(!arrlist.containsKey(i))
                                        {
                                            done = false;
                                        }
                                    }
                                }

                            }
                            if(done){
                                //mText.setText("Click THIS Button");
                                //Toast.makeText(getActivity(), "Done.", Toast.LENGTH_SHORT).show();
                                myHandler.sendEmptyMessage(0x125);
                                Log.d("done",total_pack_num+" "+ss.toString());
                                done = false;
                            }else{
                                String ss_view = ss_temp;
                                //mTextView.setText(ss_view);
                            }
                        }

                    }
                    //if(arrlist.containsKey(1)&&arrlist.containsKey(2)&&arrlist.containsKey(3)&&arrlist.containsKey(4)){
                    //    mScrollView.scrollBy(0,2);
                    //}

                }



            }
            if(mImage!=null){
                mImage.close();
            }
        }

    };
    final Handler myHandler = new Handler() {

        @Override
        //重写handleMessage方法,根据msg中what的值判断是否执行后续操作
        public void handleMessage(Message msg) {
            if(msg.what == 0x123)
            {
                Log.d("expMode","camera start again");
                closeCamera();
                stopBackgroundThread();
                startBackgroundThread();
                try {
                    openCamera(mTextureView.getWidth(), mTextureView.getHeight());
                    startTime_total = System.currentTimeMillis();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                handler_finished = 1;
                Log.d("expMode","Finished");
            }
            if (msg.what == 0x124) {
                mProcessing.performClick();

            }
            if (msg.what == 0x125) {
                mText.performClick();

            }
        }
    };



    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            mImageReader = ImageReader.newInstance(mVideoSize.getWidth(), mVideoSize.getHeight(),
                    ImageFormat.YUV_420_888, /*maxImages*/7);
            mImageReader.setOnImageAvailableListener(
                    mOnImageAvailableListener, mBackgroundHandler);

            mPreviewBuilder.addTarget(previewSurface);
            mPreviewBuilder.addTarget(mImageReader.getSurface());

            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, mControlMode);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, mAWBmode);
            mPreviewBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, mEffectMode);
            mPreviewBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, mSceneMode);
            Log.d("expMode",String.valueOf(expMode));

            if(expMode==MANUAL_EXPOSURE) {

                if (mAEmodeON == CONTROL_AE_MODE_ON) {
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_ON);
                    if (mFrameRate != null) {
                        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, mFrameRate);
                    } else {
                        mFrameRate = mPreviewBuilder.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
                    }
                } else {
                    //mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE,CONTROL_AWB_MODE_OFF);
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF);
                    mPreviewBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mExposure);
                    mPreviewBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, mSensitivity);
                    mFrameRate = mPreviewBuilder.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
                    mPreviewBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, 1 / Long.valueOf(mFrameRate.getUpper()) * 1000000000);
                    //long frame_rate = 20;
                    //mPreviewBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, 1 / frame_rate * 1000000000);
                }
                if (mAFmodeON == CONTROL_AF_MODE_OFF) {
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CONTROL_AF_MODE_OFF);
                    mPreviewBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, mFocalDistance);
                } else {
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, mAFmodeON);
                }
            }else{
                mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CONTROL_MODE_AUTO);
                if (mAEmodeON == CONTROL_AE_MODE_ON) {
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_ON);
                    if (mFrameRate != null) {
                        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, mFrameRate);
                    } else {
                        mFrameRate = mPreviewBuilder.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
                    }
                }
            }


            mCameraDevice.createCaptureSession(Arrays.asList( previewSurface,mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            Log.d("startpreview thread:",Long.toString(Thread.currentThread().getId()));
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Activity activity = getActivity();
                            if (null != activity) {
                                Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            Log.e("rotation",String.valueOf(rotation));
            Log.d("expMode",String.valueOf(expMode));
            if(mHighSpeed==false) {
                if (mIsRecordingVideo) {
                    mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                } else {
                    mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                }
            }else{
                if (mIsRecordingVideo) {
                    setUpCaptureRequestBuilder(mPreviewBuilder);
                    List<CaptureRequest> mPreviewBuilderBurst = mPreviewSessionHighSpeed
                            .createHighSpeedRequestList(mPreviewBuilder.build());
                    mPreviewSessionHighSpeed.setRepeatingBurst(mPreviewBuilderBurst, null, mBackgroundHandler);
                } else {
                    mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                }
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d("CameraAccessException","ERROR");
        }
    }

    CameraConstrainedHighSpeedCaptureSession mPreviewSessionHighSpeed;
    private Range<Integer> getHighestFpsRange(Range<Integer>[] fpsRanges) {
        Range<Integer> fpsRange = Range.create(fpsRanges[0].getLower(), fpsRanges[0].getUpper());
        Log.d("fpsRange",fpsRange.getLower()+" "+fpsRange.getUpper());
        for (Range<Integer> r : fpsRanges) {
            if (r.getUpper() > fpsRange.getUpper()) {
                fpsRange.extend(0, r.getUpper());
                Log.d("fpsRange",fpsRange.getLower()+" "+fpsRange.getUpper());
            }
        }
        for (Range<Integer> r : fpsRanges) {
            if (r.getUpper() == fpsRange.getUpper()) {
                if (r.getLower() < fpsRange.getLower()) {
                    fpsRange.extend(r.getLower(), fpsRange.getUpper());
                    Log.d("fpsRange",fpsRange.getLower()+" "+fpsRange.getUpper());
                }
            }
        }
        return fpsRange;
    }


    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
//        Range<Integer> fpsRange = Range.create(240, 240);
        Range<Integer> fpsRange = getHighestFpsRange(availableFpsRange);
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);

    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }


    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM);
        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getVideoFilePath(getActivity());
        }
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        //mMediaRecorder.setCaptureRate(60);
        mMediaRecorder.setVideoFrameRate(mFrameRate.getUpper());
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.VP8);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }

        mMediaRecorder.prepare();
    }


    private String getVideoFilePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        /*
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + System.currentTimeMillis() + ".mp4";

         */
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + System.currentTimeMillis() + ".mp4";
    }

    private void startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);//high speed video recording bixujia！！
            //mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_OFF);
            List<Surface> surfaces = new ArrayList<>();
            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);
            mButtonVideo.setText(R.string.stop);
            mIsRecordingVideo = true;
            if(mHighSpeed==false) {


                mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, mControlMode);
                mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE, mAWBmode);
                mPreviewBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, mEffectMode);
                mPreviewBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, mSceneMode);

                if (mAEmodeON == CONTROL_AE_MODE_ON){
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_ON);
                    if (mFrameRate!=null) {
                        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, mFrameRate);
                    }else{
                        mFrameRate = mPreviewBuilder.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
                    }
                }  else{
                    //mPreviewBuilder.set(CaptureRequest.CONTROL_AWB_MODE,CONTROL_AWB_MODE_OFF);
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF);
                    mPreviewBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mExposure);
                    mPreviewBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, mSensitivity);
                    mPreviewBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION,1/Long.valueOf(mFrameRate.getUpper())*1000000000);
                }
                if(mAFmodeON == CONTROL_AF_MODE_OFF) {
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CONTROL_AF_MODE_OFF);
                    mPreviewBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE,mFocalDistance);
                }else{
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,mAFmodeON);
                }




                // Start a capture session
                // Once the session starts, we can update the UI and start recording

                mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        mPreviewSession = cameraCaptureSession;
                        //mPreviewSessionHighSpeed = (CameraConstrainedHighSpeedCaptureSession) mPreviewSession;
                        updatePreview();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // UI
                                mButtonVideo.setText(R.string.stop);
                                mIsRecordingVideo = true;

                                // Start recording
                                Log.d("mediarecorder start", "HERE");
                                mMediaRecorder.start();
                                Log.d("mediarecorder end", "HERE");
                            }


                        });
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Activity activity = getActivity();
                        if (null != activity) {
                            Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, mBackgroundHandler);
            }else{
                final Activity activity = getActivity();
                if (null == activity || activity.isFinishing()) {
                    return;
                }

                CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics
                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);


                availableFpsRange = map.getHighSpeedVideoFpsRangesFor(mVideoSize);
                for (Range range : availableFpsRange) {
                    Log.d("availableFpsRange",range.getLower()+" "+range.getUpper());
                }

                Range<Integer> fpsRange = getHighestFpsRange(availableFpsRange);
                Log.d("HighestFpsRange",fpsRange.getLower()+" "+fpsRange.getUpper());
                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);

                mCameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        mPreviewSession = cameraCaptureSession;
                        mPreviewSessionHighSpeed = (CameraConstrainedHighSpeedCaptureSession) mPreviewSession;
                        updatePreview();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // UI
                                mButtonVideo.setText(R.string.stop);
                                mIsRecordingVideo = true;

                                // Start recording
                                Log.d("mediarecorder start","HERE");
                                mMediaRecorder.start();
                                Log.d("mediarecorder end","HERE");
                            }


                        });
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Activity activity = getActivity();
                        if (null != activity) {
                            Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, mBackgroundHandler);
            }

        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
            Log.d("capturesession:","CameraAccessException");
        }
        catch (IllegalArgumentException e) {
            Log.d("capturesession:","IllegalArgumentException");
        }
        catch (IllegalStateException e) {
            Log.d("capturesession:","IllegalStateException");
        }

    }


    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }


    private void stopRecordingVideo() {
        // UI
        mExposure = mExposure_image;
        mSensitivity = mSensitivity_image;
        mIsRecordingVideo = false;
        mButtonVideo.setText(R.string.record);
        /*
        // Stop recording
        try {
            mPreviewSessionHighSpeed.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

         */

        mMediaRecorder.stop();
        Log.d("stop","HERE");
        mMediaRecorder.reset();

        Activity activity = getActivity();
        if (null != activity) {
            Toast.makeText(activity, "Video saved: " + mNextVideoAbsolutePath,
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Video saved: " + mNextVideoAbsolutePath);
        }
        mNextVideoAbsolutePath = null;
        startPreview();
    }


    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.permission_request)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions( VIDEO_PERMISSIONS,
                                    REQUEST_VIDEO_PERMISSIONS);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    parent.getActivity().finish();
                                }
                            })
                    .create();
        }

    }

    ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    class MyThread2 implements Runnable{
        private float[] mMat_mean;// current image
        private byte[] mBytes;// initial image
        private float[] mEqual;
        private int mkk;
        private int mHeight;
        private float msymbol_rate;
        private float msamplint_ratio;
        MyThread2(float[] mat_mean,byte[] bytes,float[] equal,int kk,int height,float symbol_rate,float sampling_ratio)
        {
            mMat_mean = mat_mean;
            mBytes=bytes;
            mEqual = equal;
            mkk = kk;
            mHeight = height;
            msymbol_rate = symbol_rate;
            msamplint_ratio = sampling_ratio;
        }
        public void run()
        {
            long startTime = System.currentTimeMillis();
            float[] Yi = scaling(mMat_mean,mBytes);
            int offset = timing_recovery(Yi,Yi.length,0);
            float[] x = equalization(Yi,Yi.length,offset,mEqual);

            Log.d("test","finished");

            double sampling_ratio = msamplint_ratio;
            double ratio = 9/sampling_ratio*msymbol_rate/20;
            int Yi_size = (int) Math.floor(mHeight*ratio-ratio+1);
            int Yi_temp_size = (int) Math.floor((Math.floor((( (double) Yi_size - 1) - ( (double) 17 -4 + 2.25)) / 4.5) + 1) / 2) * 2;
            int decoded_size = (int) Math.floor((float) Yi_temp_size/2.0-4.0);
            float[] decoded = new float[decoded_size];

            if(text_or_file==1){
                //regular
                for(int i=0;i<9;i++){
                    //mHomo_pre[i] = x[i];
                }
                if(!(x[0]==-1)) {
                    for (int i = 0; i < decoded_size; i++) {
                        decoded[i] = (byte)x[i];
                    }
                    for (int i = decoded_size; i < decoded_size + mEqual.length; i++) {
                        mEqual[i - decoded_size] = x[i];
                    }
                    //save_decode(decoded_save,String.valueOf(mkk)+".csv",0);
                }else{
                    Log.d("equal",mkk+"cannot update");
                }
            }
            //int seg = 0;
            //save_decode(output,String.valueOf(mkk)+".csv",seg);


            Bundle bundle = new Bundle();
            //bundle.putFloatArray("homo_pre", mHomo_pre);
            bundle.putFloatArray("decoded", decoded);
            bundle.putFloatArray("equal", mEqual);
            bundle.putInt("kk",mkk);
            Message message = new Message();
            message.setData(bundle);

            mHandlerThread.sendMessage(message);
            //Log.e("sub","---------> msg.what = " );

            Thread t = Thread.currentThread();
            long l = t.getId();
            String name = t.getName();
            long p = t.getPriority();
            String gname = t.getThreadGroup().getName();
            Log.d("thread",name
                    + ":(id)" + l
                    + ":(priority)" + p
                    + ":(group)" + gname);
            //Log.d("shift",shift.toString());
            long endTime = System.currentTimeMillis(); //结束时间
            long runTime = endTime - startTime;
            Log.d("test_data_processing",mkk+"    "+String.valueOf(runTime));
        }
    }
    Handler mHandlerThread = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(scaling_done == 1){
                Bundle bundle = msg.getData();
                //homo_pre = bundle.getFloatArray("homo_pre");
                equal = bundle.getFloatArray("equal");
                float[] decoded = bundle.getFloatArray("decoded");
                if(!(decoded==null)) {
                    byte[] decoded_byte = new byte[decoded.length];
                    for (int i = 0; i < decoded.length; i++) {
                        decoded_byte[i] = (byte) decoded[i];
                    }
                    int kk = bundle.getInt("kk");
                    decoded_map.put(kk, decoded_byte);
                }
            }


            Log.e("sub","handler received");
        }
    };



    private float[] scaling(float[] mat_mean,byte[] bytes){
        float[] img_mean = new float[mVideoSize.getHeight()];
        float mean_value = 0;
        float std_value = 0;
        for(int i = 0;i<mVideoSize.getHeight();i++){
            img_mean[i] = 0;
            for(int j=0;j<mVideoSize.getWidth();j = j+mat_step){
                img_mean[i] = img_mean[i]+(bytes[(i*mVideoSize.getWidth())+j]&0xFF)/(mVideoSize.getWidth()/mat_step+1);
            }
            img_mean[i] = img_mean[i]/mat_mean[i];

            mean_value = mean_value+img_mean[i]/mVideoSize.getHeight();
        }

        for(int i = 0;i<mVideoSize.getHeight();i++){
            std_value = std_value+(float) Math.pow(img_mean[i]-mean_value,2);
            if(kk==10) {
                Log.d("img_mean", String.valueOf(i) + " "+img_mean[i]);
            }
        }
        std_value = (float) Math.sqrt(std_value/mVideoSize.getHeight());
        for(int i = 0;i<mVideoSize.getHeight();i++){
            img_mean[i] = (img_mean[i]-mean_value)/std_value;
            if(img_mean[i]>2){
                img_mean[i] = 2;
            }else if (img_mean[i]<-2){
                img_mean[i] = -2;
            }
        }
        float ratio = (float) (9.0 / sampling_ratio * symbol_rate / 20.0);
        int Yi_size = (int) ((float)mVideoSize.getHeight() * ratio - ratio + 1.0);
        float[] Yi = new float[Yi_size];
        //Log.d("scaling",String.valueOf(Yi_size)+" "+String.valueOf(mVideoSize.getHeight())+" "+String.valueOf(ratio));

        // interpolation
        int xi = 0;
        for (int i = 0; i < mVideoSize.getHeight(); i++) {
            while (xi < (float)i * ratio && xi<Yi_size-1) {
                Yi[xi] = (float) (img_mean[i - 1] + (img_mean[i] - img_mean[i - 1]) / ratio * ((float) xi - ((float) i - 1.0) * ratio));
                xi = xi + 1;
                if(Yi[xi]>2){
                    Yi[xi] = 2;
                }else if(Yi[xi] < -2){
                    Yi[xi] = -2;
                }
            }
        }
        return Yi;
    }
    private int timing_recovery(float[] Yi, int Yi_size, float threshold) {
        int[][] length_11 = new int[3][Yi_size];
        int[][] length_00 = new int[3][Yi_size];
        int indicator;
        int length = 1;
        int start_point = 0;
        int index_00 = 0;
        int index_11 = 0;
        int offset = -1;
        int[] col_temp = new int[Yi_size];
        float mean_Yi = threshold;

        if (Yi[0] < mean_Yi) {
            col_temp[0] = 0;
            indicator = 0;
        } else {
            col_temp[0] = 1;
            indicator = 1;
        }

        for (int j = 1; j < Yi_size; j++) {
            if (Yi[j] < mean_Yi) {
                col_temp[j] = 0;
                if (indicator == 0) {
                    length++;
                } else {
                    length_11[0][index_11] = length;
                    length_11[1][index_11] = start_point;
                    indicator = 0;
                    length = 1;
                    start_point = j;
                    index_11++;
                }
            } else {
                col_temp[j] = 1;
                if (indicator == 1) {
                    length++;
                } else {
                    length_00[0][index_00] = length;
                    length_00[1][index_00] = start_point;
                    indicator = 1;
                    length = 1;
                    start_point = j;
                    index_00++;
                }
            }
        }

        if (indicator == 0) {
            length_00[0][index_00] = length;
            length_00[1][index_00] = start_point;
            index_00++;
        } else {
            length_11[0][index_11] = length;
            length_11[1][index_11] = start_point;
            index_11++;
        }

        length = 0;
        for (int j = 0; j < index_00; j++) {
            length_00[2][j] = (int) ((float) length_00[0][j] / 9.0 + 0.5);
            length = length + length_00[2][j];
        }
        for (int j = 0; j < index_11; j++) {
            length_11[2][j] = (int) ((float) length_11[0][j] / 9.0 + 0.5);
            length = length + length_11[2][j];
        }


        int[] signal = new int[length];
        int[] signal_start_point = new int[length];
        int ii = 0;
        int ii_00 = 0;
        int ii_11 = 0;
        if (length_00[1][0] < length_11[1][0]) {
            indicator = 1;
            for (int j = ii; j < ii + length_00[2][ii_00]; j++) {
                signal[j] = 0;
                signal_start_point[j] = length_00[1][ii_00];
            }
            ii = ii + length_00[2][ii_00];
            ii_00++;
        }
        if (length_11[1][0] < length_00[1][0]) {
            indicator = 0;
            for (int j = ii; j < ii + length_11[2][ii_11]; j++) {
                signal[j] = 1;
                signal_start_point[j] = length_11[1][ii_11];
            }
            ii = ii + length_11[2][ii_11];
            ii_11++;
        }
        while (ii < length) {
            if (indicator == 1) {
                indicator = 0;
                for (int j = ii; j < ii + length_11[2][ii_11]; j++) {
                    signal[j] = 1;
                    signal_start_point[j] = length_11[1][ii_11];
                }

                ii = ii + length_11[2][ii_11];
                ii_11++;
                if (ii_11 > index_11 || ii_11 == index_11) {
                    break;
                }

            } else if (indicator == 0) {
                indicator = 1;
                for (int j = ii; j < ii + length_00[2][ii_00]; j++) {
                    signal[j] = 0;
                    signal_start_point[j] = length_00[1][ii_00];
                }
                ii = ii + length_00[2][ii_00];
                ii_00++;
                if (ii_00 > (index_00 - 1) || ii_00 == (index_00 - 1)) {
                    break;
                }
            }
        }
        int header_1[] = {1, 1, 1};
        int index_header1[] = {0, 0};
        int index_header1_temp[] = {0, 0};
        int sample_point_ref = 0;
        int signal_temp = 0;
        int compare_indicator = 0;
//header 1
        int signal_compare[] = {1, 0, 1, 0, 1, 0, 1, 0};
        for (int j = 0; j < length - 15; j++) {
            index_header1_temp[0] = j;
            index_header1_temp[1] = 0;
            if (signal[j] == header_1[0] && signal[j + 1] == header_1[1] && signal[j + 2] == header_1[2]) {
//Log.d("mmmm",String.valueOf(signal[j+3])+String.valueOf(signal[j+4])+String.valueOf(signal[j+5])+String.valueOf(signal[j+6]));
//Log.d("mmmm",String.valueOf(signal[j+7])+String.valueOf(signal[j+8])+String.valueOf(signal[j+9])+String.valueOf(signal[j+10]));
                int k_init = 0;
                if ((signal_start_point[j + 3] - 9) > 0 || (signal_start_point[j + 3] - 9) == 0) {
                    k_init = -9;
                }
                for (int kk = k_init; kk < 9; kk++) {
                    sample_point_ref = signal_start_point[j + 3] + kk;
                    if (sample_point_ref + 9 * 7 < Yi_size) {
                        compare_indicator = 1;
                        for (int kkk = 0; kkk < 8; kkk++) {
                            if (Yi[sample_point_ref + kkk * 9] < mean_Yi) {
                                signal_temp = 0;
                            } else {
                                signal_temp = 1;
                            }
                            if (!(signal_temp == signal_compare[kkk])) {
                                compare_indicator = 0;
                            }
                        }
                        if (compare_indicator == 1) {
                            index_header1_temp[1]++;
//Log.d("mmmm",String.valueOf(signal[j+3])+String.valueOf(signal[j+4])+String.valueOf(signal[j+5])+String.valueOf(signal[j+6]));
//Log.d("mmmm",String.valueOf(signal[j+7])+String.valueOf(signal[j+8])+String.valueOf(signal[j+9])+String.valueOf(signal[j+10]));
                            continue;
                        }
                    }
                }
            }
            if (index_header1[0] == 0) {
                index_header1[0] = index_header1_temp[0];
                index_header1[1] = index_header1_temp[1];
            } else if (index_header1_temp[1] > index_header1[1]) {
                index_header1[0] = index_header1_temp[0];
                index_header1[1] = index_header1_temp[1];
            }
        }

        if (index_header1[1] == 0) {
            return offset;
        } else {
            float col_sampled = 0;
            float offset_temp[] = {0, 500};

            sample_point_ref = signal_start_point[index_header1[0] + 4];

            for (int j = -9; j < 10; j++) {
                col_sampled = 0;
                for (int kkk = 0; kkk < 6; kkk++) {
                    if ((Yi[sample_point_ref + j + kkk * 9 - 4] - Yi[sample_point_ref + j + kkk * 9 + 4]) > 0) {
                        col_sampled = col_sampled + (Yi[sample_point_ref + j + kkk * 9 - 4] - Yi[sample_point_ref + j + kkk * 9 + 4]);
                    } else {
                        col_sampled = col_sampled - (Yi[sample_point_ref + j + kkk * 9 - 4] - Yi[sample_point_ref + j + kkk * 9 + 4]);
                    }
                }
                if (col_sampled < offset_temp[1]) {
                    offset_temp[0] = j;
                    offset_temp[1] = col_sampled;
                }
            }
            offset = (int) (signal_start_point[index_header1[0] + 4] + offset_temp[0]);
            offset = offset % 9;

            if (offset == 9) {
                offset = 0;
            }
            offset = offset + 9;
            return offset;

        }
    }
    private float[] equalization(float[] Yi, int Yi_size, int offset, float[] equal){
        int index_left = 0;
        int index_right = 0;
        float index_temp = 0;
        int Yi_temp_size = (int) Math.floor((Math.floor((( (double) Yi_size - 1) - ( (double) 17 -4 + 2.25)) / 4.5) + 1) / 2) * 2;
        int decode_size = (int) Math.floor((float) Yi_temp_size/2.0-4.0);
        float[] decode =new float[decode_size];
        float decode_temp = 0;

        int equal_index = 0;
        float step = 0.001f;
        float equal2[] =  {-0.0344262282406877f,-0.0242516605645861f,0.0781810073375937f,-0.107613566887167f,0.0360030615282260f,-0.0949134563267548f,0.344318916743703f,0.939099313553164f,-0.0233891837299242f,-0.197346346038834f};
        if(offset==-1){

            float[] decode_output=new float[decode_size+ 10];
            for(int i=0;i< decode_size;i++){
                decode_output[i] = -1;
            }
            for(int i=decode_size;i<decode_size+ 10;i++){
                decode_output[i] = equal2[i-decode_size];
            }
            return decode_output;
        }
        float[] Yi_temp = new float[Yi_temp_size];
        float mean_Yi=0;

        for (int j = 0; j < Yi_temp_size; j++) {
            index_temp = (float)offset -4.0f + 2.25f + (float)j * 4.5f;
            index_left = (int) (index_temp);
            index_right = (int) (index_temp)+1;
            Yi_temp[j] = Yi[index_left] + (Yi[index_right] - Yi[index_left]) / 1.0f * (index_temp - (float)index_left);
            if(Yi_temp[j]>2){
                Yi_temp[j] = 2;
            }else if(Yi_temp[j]<-2){
                Yi_temp[j]=-2;
            }
        }

        for(int j=0;j< decode_size;j++){
            decode_temp = 0;
            equal_index = 0;
            for(int kkk = j*2;kkk<j*2+10;kkk++ ){
                decode_temp = decode_temp+equal[equal_index]*(Yi_temp[kkk]-mean_Yi);
                equal_index++;
            }
            equal_index = 0;
            if(decode_temp<0){
                decode[j] = 0;
                for(int kkk = j*2;kkk<j*2+10;kkk++ ){
                    equal[equal_index] = equal[equal_index] + step*(-1.0f - decode_temp)*(Yi_temp[kkk]-mean_Yi);
                    equal_index++;
                }

            }else{
                decode[j] = 1;
                for(int kkk = j*2;kkk<j*2+10;kkk++ ){
                    equal[equal_index] = equal[equal_index] + step*(1.0f - decode_temp)*(Yi_temp[kkk]-mean_Yi);
                    equal_index++;
                }

            }
        }
        float[] decode_output = new float[decode_size+ 10];
        for(int i=0;i< decode_size;i++){
            decode_output[i] = decode[i];
        }
        for(int i=decode_size;i<decode_size+ 10;i++){
            decode_output[i] = equal[i-decode_size];
        }
        return decode_output;
    }
    private void packet_reconstruction(byte[] decode_input,byte[] signal_frame_head,int OOK_pack_length,int net_pack_length,
                                       int signal_frame_head_length,int header_index,int frame_index,int kk){
        // packet reconstruction
        long startTime = System.currentTimeMillis();
        Log.d("packet",kk+" "+"signal_header"+header_index);

        int header_length = 16;
        int[] P = new int[10];
        int[] packet_indicator = new int[10];
        int[] header_OOK = {1,1,1,1, 1,0,1,0, 1,0,1,0};
        int indicator_temp = 1;
        int P_length = 0;
        int equal_index;
        int current_index = 0;
        byte[] decode = new byte[OOK_pack_length+8];
        int FRAME = 0;
        int diff = 0;
        int seg = 0;
        String fileName;
        byte[] signal_frame_head_output = new byte[OOK_pack_length+4];
        int mkk = kk;
        if(signal_frame_head==null){
            signal_frame_head = new byte[OOK_pack_length+3];
        }


        for(int j=0;j<decode_input.length-20;j++){
            equal_index = j;
            indicator_temp = 1;

            for(int kkk=0;kkk<12;kkk++){
                if(decode_input[equal_index]!=header_OOK[kkk]){
                    indicator_temp = 0;
                    break;
                }
                equal_index++;
            }
            if(indicator_temp == 1){
                FRAME = (int)decode_input[equal_index]*8+(int)decode_input[equal_index+1]*4+(int)decode_input[equal_index+2]*2+(int)decode_input[equal_index+3];
                switch (FRAME){
                    case 10:
                        P[P_length] = j;
                        packet_indicator[P_length] = 0;
                        P_length++;
                        continue;
                    case 5:
                        P[P_length] = j;
                        packet_indicator[P_length] = 1;
                        P_length++;
                        continue;
                    case 9:
                        P[P_length] = j;
                        packet_indicator[P_length] = 2;
                        P_length++;
                        continue;
                    default:
                        continue;
                }

            }

        }
        int[] P_2 = new int[10];
        int[] packet_indicator_2 = new int[10];
        int P_2_length = 0;
        if(P_length == 1){
            P_2[0] = P[0];
            packet_indicator_2[0] = packet_indicator[0];
            P_2_length = 1;
        }else if (P_length>1) {
            for (int j = 0; j <= P_length-2; j++) {
                for (int kkk = j + 1; kkk <= P_length - 1; kkk++) {
                    if (P[kkk] - P[j] == OOK_pack_length) {
                        if ( (P_2_length>0 && P[j] != P_2[P_2_length - 1]) || P_2_length==0) {
                            P_2[P_2_length] = P[j];
                            packet_indicator_2[P_2_length] = packet_indicator[j];
                            P_2_length++;
                        }
                        if ( (P_2_length>0 && P[kkk] != P_2[P_2_length - 1]) || P_2_length==0) {
                            P_2[P_2_length] = P[kkk];
                            packet_indicator_2[P_2_length] = packet_indicator[kkk];
                            P_2_length++;
                        }

                    }
                }
            }
        }
        if(P_2_length==0){
            Log.d("packet",mkk+"warning: no header is found");
            byte[] put_temp = {0};
            signal_frame_head_map.put(mkk,put_temp);
            return;
        }
        int reuse_index=0;
        int comp_index;
        int indicator_past= signal_indicator;

        //Log.d("packet",mkk+" "+"pos 1"+" "+header_length+" "+signal_frame_head_length);
        if(P_2[0]+1>(net_pack_length) && packet_indicator_2[0]==0){
            int P_temp = P_2[0]-net_pack_length;
            current_index = decode_input[P_temp] * 8 + decode_input[P_temp + 1] * 4 + decode_input[P_temp + 2] * 2 +decode_input[P_temp + 3] * 1;
            switch( current_index){
                case 3:
                    current_index = 0;
                    break;
                case 6:
                    current_index = 1;
                    break;
                case 9:
                    current_index = 2;
                    break;
                case 12:
                    current_index = 3;
                    break;
                case 10:
                    current_index = 5;
                    Log.d("codecode","findfind");
                    break;
                default:
                    current_index = 4;
                    break;
            }


            System.arraycopy(decode_input,P_2[0]-net_pack_length,decode,16,net_pack_length);
            decode[OOK_pack_length+0] = (byte) ((mkk & 0xFF000000) >> 24);
            decode[OOK_pack_length+1] = (byte) ((mkk & 0x00FF0000) >> 16);
            decode[OOK_pack_length+2] = (byte) ((mkk & 0x0000FF00) >> 8);
            decode[OOK_pack_length+3] = (byte) ((mkk & 0x000000FF) >> 0);

            decode[OOK_pack_length+4] = 0;
            decode[OOK_pack_length+5] = 0;
            decode[OOK_pack_length+6] = 0;
            decode[OOK_pack_length+7] = 0;
            signal_frame_head_length = 0;

            fileName = "decode_" + String.valueOf(signal_indicator) + ".csv";
            if (pre_rs==1){
                save_decode(decode,fileName,seg);
            }else{
                if(current_index==5){
                    code_indicator = 0;
                    code_map.put(code_indicator,decode);
                }else{
                    if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator<net_pack_num_bit-1){
                        code_indicator++;
                        code_map.put(code_indicator,decode);
                    }else if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator>=net_pack_num_bit-1){
                        code_indicator=-1;
                    } else if(code_indicator!=-1){
                        code_indicator++;
                        code_map.put(code_indicator,decode);
                    }
                }

            }

            Log.d("codecode",String.valueOf(mkk)+" "+String.valueOf(current_index)+" "+code_indicator);
            signal_indicator++;
        }
        if(signal_frame_head_length!=0) {
            comp_index = P_2[0]  + signal_frame_head_length - OOK_pack_length;
            if (packet_indicator_2[0] == 0 && comp_index < 0) {
                signal_frame_head_length = 0;
            } else if (packet_indicator_2[0] == 2 && comp_index < 0) {
                signal_frame_head_length = 0;
            } else if (packet_indicator_2[0] == 1 && comp_index > 0) {
                //if the header 0 is not detected
                if (P_2[0] > OOK_pack_length) {
                    int P_temp = P_2[0] - OOK_pack_length - 1;//position
                    P_temp = P_temp + 1;//length
                    comp_index = P_temp + signal_frame_head_length - OOK_pack_length;
                    //////////////////////////////////////
                    current_index = decode_input[P_temp + header_length] * 8 + decode_input[P_temp + header_length + 1] * 4 + decode_input[P_temp + header_length + 2] * 2 +decode_input[P_temp + header_length + 3] * 1;
                    switch( current_index){
                        case 3:
                            current_index = 0;
                            break;
                        case 6:
                            current_index = 1;
                            break;
                        case 9:
                            current_index = 2;
                            break;
                        case 12:
                            current_index = 3;
                            break;
                        case 10:
                            current_index = 5;
                            Log.d("codecode","findfind");
                            break;
                        default:
                            current_index = 4;
                            break;
                    }
                    if(current_index!=4 && current_index!=5){
                        current_index = current_index-1;
                        current_index = current_index % 4;
                    }

                    if (comp_index > 0 && ((current_index == header_index) || header_index==5)) {
                        /////////////////////////////////
                        if (comp_index % 2 == 1) {
                            diff = (int) (signal_frame_head_length - Math.floor(comp_index / 2)) - (int) Math.floor((int) (signal_frame_head_length - Math.floor(comp_index / 2))/4)*4;

                            System.arraycopy(signal_frame_head, 0, decode, 0, (int) (signal_frame_head_length - Math.floor(comp_index / 2))-diff  );
                            System.arraycopy(decode_input, (int) Math.floor(comp_index / 2) + 1 - diff, decode, (int) (signal_frame_head_length - Math.floor(comp_index / 2))-diff, (P_temp) - (int) (Math.floor(comp_index / 2) + 1) + diff);
                            decode[OOK_pack_length+0] = (byte) ((mkk & 0xFF000000) >> 24);
                            decode[OOK_pack_length+1] = (byte) ((mkk & 0x00FF0000) >> 16);
                            decode[OOK_pack_length+2] = (byte) ((mkk & 0x0000FF00) >> 8);
                            decode[OOK_pack_length+3] = (byte) ((mkk & 0x000000FF) >> 0);

                            int markhere = (int) (signal_frame_head_length - Math.floor(comp_index / 2))-diff;
                            decode[OOK_pack_length+4] = (byte) ((markhere & 0xFF000000) >> 24);
                            decode[OOK_pack_length+5] = (byte) ((markhere & 0x00FF0000) >> 16);
                            decode[OOK_pack_length+6] = (byte) ((markhere & 0x0000FF00) >> 8);
                            decode[OOK_pack_length+7] = (byte) ((markhere & 0x000000FF) >> 0);

                            fileName = "decode_" + String.valueOf(signal_indicator) + ".csv";

                            if (pre_rs==1){
                                save_decode(decode,fileName,seg);
                            }else{
                                if(header_index==5){
                                    code_indicator = 0;
                                    code_map.put(code_indicator,decode);
                                }else{
                                    if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator<net_pack_num_bit-1){
                                        code_indicator++;
                                        code_map.put(code_indicator,decode);
                                    }else if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator>=net_pack_num_bit-1){
                                        code_indicator=-1;
                                    } else if(code_indicator!=-1){
                                        code_indicator++;
                                        code_map.put(code_indicator,decode);
                                    }
                                }
                            }
                            Log.d("codecode",String.valueOf(mkk)+" "+String.valueOf(header_index)+" "+code_indicator);
                            signal_indicator++;
                        } else {
                            diff = (int) (signal_frame_head_length - comp_index / 2) - (int) Math.floor((int) (signal_frame_head_length - comp_index / 2)/4)*4;

                            System.arraycopy(signal_frame_head, 0, decode, 0, signal_frame_head_length - comp_index / 2 - diff);
                            System.arraycopy(decode_input, comp_index / 2- diff, decode, signal_frame_head_length - comp_index / 2- diff, (P_temp) - comp_index / 2 + diff);
                            decode[OOK_pack_length+0] = (byte) ((mkk & 0xFF000000) >> 24);
                            decode[OOK_pack_length+1] = (byte) ((mkk & 0x00FF0000) >> 16);
                            decode[OOK_pack_length+2] = (byte) ((mkk & 0x0000FF00) >> 8);
                            decode[OOK_pack_length+3] = (byte) ((mkk & 0x000000FF) >> 0);

                            int markhere = (int) signal_frame_head_length - comp_index / 2- diff;
                            decode[OOK_pack_length+4] = (byte) ((markhere & 0xFF000000) >> 24);
                            decode[OOK_pack_length+5] = (byte) ((markhere & 0x00FF0000) >> 16);
                            decode[OOK_pack_length+6] = (byte) ((markhere & 0x0000FF00) >> 8);
                            decode[OOK_pack_length+7] = (byte) ((markhere & 0x000000FF) >> 0);

                            fileName = "decode_" + String.valueOf(signal_indicator) + ".csv";

                            if (pre_rs==1){
                                save_decode(decode,fileName,seg);
                            }else{
                                if(header_index==5){
                                    code_indicator = 0;
                                    code_map.put(code_indicator,decode);
                                }else{
                                    if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator<net_pack_num_bit-1){
                                        code_indicator++;
                                        code_map.put(code_indicator,decode);
                                    }else if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator>=net_pack_num_bit-1){
                                        code_indicator=-1;
                                    } else if(code_indicator!=-1){
                                        code_indicator++;
                                        code_map.put(code_indicator,decode);
                                    }
                                }

                            }
                            Log.d("codecode",String.valueOf(mkk)+" "+String.valueOf(header_index)+" "+code_indicator);
                            signal_indicator++;
                        }
                        signal_frame_head_length = 0;

                    }
                    System.arraycopy(decode_input,P_2[0]-OOK_pack_length,decode,0,OOK_pack_length);
                    decode[OOK_pack_length+0] = (byte) ((mkk & 0xFF000000) >> 24);
                    decode[OOK_pack_length+1] = (byte) ((mkk & 0x00FF0000) >> 16);
                    decode[OOK_pack_length+2] = (byte) ((mkk & 0x0000FF00) >> 8);
                    decode[OOK_pack_length+3] = (byte) ((mkk & 0x000000FF) >> 0);

                    decode[OOK_pack_length+4] = 0;
                    decode[OOK_pack_length+5] = 0;
                    decode[OOK_pack_length+6] = 0;
                    decode[OOK_pack_length+7] = 0;
                    fileName = "decode_" + String.valueOf(signal_indicator) + ".csv";

                    if (pre_rs==1){
                        save_decode(decode,fileName,seg);
                    }else{
                        if(current_index==5){
                            code_indicator = 0;
                            code_map.put(code_indicator,decode);
                        }else{

                            if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator<net_pack_num_bit-1){
                                code_indicator++;
                                code_map.put(code_indicator,decode);
                            }else if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator>=net_pack_num_bit-1){
                                code_indicator=-1;
                            } else if(code_indicator!=-1){
                                code_indicator++;
                                code_map.put(code_indicator,decode);
                            }

                        }

                    }
                    Log.d("codecode",String.valueOf(mkk)+" "+String.valueOf(current_index)+" "+code_indicator);
                    signal_indicator++;
                    reuse_index = 1;
                }
            }else{
                //////////////////////////////////////////
                current_index = (int)decode_input[P_2[0] + header_length] * 8 + (int)decode_input[P_2[0] + header_length + 1] * 4 +(int) decode_input[P_2[0] + header_length + 2] * 2 +(int)decode_input[P_2[0] + header_length + 3] * 1;
                switch(current_index){
                    case 3:
                        current_index = 0;
                        break;
                    case 6:
                        current_index = 1;
                        break;
                    case 9:
                        current_index = 2;
                        break;
                    case 12:
                        current_index = 3;
                        break;
                    case 10:
                        current_index = 5;
                        Log.d("codecode","findfind");
                        break;
                    default:
                        current_index = 4;
                        break;
                }




                Log.d("mmmm_index", String.valueOf(current_index) + " " + String.valueOf(header_index));
                boolean b1 = current_index!=4 && ((packet_indicator_2[0] == 0 && (current_index - 1 + 4) % 4 == header_index) || (packet_indicator_2[0] == 2 && current_index % 4 == header_index));
                boolean b2 = header_index==5;
                if (comp_index > 0 &&(b1||b2)) {
                    if (comp_index % 2 == 1) {
                        diff = (int) (signal_frame_head_length - Math.floor(comp_index / 2)) - (int) Math.floor((int) (signal_frame_head_length - Math.floor(comp_index / 2))/4)*4;

                        System.arraycopy(signal_frame_head, 0, decode, 0, (int) (signal_frame_head_length - Math.floor(comp_index / 2)) - diff);
                        System.arraycopy(decode_input, (int) Math.floor(comp_index / 2) + 1 - diff, decode,(int) (signal_frame_head_length - Math.floor(comp_index / 2)) - diff, (P_2[0]) - (int) (Math.floor(comp_index / 2) + 1) + diff);

                        decode[OOK_pack_length+0] = (byte) ((mkk & 0xFF000000) >> 24);
                        decode[OOK_pack_length+1] = (byte) ((mkk & 0x00FF0000) >> 16);
                        decode[OOK_pack_length+2] = (byte) ((mkk & 0x0000FF00) >> 8);
                        decode[OOK_pack_length+3] = (byte) ((mkk & 0x000000FF) >> 0);

                        int markhere = (int) (signal_frame_head_length - Math.floor(comp_index / 2)) - diff;
                        decode[OOK_pack_length+4] = (byte) ((markhere & 0xFF000000) >> 24);
                        decode[OOK_pack_length+5] = (byte) ((markhere & 0x00FF0000) >> 16);
                        decode[OOK_pack_length+6] = (byte) ((markhere & 0x0000FF00) >> 8);
                        decode[OOK_pack_length+7] = (byte) ((markhere & 0x000000FF) >> 0);

                        fileName = "decode_" + String.valueOf(signal_indicator) + ".csv";

                        if (pre_rs==1){
                            save_decode(decode,fileName,seg);
                        }else{
                            if(header_index==5){
                                code_indicator = 0;
                                code_map.put(code_indicator,decode);
                            }else{
                                if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator<net_pack_num_bit-1){
                                    code_indicator++;
                                    code_map.put(code_indicator,decode);
                                }else if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator>=net_pack_num_bit-1){
                                    code_indicator=-1;
                                } else if(code_indicator!=-1){
                                    code_indicator++;
                                    code_map.put(code_indicator,decode);
                                }

                            }

                        }
                        Log.d("codecode",mkk+" "+String.valueOf(header_index)+" "+code_indicator);
                        signal_indicator++;
                    } else {
                        diff = (int) (signal_frame_head_length - comp_index / 2) - (int) Math.floor((int) (signal_frame_head_length - comp_index / 2)/4)*4;

                        System.arraycopy(signal_frame_head, 0, decode, 0, signal_frame_head_length - comp_index / 2 - diff);
                        System.arraycopy(decode_input, comp_index / 2 - diff, decode, signal_frame_head_length - comp_index / 2 - diff, (P_2[0]) - comp_index / 2 + diff);
                        decode[OOK_pack_length+0] = (byte) ((mkk & 0xFF000000) >> 24);
                        decode[OOK_pack_length+1] = (byte) ((mkk & 0x00FF0000) >> 16);
                        decode[OOK_pack_length+2] = (byte) ((mkk & 0x0000FF00) >> 8);
                        decode[OOK_pack_length+3] = (byte) ((mkk & 0x000000FF) >> 0);

                        int markhere = signal_frame_head_length - comp_index / 2 - diff;
                        decode[OOK_pack_length+4] = (byte) ((markhere & 0xFF000000) >> 24);
                        decode[OOK_pack_length+5] = (byte) ((markhere & 0x00FF0000) >> 16);
                        decode[OOK_pack_length+6] = (byte) ((markhere & 0x0000FF00) >> 8);
                        decode[OOK_pack_length+7] = (byte) ((markhere & 0x000000FF) >> 0);

                        fileName = "decode_" + String.valueOf(signal_indicator) + ".csv";

                        if (pre_rs==1){
                            save_decode(decode,fileName,seg);
                        }else{
                            if(header_index==5){
                                code_indicator = 0;
                                code_map.put(code_indicator,decode);
                            }else{
                                if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator<net_pack_num_bit-1){
                                    code_indicator++;
                                    code_map.put(code_indicator,decode);
                                }else if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator>=net_pack_num_bit-1){
                                    code_indicator=-1;
                                } else if(code_indicator!=-1){
                                    code_indicator++;
                                    code_map.put(code_indicator,decode);
                                }
                            }

                        }
                        Log.d("codecode",mkk+" "+String.valueOf(header_index)+" "+code_indicator);
                        signal_indicator++;
                    }
                    signal_frame_head_length = 0;
                    reuse_index = 1;
                } else {
                    signal_frame_head_length = 0;
                }
            }
        }
        signal_frame_head_length = 0;
        if(P_2[0]+OOK_pack_length<=decode_input.length){
            current_index = (int)decode_input[P_2[0] + header_length] * 8 + (int)decode_input[P_2[0] + header_length + 1] * 4 +(int) decode_input[P_2[0] + header_length + 2] * 2 +(int)decode_input[P_2[0] + header_length + 3] * 1;
            switch(current_index){
                case 3:
                    current_index = 0;
                    break;
                case 6:
                    current_index = 1;
                    break;
                case 9:
                    current_index = 2;
                    break;
                case 12:
                    current_index = 3;
                    break;
                case 10:
                    current_index = 5;
                    Log.d("codecode","findfind");
                    break;
                default:
                    current_index = 4;
                    break;
            }

            System.arraycopy(decode_input,P_2[0],decode,0,OOK_pack_length);
            decode[OOK_pack_length+0] = (byte) ((mkk & 0xFF000000) >> 24);
            decode[OOK_pack_length+1] = (byte) ((mkk & 0x00FF0000) >> 16);
            decode[OOK_pack_length+2] = (byte) ((mkk & 0x0000FF00) >> 8);
            decode[OOK_pack_length+3] = (byte) ((mkk & 0x000000FF) >> 0);

            decode[OOK_pack_length+4] = 0;
            decode[OOK_pack_length+5] = 0;
            decode[OOK_pack_length+6] = 0;
            decode[OOK_pack_length+7] = 0;
            fileName = "decode_" + String.valueOf(signal_indicator) + ".csv";

            if (pre_rs==1){
                save_decode(decode,fileName,seg);
            }else{
                if(current_index==5){
                    code_indicator = 0;
                    code_map.put(code_indicator,decode);
                }else{
                    if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator<net_pack_num_bit-1){
                        code_indicator++;
                        code_map.put(code_indicator,decode);
                    }else if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator>=net_pack_num_bit-1){
                        code_indicator=-1;
                    } else if(code_indicator!=-1){
                        code_indicator++;
                        code_map.put(code_indicator,decode);
                    }
                }

            }
            Log.d("codecode",mkk+" "+String.valueOf(current_index)+" "+code_indicator);
            signal_indicator++;
        }else{

            System.arraycopy(decode_input,P_2[0],signal_frame_head,0,decode_input.length-P_2[0]);
/////////////////////////////////////////////////
            signal_frame_head_length = decode_input.length-P_2[0];
            //Log.d("packet",mkk+" "+"pos 2"+" "+header_length+" "+signal_frame_head_length+" "+decode_input.length);
            frame_index = packet_indicator_2[0];
            header_index = (int)decode_input[P_2[0]+header_length]*8+(int)decode_input[P_2[0]+header_length+1]*4+(int)decode_input[P_2[0]+header_length+2]*2+(int)decode_input[P_2[0]+header_length+3]*1;

            switch(header_index){
                case 3:
                    header_index = 0;
                    break;
                case 6:
                    header_index = 1;
                    break;
                case 9:
                    header_index = 2;
                    break;
                case 12:
                    header_index = 3;
                    break;
                case 10:
                    header_index = 5;
                    Log.d("codecode","findfind");
                    break;
                default:
                    header_index = 4;
                    break;
            }
            //////////////////////////////////////////////////////////////////////////////////////
            signal_frame_head_output[0] = (byte) ((signal_frame_head_length & 0xFF00)>> 8);
            signal_frame_head_output[1] = (byte) (signal_frame_head_length & 0x00FF);
            signal_frame_head_output[2] = (byte) (header_index & 0xFF);
            signal_frame_head_output[3] = (byte) (frame_index & 0xFF);
            Log.d("packet",kk+" "+"header_index"+" "+signal_frame_head_output[2]);

            System.arraycopy(signal_frame_head,0,signal_frame_head_output,4,signal_frame_head_length);

            comp_index = P_2[0]+signal_frame_head_length-OOK_pack_length;
            if( (packet_indicator_2[0]==1 || packet_indicator_2[0]==2) && comp_index>0 && reuse_index==0){
                if(comp_index % 2 ==1 && signal_frame_head_length-Math.floor(comp_index/2)>0){
                    diff = (int) (signal_frame_head_length - Math.floor(comp_index / 2)) - (int) Math.floor((int) (signal_frame_head_length - Math.floor(comp_index / 2))/4)*4;

                    System.arraycopy(signal_frame_head,0,decode,0,(int) (signal_frame_head_length-Math.floor(comp_index/2)) -diff);
                    System.arraycopy(decode_input,(int) Math.floor(comp_index/2)+1 -diff, decode,(int) (signal_frame_head_length-Math.floor(comp_index/2))-diff,  (P_2[0]) - (int)(Math.floor(comp_index/2)+1) + diff);
                    decode[OOK_pack_length+0] = (byte) ((mkk & 0xFF000000) >> 24);
                    decode[OOK_pack_length+1] = (byte) ((mkk & 0x00FF0000) >> 16);
                    decode[OOK_pack_length+2] = (byte) ((mkk & 0x0000FF00) >> 8);
                    decode[OOK_pack_length+3] = (byte) ((mkk & 0x000000FF) >> 0);

                    int markhere = (int) (signal_frame_head_length-Math.floor(comp_index/2))-diff;
                    decode[OOK_pack_length+4] = (byte) ((markhere & 0xFF000000) >> 24);
                    decode[OOK_pack_length+5] = (byte) ((markhere & 0x00FF0000) >> 16);
                    decode[OOK_pack_length+6] = (byte) ((markhere & 0x0000FF00) >> 8);
                    decode[OOK_pack_length+7] = (byte) ((markhere & 0x000000FF) >> 0);

                    fileName = "decode_" + String.valueOf(signal_indicator) + ".csv";

                    if (pre_rs==1){
                        save_decode(decode,fileName,seg);
                    }else{
                        if(header_index==5){
                            code_indicator = 0;
                            code_map.put(code_indicator,decode);
                        }else{
                            if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator<net_pack_num_bit-1){
                                code_indicator++;
                                code_map.put(code_indicator,decode);
                            }else if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator>=net_pack_num_bit-1){
                                code_indicator=-1;
                            } else if(code_indicator!=-1){
                                code_indicator++;
                                code_map.put(code_indicator,decode);
                            }
                        }

                    }
                    Log.d("packet",mkk+"backforward");
                    Log.d("codecode",mkk+" "+String.valueOf(header_index)+" "+code_indicator);
                    signal_indicator++;
                }else if(comp_index%2==0 && signal_frame_head_length-comp_index/2>0){
                    if(signal_frame_head_length-comp_index/2<0){
                        Log.d("mmmm","error "+String.valueOf(signal_frame_head_length)+" "+String.valueOf(comp_index));
                    }
                    diff = (int) (signal_frame_head_length - comp_index / 2) - (int) Math.floor((int) (signal_frame_head_length - comp_index / 2)/4)*4;

                    System.arraycopy(signal_frame_head,0,decode,0,signal_frame_head_length-comp_index/2 - diff);
                    System.arraycopy(decode_input, comp_index/2 - diff, decode,signal_frame_head_length-comp_index/2 - diff,  (P_2[0]) - comp_index/2 + diff);
                    decode[OOK_pack_length+0] = (byte) ((mkk & 0xFF000000) >> 24);
                    decode[OOK_pack_length+1] = (byte) ((mkk & 0x00FF0000) >> 16);
                    decode[OOK_pack_length+2] = (byte) ((mkk & 0x0000FF00) >> 8);
                    decode[OOK_pack_length+3] = (byte) ((mkk & 0x000000FF) >> 0);

                    int markhere = signal_frame_head_length-comp_index/2 - diff;
                    decode[OOK_pack_length+4] = (byte) ((markhere & 0xFF000000) >> 24);
                    decode[OOK_pack_length+5] = (byte) ((markhere & 0x00FF0000) >> 16);
                    decode[OOK_pack_length+6] = (byte) ((markhere & 0x0000FF00) >> 8);
                    decode[OOK_pack_length+7] = (byte) ((markhere & 0x000000FF) >> 0);
                    fileName = "decode_" + String.valueOf(signal_indicator) + ".csv";

                    if (pre_rs==1){
                        save_decode(decode,fileName,seg);
                    }else{
                        if(header_index==5){
                            code_indicator = 0;
                            code_map.put(code_indicator,decode);
                        }else{

                            if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator<net_pack_num_bit-1){
                                code_indicator++;
                                code_map.put(code_indicator,decode);
                            }else if(code_indicator!=-1 && net_pack_num_bit!=-1 && code_indicator>=net_pack_num_bit-1){
                                code_indicator=-1;
                            } else if(code_indicator!=-1){
                                code_indicator++;
                                code_map.put(code_indicator,decode);
                            }

                        }

                    }
                    Log.d("packet",mkk+"backforward");
                    Log.d("codecode",mkk+" "+String.valueOf(header_index)+" "+code_indicator);
                    signal_indicator++;
                }
                signal_frame_head_length=0;
            } else if( (packet_indicator_2[0]==1 || packet_indicator_2[0]==2) && reuse_index==1){
                signal_frame_head_length=0;
            }
        }
        if(P_2_length==2 && decode_input.length-P_2[1]>header_length){
            System.arraycopy(decode_input,P_2[1],signal_frame_head,0,decode_input.length-P_2[1]);

            signal_frame_head_length = decode_input.length-P_2[1];
            //Log.d("packet",mkk+" "+"pos 3"+header_length+" "+signal_frame_head_length);
            ////////////////////////////////////
            frame_index = packet_indicator_2[1];
            header_index = (int)decode_input[P_2[1]+header_length]*8+(int)decode_input[P_2[1]+header_length+1]*4+(int)decode_input[P_2[1]+header_length+2]*2+(int)decode_input[P_2[1]+header_length+3]*1;

            switch(header_index){
                case 3:
                    header_index = 0;
                    break;
                case 6:
                    header_index = 1;
                    break;
                case 9:
                    header_index = 2;
                    break;
                case 12:
                    header_index = 3;
                    break;
                case 10:
                    header_index = 5;
                    Log.d("codecode","findfind");
                    break;
                default:
                    header_index = 4;
                    signal_frame_head_length = 0;
                    break;
            }
            //////////////////////////////////////////////////////////////////////////////////////
            signal_frame_head_output[0] = (byte) ((signal_frame_head_length & 0xFF00)>> 8);
            signal_frame_head_output[1] = (byte) (signal_frame_head_length & 0x00FF);
            signal_frame_head_output[2] = (byte) (header_index & 0xFF);
            signal_frame_head_output[3] = (byte) (frame_index & 0xFF);
            System.arraycopy(signal_frame_head,0,signal_frame_head_output,4,signal_frame_head_length);

            if(packet_indicator_2[1]==1||packet_indicator_2[1]==2){
                signal_frame_head_length = 0;
            }
        }
        if(signal_frame_head_length==header_length){
            signal_frame_head_length=0;
        }
        if(header_index==4){
            signal_frame_head_length = 0;
        }

        if(signal_indicator==indicator_past){
            //if(code_indicator!=-1){
            //    code_indicator++;
            //}
            Log.d("codecode",mkk+" "+"lost packet");
        }

        if(signal_frame_head_length>0){
            Log.d("packet",mkk+" "+"signal frame head save"+" "+ (signal_frame_head_length));
            signal_frame_head_map.put(mkk,signal_frame_head_output);
        }else{
            byte[] put_temp = {0};
            signal_frame_head_map.put(mkk,put_temp);
        }
        long endTime = System.currentTimeMillis(); //结束时间
        long runTime = endTime - startTime;
        Log.d("test_packet",String.valueOf(runTime));
    }

    int[] LOOKUP_34 = {0,7,4,3,0,2,6,2,1,1,5,0,3,4,7,0};
    private void save_decode(byte[] bytes,String fileName,int seg){
        long startTime = System.currentTimeMillis();

        if(pre_rs == 1){
            byte[] byte_64 = Base64.encode(bytes, Base64.DEFAULT);
            Activity activity = getActivity();
            File file = new File(activity.getExternalFilesDir(null),
                    fileName);

            FileOutputStream outputStream = null;
            try {
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                outputStream = new FileOutputStream(file);
                outputStream.write(byte_64);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if(pre_rs == 0) {
            //GenericGF QQ = new GenericGF(0x011D, 256, 0);
            ReedSolomonDecoder rsDecoder = new ReedSolomonDecoder(QR_CODE_FIELD_256);//QR_CODE_FIELD_256
            int[] received = new int[codeword_length];
            int[] received_temp = new int[(int) Math.floor(((double) net_pack_length-4)/4) *3*pack_to];
            int received_value = 0;
            byte[] decoded = new byte[(codeword_length - twoS)*codeword_num];
            Log.d("mmmm","codeword_length"+codeword_length+" "+"twoS"+twoS);
            int index = 20 ;
            int pack_num = 0;
            int i=0;
            byte[] mask_temp = new byte[3];
            int decode_length = received_temp.length;

            if(seg==1 || (seg!=Math.ceil((double)(net_pack_num_bit)/(double)pack_to) && net_pack_num_bit!=-1)){
                decode_length = received_temp.length;
            }else{
                decode_length = total_symbol-(int)Math.floor((double)(net_pack_num_bit)/(double)pack_to)*symbol_length+twoS;
                decode_length = (int) Math.ceil((double)decode_length*8/3)*3;
            }
            Log.d("decode_length",seg+" "+total_symbol+" "+net_pack_num_bit+" "+decode_length);
            while(i!=decode_length) {
                received_value = 0;

                if ((index) % OOK_pack_length == 0 || (index + 1) % OOK_pack_length == 0 || (index + 2) % OOK_pack_length == 0 || (index + 3) % OOK_pack_length == 0) {
                    index = (int) Math.ceil((double) index / OOK_pack_length) * OOK_pack_length + 20;

                }
                for (int j = 0; j < 4; j++) {
                    received_value = received_value +(int) ( (bytes[index] & 0xFF)) * (int) Math.pow(2, (3 - j));
                    index++;
                }
                switch(LOOKUP_34[received_value]) {
                    case 0:
                        received_temp[i] = 0^mask_temp[0];
                        received_temp[i + 1] = 0^mask_temp[1];
                        received_temp[i + 2] = 0^mask_temp[2];
                        i = i + 3;
                        break;
                    case 1:
                        received_temp[i] = 0^mask_temp[0];
                        received_temp[i + 1] = 0^mask_temp[1];
                        received_temp[i + 2] = 1^mask_temp[2];
                        i = i + 3;
                        break;
                    case 2:
                        received_temp[i] = 0^mask_temp[0];
                        received_temp[i + 1] = 1^mask_temp[1];
                        received_temp[i + 2] = 0^mask_temp[2];
                        i = i + 3;
                        break;
                    case 3:
                        received_temp[i] = 0^mask_temp[0];
                        received_temp[i + 1] = 1^mask_temp[1];
                        received_temp[i + 2] = 1^mask_temp[2];
                        i = i + 3;
                        break;
                    case 4:
                        received_temp[i] = 1^mask_temp[0];
                        received_temp[i + 1] = 0^mask_temp[1];
                        received_temp[i + 2] = 0^mask_temp[2];
                        i = i + 3;
                        break;
                    case 5:
                        received_temp[i] = 1^mask_temp[0];
                        received_temp[i + 1] = 0^mask_temp[1];
                        received_temp[i + 2] = 1^mask_temp[2];
                        i = i + 3;
                        break;
                    case 6:
                        received_temp[i] = 1^mask_temp[0];
                        received_temp[i + 1] = 1^mask_temp[1];
                        received_temp[i + 2] = 0^mask_temp[2];
                        i = i + 3;
                        break;
                    case 7:
                        received_temp[i] = 1^mask_temp[0];
                        received_temp[i + 1] = 1^mask_temp[1];
                        received_temp[i + 2] = 1^mask_temp[2];
                        i = i + 3;
                        break;

                }
            }
            index=0;
            int[] save_decoded = new int[codeword_length];
            for(i=0;i<codeword_num;i++) {
                received = new int[codeword_length];

                if(seg==1 || (seg!=Math.ceil((double)(net_pack_num_bit+1)/(double)pack_to) && net_pack_num_bit!=-1)){
                    decode_length = codeword_length;
                }else{
                    decode_length = total_symbol-(int)Math.floor((double)(net_pack_num_bit+1)/(double)pack_to)*symbol_length+twoS;
                }

                for (int kkk = 0; kkk < codeword_length; kkk++) {
                    if(kkk>decode_length-1){
                        received[kkk] = 0;
                    }else {
                        for (int j = 0; j < 8; j++) {
                            received[kkk] = received[kkk] + +(int) (received_temp[index] & 0xFF) * (int) Math.pow(2, (7 - j));
                            index++;
                        }
                    }

                }

                for(int k=0;k<symbol_length;k++){
                    save_decoded[k] = received[twoS+k];
                }
                for(int k=symbol_length;k<codeword_length;k++){
                    save_decoded[k] = received[k-symbol_length];
                }
                try {
                    rsDecoder.decode(save_decoded, twoS);//Log.d("mmmm",String.valueOf(i)+" "+String.valueOf(pack_num));
                    for (int kkk = 0; kkk < codeword_length - twoS; kkk++) {
                        decoded[ (i-pack_num)*(codeword_length-twoS)+kkk ] = (byte) save_decoded[kkk];

                    }
                    if(seg==1){
                        net_pack_num_bit =(decoded[0]&0xFF);
                        total_symbol = (decoded[1]&0xFF);
                        total_pack_num = (decoded[3]&0xFF);
                    }

                    //Log.d("mmmm",String.valueOf(decoded[3]));
                } catch (ReedSolomonException ignored) {
                    Log.d("mmmm", "ReedSolomonException");
                    pack_num++;
                }

            }

            if(pack_num!=codeword_num) {
                received_map.put(seg,decoded);
                Log.d("codecode","save"+seg+" "+decoded.length);
                byte[] byte_64 = Base64.encode(decoded, Base64.DEFAULT);
                Activity activity = getActivity();
                File file = new File(activity.getExternalFilesDir(null),
                        fileName);

                FileOutputStream outputStream = null;
                try {
                    if (file.exists()) {
                        file.delete();
                    }
                    file.createNewFile();
                    outputStream = new FileOutputStream(file);
                    outputStream.write(byte_64);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        long endTime = System.currentTimeMillis(); //结束时间
        long runTime = endTime - startTime;
        Log.d("test_decode",String.valueOf(runTime));
    }
    private void remove_hash (){
        Set<Integer> keys = code_map.keySet();
        Iterator<Integer> iterator1 = keys.iterator();
        ArrayList<Integer> decoded_delete = new ArrayList<>();

        while (iterator1.hasNext()) {
            int kk_temp = iterator1.next();
            decoded_delete.add(kk_temp);
        }
        for(int i=0;i<decoded_delete.size();i++){
            code_map.remove(decoded_delete.get(i));
        }


        keys = received_map.keySet();
        iterator1 = keys.iterator();
        decoded_delete = new ArrayList<>();

        while (iterator1.hasNext()) {
            int kk_temp = iterator1.next();
            decoded_delete.add(kk_temp);
        }
        for(int i=0;i<decoded_delete.size();i++){
            received_map.remove(decoded_delete.get(i));
        }

        //arrlist.clear();
        startTime_total = System.currentTimeMillis();
    }

}




