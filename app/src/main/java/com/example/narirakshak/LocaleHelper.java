package com.example.narirakshak;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS_NAME = "NariRakshakPrefs";
    private static final String KEY_LANGUAGE = "app_language";
    public static final String LANG_ENGLISH = "en";
    public static final String LANG_HINDI = "hi";

    // ✅ Context directly use karo — getApplicationContext() mat karo
    public static String getSavedLanguage(Context context) {
        String lang = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, LANG_ENGLISH);
        android.util.Log.d("LOCALE_DEBUG", "Saved language: " + lang);
        return lang;
    }

    public static void saveLanguage(Context context, String language) {
        // Save karne ke liye applicationContext safe hai
        Context ctx = context.getApplicationContext();
        if (ctx == null) ctx = context;
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, language)
                .commit();
    }

    public static Context applyLanguage(Context context) {
        String language = getSavedLanguage(context);
        return setLocale(context, language);
    }

    public static Context setLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
        }

        // ✅ Dono karo saath mein
        context.getResources().updateConfiguration(config,
                context.getResources().getDisplayMetrics());
        return context.createConfigurationContext(config);
    }
}