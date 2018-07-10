package app.vites.gles.drawable;

import android.opengl.Matrix;
import android.support.annotation.FloatRange;

import java.nio.FloatBuffer;
import java.util.LinkedList;

import app.vites.gles.GlesUtil;
import app.vites.gles.IDrawable;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBufferSubData;
import static android.opengl.GLES20.glDeleteBuffers;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * 最简单的texture
 * <p>
 * Created by trs on 18-6-29.
 */
public class TextureDrawable implements IDrawable {

    protected final int POSITION_ATTR_LEN = 2;//数组中从0开始每2位为一个坐标属性

    private final LinkedList<Runnable> mTaskList = new LinkedList<>();//确保操作在GLThread中运行
    private float mWidthRatio = 1, mHeightRatio = 1;
    private int mInputWidth, mInputHeight;
    private float mColorAlpha = 1;

    protected boolean isInit = false;
    protected boolean isDeleteTex = false;
    protected int mProgramId;
    protected final int[] mTextureId = new int[]{NO_TEXTURE};
    protected int mOriginOutputWidth, mOriginOutputHeight;
    protected int mOutputWidth, mOutputHeight;
    protected FloatBuffer mVertexData;
    protected FloatBuffer mTexCoordData;
    protected final float[] mTexMatrix = new float[16];
    protected final float[] mMVPMatrix = new float[16];
    protected ScaleType mScaleType = ScaleType.FIT_XY;

    //vertex loc
    protected int mAPositionLoc;
    protected int mATexCoordLoc;
    protected int mUTexMatrixLoc;
    protected int mUMVPMatrixLoc;

    //fragment loc
    protected int mUTexSamplerLoc;
    protected int mUColorAlphaLoc;

    //vbo
    private final int[] mVertexVBOId = new int[1];
    private final int[] mTexCoordVBOId = new int[1];

    //原始的顶点坐标(中心点屏幕中央)
    protected final float[] mOriginVertexCoord = new float[]{
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,   // 1 bottom right
            -1.0f, 1.0f,   // 2 top left
            1.0f, 1.0f,   // 3 top right
    };

    //经修改的顶点坐标(中心点屏幕中央)
    protected final float[] mVertexCoord = new float[8];

    //纹理坐标(中心点在纹理右下角)
    protected final float[] mTexCoord = new float[]{
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };

    public TextureDrawable() {
        mVertexData = GlesUtil.createFloatBuffer(mOriginVertexCoord);
        mTexCoordData = GlesUtil.createFloatBuffer(mTexCoord);
    }

    @Override
    public final void init() {
        if (isInit)
            return;

        isInit = true;
        onInit();
    }

    protected void onInit() {
        mProgramId = GlesUtil.linkProgram(getVertexShaderCode(), getFragmentShaderCode());
        GlesUtil.validateProgram(mProgramId);

        GlesUtil.createDynamicVBO(mVertexVBOId, mVertexData);
        GlesUtil.createDynamicVBO(mTexCoordVBOId, mTexCoordData);

        //vertex loc
        {
            mAPositionLoc = glGetAttribLocation(mProgramId, "aPosition");
            GlesUtil.checkError("glGetAttribLocation");
            glEnableVertexAttribArray(mAPositionLoc);

            mATexCoordLoc = glGetAttribLocation(mProgramId, "aTexCoord");
            GlesUtil.checkError("glGetAttribLocation");
            glEnableVertexAttribArray(mATexCoordLoc);

            mUMVPMatrixLoc = glGetUniformLocation(mProgramId, "uMVPMatrix");
            GlesUtil.checkError("glGetUniformLocation");

            mUTexMatrixLoc = glGetUniformLocation(mProgramId, "uTexMatrix");
            GlesUtil.checkError("glGetUniformLocation");
        }

        //fragment loc
        {
            mUTexSamplerLoc = glGetUniformLocation(mProgramId, "uTexSampler");
            GlesUtil.checkError("glGetUniformLocation");

            mUColorAlphaLoc = glGetUniformLocation(mProgramId, "uColorAlpha");
            GlesUtil.checkError("glGetUniformLocation");
        }

        Matrix.setIdentityM(mTexMatrix, 0);
        Matrix.setIdentityM(mMVPMatrix, 0);
    }

    protected String getVertexShaderCode() {
        return "attribute vec4 aPosition;\n" +
                "attribute vec4 aTexCoord;\n" +
                "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uTexMatrix;\n" +
                "varying vec2 vTexCoord;\n" +
                "void main(){\n" +
                "    vTexCoord = (uTexMatrix * aTexCoord).xy;\n" +
                "    gl_Position = uMVPMatrix * aPosition;\n" +
                "}";
    }

    protected String getFragmentShaderCode() {
        return "precision mediump float;\n" +
                "uniform sampler2D uTexSampler;\n" +
                "uniform float uColorAlpha;\n" +
                "varying vec2 vTexCoord;\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D(uTexSampler,vTexCoord) * uColorAlpha;\n" +
                "}";
    }

    protected void onDestroy() {

    }

    @Override
    public final void destroy() {
        if (!isInit)
            return;

        isInit = false;
        onDestroy();

        if (isDeleteTex) {
            glDeleteTextures(1, mTextureId, 0);
            mTextureId[0] = 0;
        }

        //删除vbo
        {
            glDeleteBuffers(1, mVertexVBOId, 0);
            GlesUtil.checkError("glDeleteBuffers");
            mVertexVBOId[0] = 0;
            mVertexData.clear();

            glDeleteBuffers(1, mTexCoordVBOId, 0);
            GlesUtil.checkError("glDeleteBuffers");
            mTexCoordVBOId[0] = 0;
            mTexCoordData.clear();
        }

        //删除program
        glDeleteProgram(mProgramId);
        GlesUtil.checkError("glDeleteProgram");
        mProgramId = 0;
    }

    @Override
    public void setOutputSize(int outputWidth, int outputHeight) {
        mOutputWidth = mOriginOutputWidth = outputWidth;
        mOutputHeight = mOriginOutputHeight = outputHeight;
        setupScaleType();
    }

    public final int getOutputWidth() {
        return mOutputWidth;
    }

    public final int getOutputHeight() {
        return mOutputHeight;
    }

    @Override
    public void draw() {
        glUseProgram(mProgramId);
        GlesUtil.checkError("glUseProgram");

        runTasks();

        //上传mvp矩阵
        glUniformMatrix4fv(mUMVPMatrixLoc, 1, false, mMVPMatrix, 0);
        GlesUtil.checkError("glUniformMatrix4fv");

        //上传纹理矩阵
        glUniformMatrix4fv(mUTexMatrixLoc, 1, false, mTexMatrix, 0);
        GlesUtil.checkError("glUniformMatrix4fv");

        glUniform1f(mUColorAlphaLoc, mColorAlpha);
        GlesUtil.checkError("glUniform1f");

        {//使用vbo上传顶点坐标到"aPosition"
            glEnableVertexAttribArray(mAPositionLoc);
            GlesUtil.checkError("glEnableVertexAttribArray");

            glBindBuffer(GL_ARRAY_BUFFER, mVertexVBOId[0]);
            glVertexAttribPointer(mAPositionLoc, 2, GL_FLOAT, false, POSITION_ATTR_LEN * GlesUtil.FLOAT_SIZE, 0);
            GlesUtil.checkError("glVertexAttribPointer");

            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        {//使用vbo上传纹理坐标到"aTexCoord"
            glEnableVertexAttribArray(mATexCoordLoc);
            GlesUtil.checkError("glEnableVertexAttribArray");

            glBindBuffer(GL_ARRAY_BUFFER, mTexCoordVBOId[0]);
            glVertexAttribPointer(mATexCoordLoc, 2, GL_FLOAT, false, POSITION_ATTR_LEN * GlesUtil.FLOAT_SIZE, 0);
            GlesUtil.checkError("glVertexAttribPointer");

            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        if (mTextureId[0] != NO_TEXTURE) {
            glActiveTexture(GL_TEXTURE0);//激活纹理单元
            glBindTexture(GL_TEXTURE_2D, mTextureId[0]);//绑定纹理
            glUniform1i(mUTexSamplerLoc, 0);
        }

        onDraw();

        //draw
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        GlesUtil.checkError("glDrawArrays");

        //解绑纹理
        glBindTexture(GL_TEXTURE_2D, 0);
        glUseProgram(0);
    }

    /**
     * call it before glDrawArrays
     */
    protected void onDraw() {
        //关闭混合
        glDisable(GL_BLEND);
    }

    private void runTasks() {
        while (!mTaskList.isEmpty()) {
            mTaskList.removeFirst().run();
        }
    }

    protected void addTask(Runnable runnable) {
//        if (Thread.currentThread().getName().startsWith("GLThread"))
//            runnable.run();
//        else
        mTaskList.add(runnable);
    }

    /**
     * @param textureId
     * @param isDelete
     */
    public void setTextureId(int textureId, boolean isDelete) {
        isDeleteTex = isDelete;
        addTask(() -> {
            if (isDeleteTex && mTextureId[0] != NO_TEXTURE && mTextureId[0] != textureId) {
                glDeleteTextures(1, mTextureId, 0);
                GlesUtil.checkError("glDeleteTextures");
            }
            mTextureId[0] = textureId;
        });
    }

    public int getTextureId() {
        return mTextureId[0];
    }

    public void setColorAlpha(@FloatRange(from = 0.0, to = 1.0) float alpha) {
        mColorAlpha = alpha;
    }

    public void translate(float x, float y) {
        Matrix.translateM(mMVPMatrix, 0, x, y, 1);
    }

    public void scale(float x, float y) {
        Matrix.scaleM(mMVPMatrix, 0, x, y, 1);
    }

    public void resetMVPMatrix() {
        Matrix.setIdentityM(mMVPMatrix, 0);
    }

    public void setInputSize(int inputWidth, int inputHeight) {
        mInputWidth = inputWidth;
        mInputHeight = inputHeight;
        setScaleType(mScaleType);
    }

    public void setScaleType(ScaleType scaleType) {
        mScaleType = scaleType;
        setupScaleType();
        addTask(() -> {
            mVertexData.clear();
            mVertexData.put(mVertexCoord);
            mVertexData.position(0);

            glBindBuffer(GL_ARRAY_BUFFER, mVertexVBOId[0]);//绑定vbo
            GlesUtil.checkError("glBindBuffer");

            glBufferSubData(GL_ARRAY_BUFFER, 0, mVertexData.capacity() * GlesUtil.FLOAT_SIZE, mVertexData);
            GlesUtil.checkError("glBufferSubData");

            glBindBuffer(GL_ARRAY_BUFFER, 0);//解绑vbo
        });
    }

    private void setupScaleType() {
        if (mInputWidth == 0 || mInputHeight == 0)
            return;

        mWidthRatio = 1;
        mHeightRatio = 1;

        System.arraycopy(mOriginVertexCoord, 0, mVertexCoord, 0, mVertexCoord.length);

        float inputAspect = mInputWidth / (float) mInputHeight;
        float outputAspect = mOriginOutputWidth / (float) mOriginOutputHeight;

        if (mScaleType == ScaleType.CENTER_CROP) {
            if (inputAspect < outputAspect) {
                mHeightRatio = outputAspect / inputAspect;
                mVertexCoord[1] *= mHeightRatio;
                mVertexCoord[3] *= mHeightRatio;
                mVertexCoord[5] *= mHeightRatio;
                mVertexCoord[7] *= mHeightRatio;

                mOutputHeight *= mHeightRatio;
            } else {
                mWidthRatio = inputAspect / outputAspect;
                mVertexCoord[0] *= mWidthRatio;
                mVertexCoord[2] *= mWidthRatio;
                mVertexCoord[4] *= mWidthRatio;
                mVertexCoord[6] *= mWidthRatio;

                mOutputWidth *= mWidthRatio;
            }
        } else if (mScaleType == ScaleType.CENTER_INSIDE) {
            if (inputAspect < outputAspect) {
                mWidthRatio = inputAspect / outputAspect;
                mVertexCoord[0] *= mWidthRatio;
                mVertexCoord[2] *= mWidthRatio;
                mVertexCoord[4] *= mWidthRatio;
                mVertexCoord[6] *= mWidthRatio;

                mOutputWidth *= mWidthRatio;
            } else {
                mHeightRatio = outputAspect / inputAspect;
                mVertexCoord[1] *= mHeightRatio;
                mVertexCoord[3] *= mHeightRatio;
                mVertexCoord[5] *= mHeightRatio;
                mVertexCoord[7] *= mHeightRatio;

                mOutputHeight *= mHeightRatio;
            }
        }
    }
}