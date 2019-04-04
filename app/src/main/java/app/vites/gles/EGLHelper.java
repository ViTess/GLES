package app.vites.gles;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.apkfuns.logutils.LogUtils;


/**
 * 用于创建Egl环境
 * <p>
 * Created by trs on 19-3-14.
 */
public final class EGLHelper {

    //android平台对pixmap支持不好，所以只支持这两种
    public static final int SURFACE_TYPE_WINDOW = 1;
    public static final int SURFACE_TYPE_PBUFFER = 2;

    @IntDef({SURFACE_TYPE_WINDOW, SURFACE_TYPE_PBUFFER})
    public @interface SurfaceType {
    }

    private EGLConfig mEGLConfig;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EGLSurface mEglSurface;
    @SurfaceType
    private int mSurfaceType;
    private Object mSurface;
    private int mWidth, mHeight;
    private boolean isPrintInfo;

    public EGLHelper(@SurfaceType int type) {
        mSurfaceType = type;
        setSurface(null);
    }

    public void setPrintInfo(boolean isPrint) {
        isPrintInfo = isPrint;
    }

    public void init() {
        checkThread();

        //获取显示设备，android都是默认
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

        if (isPrintInfo) {
            String vendor = EGL14.eglQueryString(mEGLDisplay, EGL14.EGL_VENDOR);
            LogUtils.i("egl vendor:" + vendor);//egl实现厂商

            String versionStr = EGL14.eglQueryString(mEGLDisplay, EGL14.EGL_VERSION);
            LogUtils.i("egl version: " + versionStr);//egl版本号

            String extension = EGL14.eglQueryString(mEGLDisplay, EGL14.EGL_EXTENSIONS);
            LogUtils.i("egl extension: " + extension); //egl支持的扩展
        }

        int[] version = new int[2];
        //分别获取主版本号和次版本号
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            checkError("eglInitialize");
        }

        if (isPrintInfo) {
            LogUtils.i("egl version code:" + version[0] + "," + version[1]);
        }

        int[] attrs = new int[]{
                EGL14.EGL_RED_SIZE, 8, //指定R大小(bits)
                EGL14.EGL_GREEN_SIZE, 8, //指定G大小
                EGL14.EGL_BLUE_SIZE, 8,  //指定B大小
                EGL14.EGL_ALPHA_SIZE, 8, //指定Alpha大小，以上四项实际上指定了像素格式
                EGL14.EGL_DEPTH_SIZE, 16, //指定深度缓存(Z Buffer)大小
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, //指定渲染api类别
                EGL14.EGL_NONE  //总是以EGL10.EGL_NONE结尾
        };

        //获取满足attr的config
        int[] configNum = new int[1];//满足attr的config数量有多少个
        EGLConfig[] configs = new EGLConfig[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay,
                attrs, 0,
                configs, 0, configs.length,
                configNum, 0)) {
            checkError("eglChooseConfig");
        }

        mEGLConfig = configs[0];

        printConfig(mEGLDisplay, mEGLConfig);

        //创建egl上下文
        int contextAttrs[] = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE,};

        mEGLContext = EGL14.EGL_NO_CONTEXT;//默认NO_CONTEXT，但是可以共享context达到共享数据
        EGLContext context = EGL14.eglCreateContext(mEGLDisplay, mEGLConfig, mEGLContext, contextAttrs, 0);
        checkError("eglCreateContext");
        mEGLContext = context;
    }

    /**
     * 设置用于展示的显存窗口
     * <p>
     * support:
     * <ul>
     * <li>SurfaceView</li>
     * <li>SurfaceHolder</li>
     * <li>Surface</li>
     * <li>SurfaceTexture</li>
     * </ul>
     *
     * @param surface
     */
    public void setSurface(Object surface) {
        if (surface != null && !(surface instanceof SurfaceView) &&
                !(surface instanceof SurfaceHolder) &&
                !(surface instanceof Surface) &&
                !(surface instanceof SurfaceTexture)) {
            throw new RuntimeException("Unsupported Surface:" + surface.getClass().getSimpleName());
        }

        mSurface = surface;
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void createSurface() {
        checkThread();

        realDestroySurface();

        int[] surfaceAttr;
        try {
            if (mSurfaceType == SURFACE_TYPE_WINDOW) {
                surfaceAttr = new int[]{EGL14.EGL_NONE};
                mEglSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, mSurface, surfaceAttr, 0);
            } else {
                surfaceAttr = new int[]{
                        EGL14.EGL_WIDTH, mWidth,
                        EGL14.EGL_HEIGHT, mHeight,
                        EGL14.EGL_NONE};
                mEglSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, surfaceAttr, 0);
            }

            if (mEglSurface == EGL14.EGL_NO_SURFACE) {
                checkError("createSurface");
            }

            EGL14.eglMakeCurrent(mEGLDisplay, mEglSurface, mEglSurface, mEGLContext);
            checkError("eglMakeCurrent");

        } catch (Exception e) {
            LogUtils.e(e);
            mEglSurface = null;
        }
    }

    public void swap() {
        EGL14.eglSwapBuffers(mEGLDisplay, mEglSurface);
        checkError("eglSwapBuffers");
    }

    public void release() {
        checkThread();
        realDestroySurface();
        EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
        EGL14.eglTerminate(mEGLDisplay);
        checkError("eglDestroyContext");
        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
    }

    public void destroySurface() {
        checkThread();
        realDestroySurface();
    }

    private void realDestroySurface() {
        if (mEglSurface != null && mEglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);

            EGL14.eglDestroySurface(mEGLDisplay, mEglSurface);

            mEglSurface = null;
        }
    }

    private void printConfig(EGLDisplay display, EGLConfig config) {
        if (!isPrintInfo) {
            return;
        }

        int value = 0;

        value = findConfigAttr(display, config, EGL14.EGL_RED_SIZE, -1);
        LogUtils.i("config: EGL_RED_SIZE: " + value);

        value = findConfigAttr(display, config, EGL14.EGL_GREEN_SIZE, -1);
        LogUtils.i("config: EGL_GREEN_SIZE: " + value);

        value = findConfigAttr(display, config, EGL14.EGL_BLUE_SIZE, -1);
        LogUtils.i("config: EGL_BLUE_SIZE: " + value);

        value = findConfigAttr(display, config, EGL14.EGL_ALPHA_SIZE, -1);
        LogUtils.i("config: EGL_ALPHA_SIZE: " + value);

        value = findConfigAttr(display, config, EGL14.EGL_DEPTH_SIZE, -1);
        LogUtils.i("config: EGL_DEPTH_SIZE: " + value);

        value = findConfigAttr(display, config, EGL14.EGL_RENDERABLE_TYPE, -1);
        LogUtils.i("config: EGL_RENDERABL_TYPE: " + value);

        value = findConfigAttr(display, config, EGL14.EGL_SAMPLE_BUFFERS, -1);
        LogUtils.i("config: EGL_SAMPLE_BUFFERS: " + value);

        value = findConfigAttr(display, config, EGL14.EGL_SAMPLES, -1);
        LogUtils.i("config: EGL_SAMPLES: " + value);

        value = findConfigAttr(display, config, EGL14.EGL_STENCIL_SIZE, -1);
        LogUtils.i("config: EGL_STENCIL_SIZE: " + value);

    }

    private int findConfigAttr(EGLDisplay display, EGLConfig config, int attr, int defaultValue) {
        int[] value = new int[1];
        if (EGL14.eglGetConfigAttrib(display, config, attr, value, 0)) {
            return value[0];
        }
        return defaultValue;
    }

    private void checkError(String tag) {
        int error = EGL14.eglGetError();
        if (error != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(tag + " error:" + EGL14.eglGetError());
        }
    }

    private void checkThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("Current thread can't be main thread");
        }
    }
}