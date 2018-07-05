package app.vites.gles;

/**
 * Created by trs on 18-6-29.
 */
public interface IDrawable {

    int NO_TEXTURE = -1;
    int NO_FRAMEBUFFER = -1;

    enum ScaleType {
        FIT_XY, CENTER_CROP, CENTER_INSIDE
    }

    void init();

    void destroy();

    void setOutputSize(int outputWidth, int outputHeight);

    void draw();
}
