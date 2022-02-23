package android.app;

import android.util.Log;

public class InjectDex implements ITGDex {
    private String TAG="mikrom";
    @Override
    public void onStart() {
        Log.e(TAG,"InjectDex.onStart");
    }
}