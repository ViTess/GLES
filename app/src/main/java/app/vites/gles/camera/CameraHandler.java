package app.vites.gles.camera;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.support.annotation.IntDef;

/**
 * Created by trs on 19-3-29.
 */
public class CameraHandler extends HandlerThread implements Handler.Callback {

    public static final int CAMERA_ID_DEFAULT = 0;//打开任意一个摄像头
    public static final int CAMERA_ID_BACK = 1;//打开后置摄像头
    public static final int CAMERA_ID_FRONT = 2;//打开前置摄像头

    @IntDef({CAMERA_ID_DEFAULT, CAMERA_ID_BACK, CAMERA_ID_FRONT})
    public @interface CameraFacing {
    }

    private static final int MSG_WHAT_OPEN = 1;
    private static final int MSG_WHAT_RELEASE = 2;
    private static final int MSG_WHAT_START_PREVIEW = 3;
    private static final int MSG_WHAT_STOP_PREVIEW = 4;

    private boolean isCameraOpen = false;
    private boolean isStartPreview = false;

    @CameraFacing
    private int mCameraId;
    private int mWidth;
    private int mHeight;
    private int mRotation;

    private final Handler mHandler;
    private Object mSurface;
    private OnCameraListener mListener;

    public CameraHandler(String name) {
        this(name, Process.THREAD_PRIORITY_DEFAULT);
    }

    public CameraHandler(String name, int priority) {
        super(name, priority);
        start();
        mHandler = new Handler(getLooper(), this);
    }

    public int getWidth() {
        return CameraManager.getInstance().getWidth();
    }

    public int getHeight() {
        return CameraManager.getInstance().getHeight();
    }

    public void setOnCameraListener(OnCameraListener listener) {
        mListener = listener;
    }

    public synchronized void openCamera(@CameraFacing int id, int width, int height) {
        mCameraId = id;
        mWidth = width;
        mHeight = height;

        mHandler.sendEmptyMessage(MSG_WHAT_OPEN);

        try {
            this.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void releaseCamera() {
        mHandler.sendEmptyMessage(MSG_WHAT_RELEASE);
    }

    public void startPreview(Object surface) {
        mSurface = surface;
        mHandler.sendEmptyMessage(MSG_WHAT_START_PREVIEW);
    }

    public void stopPreview() {
        mHandler.sendEmptyMessage(MSG_WHAT_STOP_PREVIEW);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_WHAT_OPEN:
                handleOpenCamera();
                return true;
            case MSG_WHAT_RELEASE:
                handleReleaseCamera();
                return true;
            case MSG_WHAT_START_PREVIEW:
                handleStartPreview();
                return true;
            case MSG_WHAT_STOP_PREVIEW:
                handleStopPreview();
                return true;
        }
        return false;
    }

    private void handleOpenCamera() {
        if (!isCameraOpen) {
            CameraManager.getInstance().openCamera(mCameraId, mWidth, mHeight);
            isCameraOpen = true;

            if (mListener != null)
                mListener.onCameraOpen(CameraManager.getInstance().getWidth(), CameraManager.getInstance().getHeight());
        }

        synchronized (this) {
            this.notify();
        }
    }

    private void handleReleaseCamera() {
        if (isCameraOpen) {
            CameraManager.getInstance().closeCamera();
            isCameraOpen = false;

            if (mListener != null)
                mListener.onCameraClose();
        }
    }

    private void handleStartPreview() {
        if (!isStartPreview) {
            CameraManager.getInstance().setPreview(mSurface);
            CameraManager.getInstance().startPreview();
            isStartPreview = true;
        }
    }

    private void handleStopPreview() {
        if (isStartPreview) {
            CameraManager.getInstance().stopPreview();
        }
        isStartPreview = false;
    }

    public interface OnCameraListener {
        void onCameraOpen(int width, int height);

        void onCameraClose();
    }
}
