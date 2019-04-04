package app.vites.gles;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import app.vites.gles.drawable.BitmapDrawable;
import app.vites.gles.drawable.BlendDrawable;
import app.vites.gles.drawable.FBODrawable;

public class MainActivity extends AppCompatActivity {

    TestGlSurfaceView glSurfaceView;
    FBODrawable mFBODrawable;
    BitmapDrawable mBitmapDrawable;
    BlendDrawable mBlendDrawable;
    TestRenderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        glSurfaceView = (TestGlSurfaceView) findViewById(R.id.surfaceview);

        mBitmapDrawable = new BitmapDrawable();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.rua);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        mBitmapDrawable.setInputSize(width, height);
        mBitmapDrawable.setBitmap(bitmap);
        mBitmapDrawable.setScaleType(IDrawable.ScaleType.CENTER_INSIDE);
        mBitmapDrawable.setColorAlpha(1);

        mBlendDrawable = new BlendDrawable();
        mBlendDrawable.setInputSize(width, height);
        mBlendDrawable.setBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        mBlendDrawable.setScaleType(IDrawable.ScaleType.CENTER_INSIDE);
        mBlendDrawable.setColorAlpha(1);

        mFBODrawable = new FBODrawable();
        mFBODrawable.addDrawable(mBitmapDrawable);
        mFBODrawable.addDrawable(mBlendDrawable);

        mRenderer = new TestRenderer();
        mRenderer.setDrawable(mFBODrawable);
        glSurfaceView.setRenderer(mRenderer);

        View btn = findViewById(R.id.btn_camera);
        btn.setVisibility(View.VISIBLE);
        btn.setOnClickListener(v -> startActivity(new Intent(this, CameraActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRenderer.release();
    }
}
