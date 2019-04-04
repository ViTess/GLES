package app.vites.gles.drawable;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

import app.vites.gles.GlesUtil;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * Created by trs on 19-3-29.
 */
public class CameraDrawable extends TextureDrawable {

    private SurfaceTexture mSurfaceTexture;

    @Override
    protected String getFragmentShaderCode() {
        return "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "uniform samplerExternalOES uTexSampler;\n" +
                "uniform float uColorAlpha;\n" +
                "varying vec2 vTexCoord;\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D(uTexSampler,vTexCoord) * uColorAlpha;\n" +
                "}";
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @Override
    protected void onInit() {

        super.onInit();

        if (mTextureId[0] == NO_TEXTURE) {
            GlesUtil.createOESTextureObject(mTextureId);
        }

        if (mSurfaceTexture == null) {
            mSurfaceTexture = new SurfaceTexture(mTextureId[0]);
        }

        isDeleteTex = true;
    }

    @Override
    public void draw() {
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mTexMatrix);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

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
            GlesUtil.checkError("glActiveTexture");

            glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureId[0]);//绑定纹理
            GlesUtil.checkError("glBindTexture");

            glUniform1i(mUTexSamplerLoc, 0);
            GlesUtil.checkError("glUniform1i");
        }

        onDraw();

        //draw
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        GlesUtil.checkError("glDrawArrays");

        //解绑纹理
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
        glUseProgram(0);
    }

    @Override
    protected void onDestroy() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
    }
}
