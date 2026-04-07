package com.example.adapt;

import android.app.Application;

import com.example.adapt.utils.CrashRecoveryManager;

public class AdaptApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashRecoveryManager.install(this);
    }
}
