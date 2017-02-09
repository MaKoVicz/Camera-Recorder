package com.example.camerarecorder;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraMain extends AppCompatActivity {

    //region Initiation
    private Camera mCamera;
    private MyCameraSurfaceView myCameraSurfaceView;
    private MediaRecorder mediaRecorder;
    private Button btnRecord;
    private SurfaceHolder surfaceHolder;
    boolean isRecording;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_main);

        isRecording = false;
        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);

        if (mCamera == null) {
            finish();
            Toast.makeText(this, "Fail to get Camera", Toast.LENGTH_SHORT).show();
        }

        myCameraSurfaceView = new MyCameraSurfaceView(this, mCamera);
        FrameLayout mCameraPreview = (FrameLayout) findViewById(R.id.videoView);
        mCameraPreview.addView(myCameraSurfaceView);

        btnRecord = (Button) findViewById(R.id.btnRecord);
        setOnButtonRecordClick();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    public Camera getCameraInstance() {
        Camera c = null;

        try {
            c = Camera.open();
        } catch (Exception ex) {
        }

        return c;
    }

    public void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = new MediaRecorder();
            mCamera.lock();           // lock camera for later use
        }
    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public String returnOutputPath() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss");
        Date currentDate = new Date();
        String strDate = sdfDate.format(currentDate);
        String outputPath = Environment.getExternalStorageDirectory().getPath()
                + File.separator + strDate + ".mp4";

        return outputPath;
    }

    public boolean prepareMediaRecorder() {
        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);
        mediaRecorder = new MediaRecorder();

        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

        mediaRecorder.setOutputFile(returnOutputPath());
        mediaRecorder.setMaxDuration(60000);
        mediaRecorder.setMaxFileSize(50000000);

        mediaRecorder.setPreviewDisplay(myCameraSurfaceView.getHolder().getSurface());
        mediaRecorder.setOrientationHint(90);

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }

        return true;
    }

    public void setOnButtonRecordClick() {
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (isRecording) {
                        //stop recording and release the camera
                        mediaRecorder.stop();
                        releaseMediaRecorder();

                        //Exit after saved
                        //finish();
                        btnRecord.setText("REC");
                        isRecording = false;
                    } else {
                        //Release Camera before MediaRecorder start
                        releaseCamera();

                        if (!prepareMediaRecorder()) {
                            Toast.makeText(CameraMain.this, "Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
                            finish();
                        }

                        mediaRecorder.start();
                        isRecording = true;
                        btnRecord.setText("STOP");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public class MyCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

        private SurfaceHolder mHolder;
        private Camera mCamera;

        public MyCameraSurfaceView(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // TODO Auto-generated method stub
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mHolder.getSurface() == null) {
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview
            }

            // make any resize, rotate or reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e) {
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }
}