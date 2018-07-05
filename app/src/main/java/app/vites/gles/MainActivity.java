package app.vites.gles;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import vite.testgles.R;

public class MainActivity extends AppCompatActivity {

    TestGlSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        glSurfaceView = (TestGlSurfaceView) findViewById(R.id.surfaceview);
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
    }
}
