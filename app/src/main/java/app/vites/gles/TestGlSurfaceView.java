package app.vites.gles;


import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.apkfuns.logutils.LogUtils;

import app.vites.gles.drawable.FBODrawable;
import app.vites.gles.drawable.SourceOverBlendDrawable;

/**
 * Created by trs on 18-6-29.
 */
public class TestGlSurfaceView extends GLSurfaceView {

    public TestGlSurfaceView(Context context) {
        super(context);
        init();
    }

    public TestGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    void init() {
        setEGLContextClientVersion(2);
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });
    }
}
