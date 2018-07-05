package app.vites.gles.drawable;

import android.graphics.Bitmap;

/**
 * 动态图像融合，即第二张图为动态获取的
 * Created by trs on 18-7-3.
 */
public class DynamicSourceOverBlendDrawable extends SourceOverBlendDrawable {
    public interface OnDynamicSourceOverListener {
        Bitmap getSecondBitmap();
    }

    public DynamicSourceOverBlendDrawable() {
        super();
    }

    public DynamicSourceOverBlendDrawable(boolean isUsedToSdk) {
        super(isUsedToSdk);
    }

    private OnDynamicSourceOverListener mListener;

    public void setOnDynamicSourceOverListener(OnDynamicSourceOverListener listener) {
        mListener = listener;
    }

    @Override
    protected void onDraw() {
        if (mListener != null) {
            setSecondBitmap(mListener.getSecondBitmap(), false);
        }
        super.onDraw();
    }
}
