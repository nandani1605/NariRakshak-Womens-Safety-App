package com.example.narirakshak;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.content.Context;

public class HelplinesActivity extends AppCompatActivity {

    private static final int CALL_PERMISSION_CODE = 123;
    private String numberToCall = "";
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helplines);

        // Individual Helpline Layouts/Buttons click listeners
        findViewById(R.id.layoutPolice).setOnClickListener(v -> makeDirectCall("112"));
        findViewById(R.id.layoutWomen).setOnClickListener(v -> makeDirectCall("1091"));
        findViewById(R.id.layoutAmbulance).setOnClickListener(v -> makeDirectCall("102"));
        findViewById(R.id.layoutChild).setOnClickListener(v -> makeDirectCall("1098"));
    }

    private void makeDirectCall(String number) {
        numberToCall = number;
        // Check if permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, CALL_PERMISSION_CODE);
        } else {
            // Permission already granted, start call
            startCallIntent(number);
        }
    }

    private void startCallIntent(String number) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + number));
        startActivity(callIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCallIntent(numberToCall);
            } else {
                Toast.makeText(this, "Permission DENIED to make calls", Toast.LENGTH_SHORT).show();
            }
        }
    }
}