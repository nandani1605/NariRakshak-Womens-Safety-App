package com.example.narirakshak;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 1. Logo wale ImageView ko dhoondho
        ImageView ivSplashLogo = findViewById(R.id.ivSplashLogo);

        // 2. Animation file load karo (res/anim/fade_zoom_in.xml)
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_zoom_in);

        // 3. Logo par animation shuru karo
        ivSplashLogo.startAnimation(animation);

        // Timer wahi 2 second rahega
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, 2000);
    }
}