package app.vites.gles;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.support.annotation.IntDef;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_DYNAMIC_DRAW;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRUE;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glValidateProgram;

/**
 * Created by trs on 18-6-29.
 */
public final class GlesUtil {

    private static final String TAG = "GlesUtil";

    public static final int FLOAT_SIZE = 4;//一个浮点型数据占4位字节

    public static FloatBuffer createFloatBuffer(float[] coords) {
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * FLOAT_SIZE);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    public static void createStaticVBO(int[] vboId, FloatBuffer buffer) {
        createVBO(vboId, buffer, GL_STATIC_DRAW);
    }

    public static void createDynamicVBO(int[] vboId, FloatBuffer buffer) {
        createVBO(vboId, buffer, GL_DYNAMIC_DRAW);
    }

    public static void createVBO(int[] vboId, FloatBuffer buffer, int usage) {
        glGenBuffers(1, vboId, 0);
        checkError("glGenBuffers");

        glBindBuffer(GL_ARRAY_BUFFER, vboId[0]);//绑定vbo
        checkError("glBindBuffer");

        glBufferData(GL_ARRAY_BUFFER, buffer.capacity() * FLOAT_SIZE, buffer, usage);//为vbo申请空间并传递数据
        checkError("glBufferData");

        glBindBuffer(GL_ARRAY_BUFFER, 0);//解绑vbo
    }

    /**
     * @param tex
     * @param width
     * @param height
     */
    public static void createTextureObject(int[] tex, int width, int height) {
        final int type = GL_TEXTURE_2D;

        //生成一个纹理
        GLES20.glGenTextures(1, tex, 0);
        checkError("glGenTextures");

        //绑定纹理
        int textureId = tex[0];
        GLES20.glBindTexture(type, textureId);
        checkError("glBindTexture " + textureId);

        //生成一个2d纹理
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        //设置纹理过滤参数
        //指定纹理缩小效果,GL_NEAREST 算法是最近邻法
        GLES20.glTexParameterf(type, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);

        //指定纹理放大效果
        GLES20.glTexParameterf(type, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        //指定水平方向纹理的包裹方式,GL_CLAMP_TO_EDGE 超出范围的纹理坐标则截断

        GLES20.glTexParameterf(type, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //指定垂直方向纹理的包裹方式
        GLES20.glTexParameterf(type, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        checkError("glTexParameter");

        GLES20.glBindTexture(type, 0);
    }

    /**
     * 创建外部纹理
     *
     * @return 纹理对应的id
     */
    public static void createOESTextureObject(int[] tex) {
        final int OESType = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

        //生成一个纹理
        GLES20.glGenTextures(1, tex, 0);
        checkError("glGenTextures");

        //将此纹理绑定到外部纹理上
        int textureId = tex[0];
        GLES20.glBindTexture(OESType, textureId);

        checkError("glBindTexture " + textureId);

        //设置纹理过滤参数
        //指定纹理缩小效果,GL_NEAREST 算法是最近邻法
        GLES20.glTexParameterf(OESType, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);

        //指定纹理放大效果,GL_LINEAR 采用双线性插值算法
        GLES20.glTexParameterf(OESType, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        //指定水平方向纹理的包裹方式,GL_CLAMP_TO_EDGE 超出范围的纹理坐标则截断
        GLES20.glTexParameterf(OESType, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);

        //指定垂直方向纹理的包裹方式
        GLES20.glTexParameterf(OESType, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        checkError("glTexParameter");
    }

    public static String getShaderCode(Context context, int resId) {
        final StringBuilder builder = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(resId)));
        String line = null;

        try {
            while ((line = br.readLine()) != null) {
                builder.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return builder.toString();
    }

    public static int compileVertexShader(String shaderCode) {
        return compileShader(GL_VERTEX_SHADER, shaderCode);
    }

    public static int compileFragmentShader(String shaderCode) {
        return compileShader(GL_FRAGMENT_SHADER, shaderCode);
    }

    private static int compileShader(int type, String shaderCode) {
        final int shaderObjId = glCreateShader(type);//创建一个Shader对象

        if (shaderObjId == 0) {
            throwError("create " + getShaderType(type) + " failed!");
        }

        glShaderSource(shaderObjId, shaderCode);//传入源代码，与Shader对象关联起来
        glCompileShader(shaderObjId);//编译着色器到这个Shader对象

        if (!isCompileShaderSuccess(shaderObjId)) {
            glDeleteShader(shaderObjId);//如果编译失败则删除该Shader对象
            throwError("compile " + getShaderType(type) + ":" + shaderObjId + " error!\nInfo:" + glGetShaderInfoLog(shaderObjId));
        }

        return shaderObjId;
    }

    private static boolean isCompileShaderSuccess(int shaderObjId) {
        final int[] compileStatus = new int[1];
        //获取编译状态，并将状态值输入到compileStatus的0位上
        glGetShaderiv(shaderObjId, GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == GL_TRUE)
            return true;
        return false;
    }

    public static int linkProgram(String vertexShaderCode, String fragmentShaderCode) {
        if (TextUtils.isEmpty(vertexShaderCode)) {
            throwError("vertex shader is empty!");
        }
        if (TextUtils.isEmpty(fragmentShaderCode)) {
            throwError("fragment shader is empty!");
        }

        int vertexShaderObjId = compileVertexShader(vertexShaderCode);
        int fragmentShaderObjId = compileFragmentShader(fragmentShaderCode);

        return linkProgram(vertexShaderObjId, fragmentShaderObjId);
    }

    public static int linkProgram(int vertexShaderObjId, int fragmentShaderObjId) {
        final int programObjId = glCreateProgram();//创建一个program对象
        if (programObjId == 0) {
            throwError("create program failed!");
        }

        //设置shader到program
        glAttachShader(programObjId, vertexShaderObjId);
        glAttachShader(programObjId, fragmentShaderObjId);

        //链接program
        glLinkProgram(programObjId);

        if (!isLinkProgramSuccess(programObjId)) {
            glDeleteProgram(programObjId);
            throwError("link program error!\nInfo:" + glGetProgramInfoLog(programObjId));
        }

        return programObjId;
    }

    /**
     * 验证program
     *
     * @param programObjId
     * @return 是否成功验证
     */
    public static boolean validateProgram(int programObjId) {
        glValidateProgram(programObjId);

        final int[] validateStatus = new int[1];
        glGetProgramiv(programObjId, GL_VALIDATE_STATUS, validateStatus, 0);
        Log.i(TAG, "Validate program result:" + validateStatus[0] + "\nInfo:" + glGetProgramInfoLog(programObjId));
        return validateStatus[0] != 0;
    }

    private static boolean isLinkProgramSuccess(int programObjId) {
        final int[] linkStatus = new int[1];
        glGetProgramiv(programObjId, GL_LINK_STATUS, linkStatus, 0);

        if (linkStatus[0] == GL_TRUE)
            return true;
        return false;
    }

    private static String getShaderType(int type) {
        return type == GL_VERTEX_SHADER ? "vertex shader" : "fragment shader";
    }

    /**
     * 检查是否出错
     *
     * @param tag
     */
    public static void checkError(String tag) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            throwError(tag + " - OPENGL ERROR: 0x" + Integer.toHexString(error));
        }
    }

    private static void throwError(String msg) {
        Log.e(TAG, msg);
        throw new RuntimeException(msg);
    }
}
