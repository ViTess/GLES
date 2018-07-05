package app.vites.gles.drawable;

import com.apkfuns.logutils.LogUtils;

import app.vites.gles.IDrawable;

/**
 * Created by trs on 18-7-2.
 */
public final class EmptyDrawable implements IDrawable {

    public static final EmptyDrawable INSTANCE = new EmptyDrawable();

    private EmptyDrawable() {
    }

    @Override
    public void init() {
        LogUtils.i("init");
    }

    @Override
    public void destroy() {
        LogUtils.i("destory");
    }

    @Override
    public void setOutputSize(int outputWidth, int outputHeight) {
        LogUtils.i("setOutputSize(" + outputWidth + "," + outputHeight + ")");
    }

    @Override
    public void draw() {
        LogUtils.i("draw");
    }
}
