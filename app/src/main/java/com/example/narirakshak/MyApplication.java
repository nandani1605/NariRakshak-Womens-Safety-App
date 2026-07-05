package com.example.narirakshak;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLanguage(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // ✅ System language change hone par bhi humari language override karo
        LocaleHelper.setLocale(getBaseContext(),
                LocaleHelper.getSavedLanguage(getApplicationContext()));
    }
}