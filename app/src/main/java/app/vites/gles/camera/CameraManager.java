package app.vites.gles.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import app.vites.gles.App;

import static app.vites.gles.camera.CameraHandler.CAMERA_ID_BACK;
import static app.vites.gles.camera.CameraHandler.CAMERA_ID_DEFAULT;
import static app.vites.gles.camera.CameraHandler.CAMERA_ID_FRONT;

/**
 * Created by trs on 19-3-28.
 */
final class CameraManager {
    private static volatile CameraManager sInstance;

    public static CameraManager getInstance() {
        if (sInstance == null) {
            synchronized (CameraManager.class) {
                if (sInstance == null) {
                    final CameraManager cameraManager = new CameraManager();
                    sInstance = cameraManager;
                }
            }
        }
        return sInstance;
    }

    private static final String TAG = "CameraManager";

    //相机的fps期望值
    private static final int DEFAULT_PREVIEW_FPS = 30;

    private static final int DEFAULT_PREVIEW_WIDTH = 1920;//1280
    private static final int DEFAULT_PREVIEW_HEIGHT = 1080;//720

    @CameraHandler.CameraFacing
    private int mCameraID = CAMERA_ID_DEFAULT;//默认打开后置摄像头
    private Camera mCamera;
    private int mCameraWidth, mCameraHeight;
    private int mSurfaceWidth, mSurfaceHeight;
    private int mPreviewFps;
    private int mRotation;
    private byte mPreviewBuffer[];

    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

            if (mPreviewBuffer != null) {
                synchronized (mPreviewBuffer) {

                    //TODO:在此处获取yuv数据，可按需求转换为rgb、bgr等数据

                    //不断调用该方法,onPreviewFrame才有返回数据
                    if (mCamera != null)
                        mCamera.addCallbackBuffer(mPreviewBuffer);

                }
            }
        }
    };

    public int getWidth() {
        return mCameraWidth;
    }

    public int getHeight() {
        return mCameraHeight;
    }

    public boolean openCamera(@CameraHandler.CameraFacing int cameraId, int width, int height) {
        if (cameraId == CAMERA_ID_DEFAULT) {
            //打开任意摄像头
            try {
                mCamera = Camera.open();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            int realCameraId = -1;
            final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int id = 0; id < Camera.getNumberOfCameras(); id++) {
                Camera.getCameraInfo(id, cameraInfo);
                if ((cameraId == CAMERA_ID_BACK && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
                        || (cameraId == CAMERA_ID_FRONT && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)) {
                    realCameraId = id;
                }
            }

            if (realCameraId == -1) {
                if (cameraId == CAMERA_ID_BACK)
                    Log.e(TAG, "back camera not found!");

                else
                    Log.e(TAG, "front camera not found!");
            } else {
                try {
                    mCamera = Camera.open(realCameraId);

                    mRotation = getRotationDegree(realCameraId);
                    Log.i(TAG, "rotation:" + mRotation);
                    mCamera.setDisplayOrientation(mRotation);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (mCamera == null)
            return false;

        mCameraID = cameraId;
        mSurfaceWidth = width;
        mSurfaceHeight = height;

        initCamera();
        return true;
    }

    public void closeCamera() {
        if (mCamera != null) {
            //xxx:据说在低版本下可能出现崩溃导致手机重启
            mCamera.setPreviewCallback(null);

            mCamera.release();
            mCamera = null;
        }
    }

    public void setPreview(Object surface) {
        if (mCamera != null)
            try {
                //xxx:据说某些机型需要设置buffersize,否则黑屏
//                surfaceTexture.setDefaultBufferSize(mCameraWidth, mCameraHeight);

                if (surface instanceof SurfaceTexture) {
                    mCamera.setPreviewTexture((SurfaceTexture) surface);
                } else if (surface instanceof SurfaceHolder) {
                    mCamera.setPreviewDisplay((SurfaceHolder) surface);
                } else {
                    throw new RuntimeException("Unsupported preview :" + surface);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void startPreview() {
        if (mCamera != null)
            mCamera.startPreview();
    }

    public void stopPreview() {
        if (mCamera != null)
            mCamera.stopPreview();
    }

    private void initCamera() {
        Camera.Parameters params = mCamera.getParameters();
        List<String> FocusModes = params.getSupportedFocusModes();
        if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        //查找支持的尺寸,如果支持的尺寸里有前面设置的尺寸,则直接拿这个尺寸
        //否则,获取跟这个尺寸相近的最低尺寸来显示,至少其中一边要大于默认值

        mCameraWidth = mSurfaceWidth > mSurfaceHeight ? mSurfaceWidth : mSurfaceHeight;
        mCameraHeight = mSurfaceHeight < mSurfaceWidth ? mSurfaceHeight : mSurfaceWidth;

        if (mCameraWidth > DEFAULT_PREVIEW_WIDTH) {
            mCameraWidth = DEFAULT_PREVIEW_WIDTH;
        }

        if (mCameraHeight > DEFAULT_PREVIEW_HEIGHT) {
            mCameraHeight = DEFAULT_PREVIEW_HEIGHT;
        }


        List<Camera.Size> previewSize = params.getSupportedPreviewSizes();
        Camera.Size size = chooseSize(previewSize, mSurfaceWidth, mSurfaceHeight, mCameraWidth, mCameraHeight);
        mCameraWidth = size.width;
        mCameraHeight = size.height;

        Log.i(TAG, "initCamera - mSurfaceWidth:" + mSurfaceWidth);
        Log.i(TAG, "initCamera - mSurfaceHeight:" + mSurfaceHeight);
        Log.i(TAG, "initCamera - width:" + mCameraWidth);
        Log.i(TAG, "initCamera - height:" + mCameraHeight);

        params.setPreviewSize(mCameraWidth, mCameraHeight);
//        params.setPictureSize(mCameraWidth, mCameraHeight);

        params.setPreviewFormat(ImageFormat.NV21);

        //设置缓存大小
        int bufferSize = mCameraWidth * mCameraHeight;
        bufferSize = bufferSize * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
        mPreviewBuffer = new byte[bufferSize];

        //注意某些机型需要在setPreviewCallbackWithBuffer后调用addCallbackBuffer才能接收回调
        mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);
        mCamera.addCallbackBuffer(mPreviewBuffer);

        params.setRecordingHint(true);
        mPreviewFps = chooseFixedPreviewFps(params, DEFAULT_PREVIEW_FPS * 1000);
        mCamera.setParameters(params);
    }

    /**
     * 选择合适的FPS
     *
     * @param parameters
     * @param expectedFps 期望的FPS
     * @return
     */
    private int chooseFixedPreviewFps(Camera.Parameters parameters, int expectedFps) {
        List<int[]> supportedFps = parameters.getSupportedPreviewFpsRange();
        for (int[] entry : supportedFps) {
            if (entry[0] == entry[1] && entry[0] == expectedFps) {
                parameters.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }
        int[] temp = new int[2];
        int guess;
        parameters.getPreviewFpsRange(temp);
        if (temp[0] == temp[1]) {
            guess = temp[0];
        } else {
            guess = temp[1] / 2;
        }
        return guess;
    }

    private static int getRotationDegree(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        int orientation = 0;

        WindowManager wm = (WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE);
        switch (wm.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90:
                orientation = 90;
                break;
            case Surface.ROTATION_180:
                orientation = 180;
                break;
            case Surface.ROTATION_270:
                orientation = 270;
                break;
            case Surface.ROTATION_0:
            default:
                orientation = 0;
                break;
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (720 - (info.orientation + orientation)) % 360;
        } else {
            return (360 - orientation + info.orientation) % 360;
        }
    }

    private static Camera.Size chooseSize(List<Camera.Size> supportSize,
                                          int surfaceWidth, int surfaceHeight,
                                          int maxWidth, int maxHeight) {
        //尺寸选择策略
        //1.具备符合 <=maxSize && ==surfaceSize ， 则使用该尺寸
        //2.仅符合 <=maxSize时，计算surfaceSize宽高比，获取宽高比最接近的尺寸来使用
        //3.不存在符合 <=maxSize 时，计算surfaceSize宽高比，获取宽高比最接近的尺寸来使用

        int temp = surfaceWidth;
        if (surfaceWidth < surfaceHeight) {
            surfaceWidth = surfaceHeight;
            surfaceHeight = temp;
        }

        class TempSize {
            public Camera.Size size;
            public float ratio;
            public float delta;

            public TempSize(Camera.Size size, float ratio, float delta) {
                this.size = size;
                this.ratio = ratio;
                this.delta = delta;
            }
        }

        class TempSizeComparator implements Comparator<TempSize> {

            @Override
            public int compare(TempSize o1, TempSize o2) {
                if (o1.delta == o2.delta) {
                    return Long.signum((long) o2.size.width * o2.size.height - (long) o1.size.width * o1.size.height);
                } else {
                    return o1.delta < o2.delta ? -1 : 1;
                }
            }
        }

        List<TempSize> sizeList = new ArrayList<>();
        Camera.Size resultSize = null;
        float ratio = ((float) surfaceHeight) / surfaceWidth;

//        LogUtils.i("surfaceWidth:%d,surfaceHeight:%d,ratio:%f", surfaceWidth, surfaceHeight, ratio);

        for (Camera.Size size : supportSize) {
            if (size.width <= maxWidth && size.height <= maxHeight &&
                    size.width == surfaceWidth && size.height == surfaceHeight) {
                resultSize = size;
                break;
            } else {
                float r = ((float) size.height) / size.width;
                float d = Math.abs(r - ratio);

                sizeList.add(new TempSize(size, r, d));

//                LogUtils.i("width:%d,height:%d,ratio:%f,delta:%f", size.width, size.height, r, d);
            }
        }

        if (resultSize == null) {
            resultSize = Collections.min(sizeList, new TempSizeComparator()).size;
        }

        return resultSize;
    }

    private static Camera.Size chooseOptimalSize(List<Camera.Size> supportSize,
                                                 int surfaceWidth, int surfaceHeight,
                                                 int maxWidth, int maxHeight,
                                                 Camera.Size aspectRatio) {
        List<Camera.Size> bigEnough = new ArrayList<>();
        List<Camera.Size> notBigEnough = new ArrayList<>();
        float w = aspectRatio.width;
        float h = aspectRatio.height;
        float ratio = h / w;

        for (Camera.Size size : supportSize) {
            if (size.width <= maxWidth &&
                    size.height <= maxHeight &&
                    size.height == size.width * ratio) {
                if (size.width >= surfaceWidth && size.height >= surfaceHeight)
                    bigEnough.add(size);
                else
                    notBigEnough.add(size);
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            return supportSize.get(0);
        }
    }

    private static class CompareSizesByArea implements Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.width * lhs.height - (long) rhs.width * rhs.height);
        }
    }
}
