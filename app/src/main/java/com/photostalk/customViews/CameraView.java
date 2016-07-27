package com.photostalk.customViews;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

/**
 * Created by mohammed on 2/20/16.
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;

    public CameraView(Context context/*, Camera camera*/) {
        super(context);

        setFocusable(true);
        setFocusableInTouchMode(true);

        //mCamera = camera;

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mCamera.autoFocus(null);
        }

        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (mCamera == null) return;
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
            mCamera.autoFocus(null);
        } catch (IOException e) {
            Log.d("CameraView", "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {
        if (mCamera == null) return;
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mSurfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        Camera.Parameters parameters = mCamera.getParameters();
        Display display = ((WindowManager) getContext().getSystemService(AppCompatActivity.WINDOW_SERVICE)).getDefaultDisplay();
        //check for supported sizes to avoid exceptions
        Camera.Size size = getBestPreviewSize(width, height, parameters);

        if (display.getRotation() == Surface.ROTATION_0) {
            parameters.setPreviewSize(size.width, size.height);
            mCamera.setDisplayOrientation(90);
        }

        if (display.getRotation() == Surface.ROTATION_90) {
            parameters.setPreviewSize(size.width, size.height);
        }

        if (display.getRotation() == Surface.ROTATION_180) {
            parameters.setPreviewSize(size.width, size.height);
        }

        if (display.getRotation() == Surface.ROTATION_270) {
            parameters.setPreviewSize(width, height);
            mCamera.setDisplayOrientation(180);
        }

        int suggestedEdge = 1000;
        float aspectRatio = (float) Math.max(width, height) / Math.min(width, height);
        int difference = Integer.MAX_VALUE;
        float aspectRatioThreshold = 0.2f;

        List<Camera.Size> supportedSizes = parameters.getSupportedPictureSizes();
        Camera.Size sizePicture = null;

        for (int j = 0; j < supportedSizes.size(); j++) {
            size = supportedSizes.get(j);
            int min = Math.min(size.height, size.width);
            int max = Math.max(size.height, size.width);
            int tmp = Math.abs(max - suggestedEdge);
            float tmpAspectRatio = (float) max / min;
            boolean isGoodAspectRatio = aspectRatio - aspectRatioThreshold <= tmpAspectRatio && tmpAspectRatio <= aspectRatio + aspectRatioThreshold;
            if (difference > tmp && isGoodAspectRatio) {
                difference = tmp;
                sizePicture = size;
            }
        }

        parameters.setJpegQuality(100);
        //parameters.setPictureFormat(ImageFormat.JPEG);

        if (sizePicture != null)
            parameters.setPictureSize(sizePicture.width, sizePicture.height);
        //Camera.Size size = parameters.getSupportedPictureSizes().get(0);
        //parameters.setPictureSize(size.width, size.height);

        mCamera.setParameters(parameters);

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
            mCamera.autoFocus(null);

        } catch (Exception e) {
            Log.d("CameraView", "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size bestSize = null;
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();

        bestSize = sizeList.get(0);

        for (int i = 1; i < sizeList.size(); i++) {
            if ((sizeList.get(i).width * sizeList.get(i).height) >
                    (bestSize.width * bestSize.height)) {
                bestSize = sizeList.get(i);
            }
        }

        return bestSize;
    }
}
