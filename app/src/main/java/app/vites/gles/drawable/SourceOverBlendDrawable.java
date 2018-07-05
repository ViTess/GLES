package app.vites.gles.drawable;

import android.graphics.Bitmap;
import android.opengl.GLUtils;

import java.nio.FloatBuffer;

import app.vites.gles.GlesUtil;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE3;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * 两个纹理融合
 * 效果：第二个纹理盖在第一个纹理之上
 * <p>
 * Created by trs on 18-7-2.
 */
public class SourceOverBlendDrawable extends BitmapDrawable {

    private int mATexCoord2Loc;
    private int mUTexSampler2Loc;

    private final int[] mTexture2Id = new int[]{NO_TEXTURE};
    private final int[] mTexCoord2VBOId = new int[1];
    private FloatBuffer mTexCoord2Data;

    public SourceOverBlendDrawable() {
        this(false);
    }

    public SourceOverBlendDrawable(boolean isUsedToSdk) {
        super(isUsedToSdk);
        //因为两个纹理大小是一致的，所以重用第一个texture的坐标
        mTexCoord2Data = GlesUtil.createFloatBuffer(mTexCoord);
    }

    @Override
    protected void onInit() {
        super.onInit();

        GlesUtil.createDynamicVBO(mTexCoord2VBOId, mTexCoord2Data);

        mATexCoord2Loc = glGetAttribLocation(mProgramId, "aTexCoord2");
        GlesUtil.checkError("glGetAttribLocation");
        glEnableVertexAttribArray(mATexCoord2Loc);

        mUTexSampler2Loc = glGetUniformLocation(mProgramId, "uTexSampler");
        GlesUtil.checkError("glGetUniformLocation");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        glDeleteTextures(1, mTexture2Id, 0);
        mTexture2Id[0] = NO_TEXTURE;
    }

    @Override
    protected String getVertexShaderCode() {
        return "attribute vec4 aPosition;\n" +
                "attribute vec4 aTexCoord;\n" + //第一个纹理的坐标
                "attribute vec4 aTexCoord2;\n" + //第二个纹理的坐标
                "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uTexMatrix;\n" +
                "varying vec2 vTexCoord;\n" +
                "varying vec2 vTexCoord2;\n" +
                "void main(){\n" +
                "    vTexCoord = (uTexMatrix * aTexCoord).xy;\n" +
                "    vTexCoord2 = (uTexMatrix * aTexCoord2).xy;\n" +
                "    gl_Position = uMVPMatrix * aPosition;\n" +
                "}";
    }

    @Override
    protected String getFragmentShaderCode() {
        return "precision mediump float;\n" +
                "uniform sampler2D uTexSampler;\n" + //第一个纹理
                "uniform sampler2D uTexSampler2;\n" + //第二个纹理
                "uniform float uColorAlpha;\n" +
                "varying vec2 vTexCoord;\n" +
                "varying vec2 vTexCoord2;\n" +
                "void main() {\n" +
                "    lowp vec4 t1Color = texture2D(uTexSampler, vTexCoord);\n" +
                "    lowp vec4 t2Color = texture2D(uTexSampler2, vTexCoord2);\n" +
                "    gl_FragColor = mix(t2Color, t1Color, t1Color.a) * uColorAlpha;\n" +
                "}";
    }

    @Override
    protected void onDraw() {
        {
            glEnableVertexAttribArray(mATexCoord2Loc);
            GlesUtil.checkError("glEnableVertexAttribArray");

            //使用vbo上传纹理坐标到aTexCoord2
            glBindBuffer(GL_ARRAY_BUFFER, mTexCoord2VBOId[0]);
            glVertexAttribPointer(mATexCoord2Loc, 2, GL_FLOAT, false, POSITION_ATTR_LEN * GlesUtil.FLOAT_SIZE, 0);
            GlesUtil.checkError("glVertexAttribPointer");

            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        if (mTexture2Id[0] != NO_TEXTURE) {
            glActiveTexture(GL_TEXTURE3);
            glBindTexture(GL_TEXTURE_2D, mTexture2Id[0]);
            glUniform1i(mUTexSampler2Loc, 3);
        }
    }

    public void setSecondBitmap(Bitmap bitmap) {
        setSecondBitmap(bitmap, true);
    }

    public void setSecondBitmap(Bitmap bitmap, boolean isRecycle) {
        addTask(() -> {
            if (bitmap == null || bitmap.isRecycled()) {
                return;
            }

            if (mTexture2Id[0] == NO_TEXTURE) {
                glActiveTexture(GL_TEXTURE3);
                GlesUtil.createTextureObject(mTexture2Id, mOriginOutputWidth, mOriginOutputHeight);
            }

            glBindTexture(GL_TEXTURE_2D, mTexture2Id[0]);
            GlesUtil.checkError("glBindTexture");

//            GLUtils.texSubImage2D(GL_TEXTURE_2D, 0, 0, 0, bitmap);
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

            glBindTexture(GL_TEXTURE_2D, 0);
            GlesUtil.checkError("glBindTexture");

            if (isRecycle)
                bitmap.recycle();
        });
    }

    public void clearSecondBitmap() {
        addTask(() -> {
            if (mTexture2Id[0] != NO_TEXTURE) {
                glDeleteTextures(1, mTexture2Id, 0);
                mTexture2Id[0] = NO_TEXTURE;
            }
        });
    }
}
