package com.example.yapa;

/**
 * Created by Dar on 5/4/2014.
 */

import android.content.Context;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/** A basic Camera cameraPreview class, provided by http://developer.android.com/guide/topics/media/camera.html#manifest */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    private final String TAG = "camera-cameraPreview";

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if(holder == null) { //...
            Log.d(TAG, "Wonderful, null holder passed to surfaceCreated");
            return;
        }
        // The Surface has been created, now tell the camera where to draw the cameraPreview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera cameraPreview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera cameraPreview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your cameraPreview can change or rotate, take care of those events here.
        // Make sure to stop the cameraPreview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // cameraPreview surface does not exist
            return;
        }

        // stop cameraPreview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent cameraPreview
        }

        // set cameraPreview size and make any resize, rotate or
        // reformatting changes here

        // start cameraPreview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera cameraPreview: " + e.getMessage());
        }
    }
}
