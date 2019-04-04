package app.vites.gles;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.apkfuns.logutils.LogUtils;

import java.util.LinkedList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import app.vites.gles.drawable.EmptyDrawable;

/**
 * Created by trs on 18-6-29.
 */
public class TestRenderer implements GLSurfaceView.Renderer {

    private IDrawable mDrawable;
    private int mOutputWidth, mOutputHeight;

    private final LinkedList<Runnable> mTaskList = new LinkedList<>();

    public TestRenderer(IDrawable drawable) {
        mDrawable = checkDrawable(drawable);
    }

    public TestRenderer() {

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LogUtils.i("onSurfaceCreated");
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        checkDrawable(mDrawable).init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mOutputWidth = width;
        mOutputHeight = height;
        LogUtils.i("width:%d,height:%d", width, height);
        GLES20.glViewport(0, 0, width, height);
        checkDrawable(mDrawable).setOutputSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        runTasks();
        checkDrawable(mDrawable).draw();
    }

    public void setDrawable(IDrawable drawable) {
        mTaskList.add(() -> {
            final IDrawable oldDrawable = checkDrawable(mDrawable);
            mDrawable = drawable;

            oldDrawable.destroy();
            checkDrawable(mDrawable).setOutputSize(mOutputWidth, mOutputHeight);
            checkDrawable(mDrawable).init();
        });
    }

    private void runTasks() {
        while (!mTaskList.isEmpty()) {
            mTaskList.removeFirst().run();
        }
    }

    public void addTasks(Runnable runnable) {
        mTaskList.add(runnable);
    }

    public void release() {
        mTaskList.clear();
        mTaskList.add(() -> {
            if (mDrawable != null)
                mDrawable.destroy();
        });
    }

    private IDrawable checkDrawable(IDrawable drawable) {
        if (drawable == null)
            return EmptyDrawable.INSTANCE;
        return drawable;
    }
}
