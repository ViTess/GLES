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

    private TestRenderer mRenderer;
    private SourceOverBlendDrawable mSBDrawable;
    private FBODrawable mFBODrawable;

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

        mRenderer = new TestRenderer();
        setRenderer(mRenderer);
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                LogUtils.i("surfaceCreated");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                LogUtils.i("surfaceChanged");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                LogUtils.i("surfaceDestroyed");
            }
        });
    }

    public void setDrawable(IDrawable drawable){
        mRenderer.setDrawable(drawable);
    }
}
