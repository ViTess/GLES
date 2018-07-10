package app.vites.gles.drawable;

import android.graphics.Bitmap;
import android.opengl.GLUtils;

import app.vites.gles.GlesUtil;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDisable;

/**
 * 最简单的绘制图片的texture
 * <p>
 * Created by trs on 18-6-29.
 */
public class BitmapDrawable extends TextureDrawable {

    public BitmapDrawable() {
        this(false);
    }

    /**
     * @param isUsedToSdk 是否用于腾讯sdk
     */
    public BitmapDrawable(boolean isUsedToSdk) {
        if (!isUsedToSdk) {
            //腾讯的texture可能已经处理过上下颠倒，所以这边要把纹理坐标处理一下
            mTexCoord[1] += 1;
            mTexCoord[3] += 1;
            mTexCoord[5] -= 1;
            mTexCoord[7] -= 1;
        }
        mVertexData = GlesUtil.createFloatBuffer(mOriginVertexCoord);
        mTexCoordData = GlesUtil.createFloatBuffer(mTexCoord);
    }

    public void setBitmap(Bitmap bitmap) {
        setBitmap(bitmap, true);
    }

    public void setBitmap(Bitmap bitmap, boolean isRecycle) {
        addTask(() -> {
            if (bitmap == null || bitmap.isRecycled()) {
                return;
            }

            if (mTextureId[0] == NO_TEXTURE)
                GlesUtil.createTextureObject(mTextureId, mOriginOutputWidth, mOriginOutputHeight);

            glBindTexture(GL_TEXTURE_2D, mTextureId[0]);
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
            glBindTexture(GL_TEXTURE_2D, 0);

            if (isRecycle)
                bitmap.recycle();
        });
    }

    public void clearBitmap() {
        addTask(() -> {
            if (isDeleteTex && mTextureId[0] != NO_TEXTURE) {
                glDeleteTextures(1, mTextureId, 0);
                mTextureId[0] = NO_TEXTURE;
            }
        });
    }
}