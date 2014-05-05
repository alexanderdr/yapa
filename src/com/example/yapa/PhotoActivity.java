package com.example.yapa;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class PhotoActivity extends Activity {

    private final String TAG = "photo-activity";

    Camera camera;
    CameraPreview cameraPreview;

    DropboxManager dropboxManager;

    Camera.PictureCallback pictureCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dropboxManager = new DropboxManager(this, getApplicationContext());

        camera = getCameraInstance();

        setContentView(R.layout.photo);

        Log.d(TAG, "creating camera preview...");
        cameraPreview = new CameraPreview(this, camera);
        FrameLayout framePreview = (FrameLayout) findViewById(R.id.camera_preview_container);
        Log.d(TAG, "attaching camera preview...");
        framePreview.addView(cameraPreview);
        Log.d(TAG, "camera preview attached...");

        pictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                dropboxManager.saveImageData(data);
                camera.startPreview(); //restart the preview-- takePicture() automatically stops it
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "photo-pause");
        dropboxManager.onPause();
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "photo-resume");
        dropboxManager.onResume();
    }

    private void releaseCamera(){
        if (camera != null){
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }

    public void onPhotoPreviewClicked(View view) {
        try {
            //camera.stopPreview(); //one thing android actually handles, apparently
            camera.takePicture(null, null, pictureCallback);
        } catch (Exception e) {
            Log.d(TAG, "Problem with takePicture()", e);
        }
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private Camera getCameraInstance(){
        if(!checkCameraHardware(getApplicationContext())) {
            Log.d(TAG, "No camera found on device");
            return null;
        }
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
            Log.d("yapam-camera", "Exception attempting to get a camera handle", e);
        }
        return c; // returns null if camera is unavailable
    }
}