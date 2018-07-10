package app.vites.gles.drawable;

import app.vites.gles.GlesUtil;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_DST_ALPHA;
import static android.opengl.GLES20.GL_DST_COLOR;
import static android.opengl.GLES20.GL_NICEST;
import static android.opengl.GLES20.GL_ONE_MINUS_CONSTANT_ALPHA;
import static android.opengl.GLES20.GL_ONE_MINUS_CONSTANT_COLOR;
import static android.opengl.GLES20.GL_ONE_MINUS_DST_ALPHA;
import static android.opengl.GLES20.GL_ONE_MINUS_DST_COLOR;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.GL_SRC_COLOR;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glHint;

/**
 * Created by trs on 18-7-10.
 */
public class BlendDrawable extends BitmapDrawable {
    @Override
    protected void onDraw() {

        //一般用glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)，这个和shader混合中的mix方法算法相同
        //但是有缺陷，如果要混合的顶层图像存在透明度在0~1之间的像素值，该像素值混合后的结果为黑色，所以一些图像会产生黑边
        //所以用glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)，能消除黑边

        //通过glBlendFunc设置源因子和目标因子
        //假设源数据为(R1, G1, B1, A1)，目标数据为(R2, G2, B2, A2)
        //源因子数据为(Sr, Sg, Sb, Sa)，目标因子数据为(Dr, Dg, Db, Da)
        //混合后的数据为(R, G, B, A)，则结果为：
        //R = R1 * Sr + R2 * Dr
        //G = G1 * Sg + G2 * Dg
        //B = B1 * Sb + B2 * Db
        //A = A1 * Sa + A2 * Da

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }
}
