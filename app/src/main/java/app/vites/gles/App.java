package app.vites.gles;

import android.app.Application;
import android.content.Context;

/**
 * Created by trs on 19-3-28.
 */
public class App extends Application {

    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
    }

    public static Context getContext() {
        return sContext;
    }
}
