package com.meekworth.lwdronecam;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.view.TextureView;

import com.meekworth.lwdronecam.utils.Log;

class CameraViewListener implements TextureView.SurfaceTextureListener {
    private static final String TAG = "LWDroneCam/CameraViewListener";
    private final DroneCam mDroneCam;
    private final TextureView mTexture;

    CameraViewListener(DroneCam droneCam, TextureView texture) {
        mDroneCam = droneCam;
        mTexture = texture;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.v(TAG, "surface available (%d, %d)", width, height);
        mDroneCam.setSurface(surface);
        adjustSize(DroneCam.DEFAULT_VID_WIDTH, DroneCam.DEFAULT_VID_HEIGHT, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.v(TAG, "surface size changed (%d, %d)", width, height);
        adjustSize(DroneCam.DEFAULT_VID_WIDTH, DroneCam.DEFAULT_VID_HEIGHT, width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.v(TAG, "surface destroyed");
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.v(TAG, "surface updated");
    }

    private void adjustSize(int videoWidth, int videoHeight, int viewWidth, int viewHeight) {
        Matrix m = new Matrix();
        float videoRatio = videoWidth / (float)videoHeight;
        float viewRatio = viewWidth / (float)viewHeight;
        int newWidth;
        int newHeight;

        if (videoRatio <= viewRatio) {
            newWidth = (int)(videoRatio * (float)viewHeight);
            newHeight = viewHeight;
        }
        else {
            newWidth = viewWidth;
            newHeight = (int)(viewWidth / videoRatio);
        }

        m.setScale(newWidth / (float)viewWidth, newHeight / (float)viewHeight);
        m.postTranslate((viewWidth - newWidth) / 2f, (viewHeight - newHeight) / 2f);
        mTexture.setTransform(m);
    }
}
