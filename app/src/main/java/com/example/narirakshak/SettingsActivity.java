package com.example.narirakshak;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchCamera, switchLocation;
    private SharedPreferences sharedPreferences;
    private RadioGroup rgLanguage;
    private RadioButton rbEnglish, rbHindi;
    private boolean isInitialCheck = true; // Language auto-trigger check loop se bachne ke liye

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        switchCamera   = findViewById(R.id.switchCamera);
        switchLocation = findViewById(R.id.switchLocation);
        rgLanguage     = findViewById(R.id.rgLanguage);
        rbEnglish      = findViewById(R.id.rbEnglish);
        rbHindi        = findViewById(R.id.rbHindi);

        sharedPreferences = getSharedPreferences("SOS_SETTINGS", MODE_PRIVATE);

        // ✅ Switches ki initial state set karein
        switchCamera.setChecked(sharedPreferences.getBoolean("camera_on", false));
        switchLocation.setChecked(sharedPreferences.getBoolean("location_on", true));

        // ✅ Standard listener optimized context ke sath
        switchCamera.setOnCheckedChangeListener((buttonView, isChecked) ->
                sharedPreferences.edit().putBoolean("camera_on", isChecked).apply());

        switchLocation.setOnCheckedChangeListener((buttonView, isChecked) ->
                sharedPreferences.edit().putBoolean("location_on", isChecked).apply());

        // ✅ Current language detect karke checkbox select karein
        String currentLang = LocaleHelper.getSavedLanguage(this);
        if (currentLang.equals(LocaleHelper.LANG_HINDI)) {
            rbHindi.setChecked(true);
        } else {
            rbEnglish.setChecked(true);
        }

        isInitialCheck = false; // Initial loading khatam

        // ✅ Language change handler
        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            if (isInitialCheck) return; // Loading ke waqt restart na ho jaye

            String selectedLang = (checkedId == R.id.rbHindi)
                    ? LocaleHelper.LANG_HINDI
                    : LocaleHelper.LANG_ENGLISH;

            // Agar bhasha sach mein badli hai, tabhi aage badhein
            if (!selectedLang.equals(currentLang)) {

                // ✅ Language save karein
                LocaleHelper.saveLanguage(getApplicationContext(), selectedLang);

                // ✅ Strings se localized text uthakar Toast dikhayein
                String msg = getString(R.string.settings_language_changed);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

                // ✅ Smooth transition aur clean restart ke sath fresh reload
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .postDelayed(() -> {
                            Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finishAffinity();
                        }, 800);
            }
        });
    }
}