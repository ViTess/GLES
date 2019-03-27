package app.vites.gles.drawable;

import java.util.LinkedList;
import java.util.List;

import app.vites.gles.GlesUtil;
import app.vites.gles.IDrawable;

import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glDeleteFramebuffers;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;

/**
 * Created by trs on 18-7-2.
 */
public final class FBODrawable extends TextureDrawable {

    private final int[] mFBOId = new int[]{NO_FRAMEBUFFER};

    private final List<IDrawable> mDrawableList = new LinkedList<>();

    @Override
    protected void onInit() {
        super.onInit();

        for (IDrawable drawable : mDrawableList) {
            if (drawable != null) {
                drawable.init();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (IDrawable drawable : mDrawableList) {
            if (drawable != null)
                drawable.destroy();
        }
        removeAllDrawable();

        //删除FBO对象
        if (mFBOId[0] != NO_FRAMEBUFFER) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteFramebuffers(1, mFBOId, 0);
            GlesUtil.checkError("glDeleteFramebuffers");
            mFBOId[0] = NO_FRAMEBUFFER;
        }

        //删除texture
        if (mTextureId[0] != NO_TEXTURE) {
            glDeleteTextures(1, mTextureId, 0);
            mTextureId[0] = NO_TEXTURE;
        }
    }

    @Override
    public void setOutputSize(int outputWidth, int outputHeight) {
        if (mOutputWidth != outputWidth || mOutputHeight != outputHeight) {
            mOutputWidth = outputWidth;
            mOutputHeight = outputHeight;

            //删除FBO对象
            if (mFBOId[0] != NO_FRAMEBUFFER) {
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                glDeleteFramebuffers(1, mFBOId, 0);
                GlesUtil.checkError("glDeleteFramebuffers");
                mFBOId[0] = NO_FRAMEBUFFER;
            }

            //删除texture
            if (mTextureId[0] != NO_TEXTURE) {
                glDeleteTextures(1, mTextureId, 0);
                mTextureId[0] = 0;
            }

            GlesUtil.createTextureObject(mTextureId, mOutputWidth, mOutputHeight);

            glGenFramebuffers(1, mFBOId, 0);
            GlesUtil.checkError("glGenFramebuffers");

            glBindFramebuffer(GL_FRAMEBUFFER, mFBOId[0]);
            GlesUtil.checkError("glBindFramebuffer");

            //TODO:目前考虑的都是2d层面的opengl操作，所以frameBuffer只需要颜色缓存(即绑定Texture)即可，如果涉及到3d，需要用到深度缓存(即RenderBuffer)
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mTextureId[0], 0);
            GlesUtil.checkError("glFramebufferTexture2D");

            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }

        for (IDrawable drawable : mDrawableList) {
            if (drawable != null) {
                drawable.setOutputSize(mOutputWidth, mOutputHeight);
            }
        }
    }

    @Override
    public void draw() {
        if (mFBOId[0] != NO_FRAMEBUFFER) {
            //绑定fbo
            glBindFramebuffer(GL_FRAMEBUFFER, mFBOId[0]);
            GlesUtil.checkError("glBindFramebuffer");

            glClear(GL_COLOR_BUFFER_BIT);

            for (IDrawable drawable : mDrawableList) {
                if (drawable != null) {
                    drawable.draw();
                }
            }

            //解绑fbo
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }

        super.draw();
    }

    public void addDrawable(IDrawable drawable) {
        addTask(() -> {
            if (drawable != null) {
                drawable.init();
                drawable.setOutputSize(mOutputWidth, mOutputHeight);
            }
        });
        mDrawableList.add(drawable);
    }

    public void removeDrawable(IDrawable drawable) {
        addTask(() -> {
            if (drawable != null) {
                drawable.destroy();
            }
        });
        mDrawableList.remove(drawable);
    }

    public void removeAllDrawable() {
        mDrawableList.clear();
    }

    public int getTextureId() {
        return mTextureId[0];
    }
}
