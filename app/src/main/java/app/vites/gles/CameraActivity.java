package app.vites.gles;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import app.vites.gles.camera.CameraHandler;
import app.vites.gles.drawable.CameraDrawable;

/**
 * Created by trs on 19-4-4.
 */
public class CameraActivity extends AppCompatActivity {

    TestGlSurfaceView glSurfaceView;
    CameraDrawable mCameraDrawable;
    CameraHandler mCameraHandler;
    TestRenderer mRenderer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        glSurfaceView = (TestGlSurfaceView) findViewById(R.id.surfaceview);

        mCameraDrawable = new CameraDrawable();
        mCameraHandler = new CameraHandler("CameraHandler");

        mRenderer = new TestRenderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                super.onSurfaceCreated(gl, config);
                mRenderer.addTasks(() -> mCameraDrawable.getSurfaceTexture().setOnFrameAvailableListener(surfaceTexture -> glSurfaceView.requestRender()));
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                super.onSurfaceChanged(gl, width, height);
                mCameraHandler.stopPreview();
                mCameraHandler.releaseCamera();

                mCameraHandler.openCamera(CameraHandler.CAMERA_ID_BACK, width, height);
                mRenderer.addTasks(() -> mCameraHandler.startPreview(mCameraDrawable.getSurfaceTexture()));
            }

            @Override
            public void release() {
                super.release();
                mCameraHandler.stopPreview();
                mCameraHandler.releaseCamera();
            }
        };
        mRenderer.setDrawable(mCameraDrawable);

        glSurfaceView.setRenderer(mRenderer);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mCameraHandler.stopPreview();
        mCameraHandler.releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRenderer.release();
    }
}
