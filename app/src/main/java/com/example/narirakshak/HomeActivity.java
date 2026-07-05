package com.example.narirakshak;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import com.google.common.util.concurrent.ListenableFuture;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    Button btnSOS;
    LinearLayout btnHelplines, btnSiren, btnCamera, btnContacts, btnProfile, btnTips, btnSafePlaces, btnSettingsGrid;
    TextView tvSirenText;

    FirebaseAuth auth;
    private static final int REQ_PERMISSIONS = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private String currentLocationLink = "";

    boolean sirenPlaying = false;
    android.media.MediaPlayer mediaPlayer;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnSOS         = findViewById(R.id.btnSOS);
        btnHelplines   = findViewById(R.id.btnHelplines);
        btnSiren       = findViewById(R.id.btnSiren);
        tvSirenText    = findViewById(R.id.tvSirenText);
        btnCamera      = findViewById(R.id.btnCamera);
        btnContacts    = findViewById(R.id.btnContacts);
        btnProfile     = findViewById(R.id.btnProfile);
        btnTips        = findViewById(R.id.btnTips);
        btnSafePlaces  = findViewById(R.id.btnSafePlaces);
        btnSettingsGrid= findViewById(R.id.btnSettingsGrid);

        Intent serviceIntent = new Intent(this, ShakeService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        if (getIntent().getBooleanExtra("show_dialog", false)) {
            showSafetyDialog();
        }

        mSensorManager  = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer  = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector  = new ShakeDetector();
        mShakeDetector.setOnShakeListener(count -> {
            if (count >= 3) { showSafetyDialog(); }
        });

        mediaPlayer = android.media.MediaPlayer.create(this, R.raw.siren_sound);
        if (mediaPlayer != null) { mediaPlayer.setLooping(true); }

        btnSOS.setOnClickListener(v -> sendSOSOnly());
        btnSiren.setOnClickListener(v -> toggleSiren());
        btnHelplines.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, HelplinesActivity.class)));
        btnContacts.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ContactsActivity.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));
        btnTips.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, TipsActivity.class)));
        btnSafePlaces.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, SafePlacesActivity.class)));
        btnSettingsGrid.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, SettingsActivity.class)));

        btnCamera.setOnClickListener(v -> {
            try {
                startActivity(new Intent(HomeActivity.this, CameraActivity.class));
            } catch (Exception e) {
                Toast.makeText(HomeActivity.this, "Error: Camera UI not loading", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra("show_dialog", false)) {
            showSafetyDialog();
        }
    }

    private void showSafetyDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_threat_title));
        builder.setMessage(getString(R.string.dialog_threat_message, 20));
        builder.setCancelable(false);

        final android.os.CountDownTimer[] timer = new android.os.CountDownTimer[1];

        builder.setNegativeButton(getString(R.string.dialog_i_am_safe), (dialog, which) -> {
            if (timer[0] != null) timer[0].cancel();
            dialog.dismiss();
        });

        builder.setPositiveButton(getString(R.string.dialog_help_me), (dialog, which) -> {
            if (timer[0] != null) timer[0].cancel();
            dialog.dismiss();
            sendSOSOnly();
        });

        final android.app.AlertDialog dialog = builder.create();
        dialog.show();

        timer[0] = new android.os.CountDownTimer(20000, 1000) {
            @Override
            public void onTick(long l) {
                dialog.setMessage(getString(R.string.dialog_threat_message, (int)(l / 1000)));
            }
            @Override
            public void onFinish() {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                    sendSOSOnly();
                }
            }
        }.start();
    }

    private void sendSOSOnly() {
        Toast.makeText(this, getString(R.string.sos_sending), Toast.LENGTH_SHORT).show();
        getLiveLocationAndSendSMS();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            showEvidenceDialog();
        }, 2000);
    }

    private void showEvidenceDialog() {
        if (isFinishing() || isDestroyed()) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_evidence_title));
        builder.setMessage(getString(R.string.dialog_evidence_message));
        builder.setCancelable(false);

        builder.setPositiveButton(getString(R.string.dialog_yes), (dialog, which) -> {
            dialog.dismiss();
            captureEvidencePhoto();
        });

        builder.setNegativeButton(getString(R.string.dialog_no), (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(this, getString(R.string.sos_stay_safe), Toast.LENGTH_SHORT).show();
        });

        builder.create().show();
    }

    private void captureEvidencePhoto() {
        Toast.makeText(this, getString(R.string.camera_capturing), Toast.LENGTH_SHORT).show();

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                ImageCapture imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, imageCapture);

                File file = new File(getExternalFilesDir(null), "evidence_proof.jpg");
                ImageCapture.OutputFileOptions outputOptions =
                        new ImageCapture.OutputFileOptions.Builder(file).build();

                imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                        new ImageCapture.OnImageSavedCallback() {
                            @Override
                            public void onImageSaved(@NonNull ImageCapture.OutputFileResults res) {
                                runOnUiThread(() -> Toast.makeText(HomeActivity.this,
                                        getString(R.string.camera_uploading), Toast.LENGTH_SHORT).show());
                                uploadEvidenceToImgBB(file);
                            }
                            @Override
                            public void onError(@NonNull ImageCaptureException ex) {
                                Log.e("Camera", "Evidence capture failed: " + ex.getMessage());
                                runOnUiThread(() -> Toast.makeText(HomeActivity.this,
                                        getString(R.string.camera_error), Toast.LENGTH_SHORT).show());
                            }
                        });
            } catch (Exception e) {
                Log.e("Camera", "Error binding camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void uploadEvidenceToImgBB(File file) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", "5c3316252f288147a05c6b7800f01f7a")
                .addFormDataPart("image", file.getName(),
                        RequestBody.create(MediaType.parse("image/jpeg"), file))
                .build();

        client.newCall(new Request.Builder()
                .url("https://api.imgbb.com/1/upload")
                .post(body)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("ImgBB", "Upload failed: " + e.getMessage());
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response res) throws IOException {
                if (res.isSuccessful()) {
                    try {
                        String responseData = res.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        String imageUrl = jsonObject.getJSONObject("data").getString("url");

                        // ✅ YE LINE ADD KI GAYI HAI: Firestore mein data save karne ke liye
                        saveEvidenceToHistory(imageUrl);

                        sendEvidenceSMS(imageUrl);
                    } catch (Exception e) {
                        Log.e("ImgBB", "Parse error: " + e.getMessage());
                    }
                }
            }
        });
    }

    // ✅ NAYA METHOD: Firestore mein link save karne ke liye
    private void saveEvidenceToHistory(String imageUrl) {
        if (auth.getCurrentUser() == null) return;

        java.util.Map<String, Object> historyData = new java.util.HashMap<>();
        historyData.put("imageUrl", imageUrl);
        historyData.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("evidence_history")
                .add(historyData)
                .addOnSuccessListener(doc -> Log.d("Firestore", "History saved successfully!"))
                .addOnFailureListener(e -> Log.e("Firestore", "History save failed", e));
    }

    private void sendEvidenceSMS(String url) {
        if (auth.getCurrentUser() == null) return;
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("contacts")
                .get()
                .addOnSuccessListener(docs -> {
                    for (DocumentSnapshot d : docs) {
                        sendSMS(d.getString("number"),
                                getString(R.string.evidence_proof) + url);
                    }
                    runOnUiThread(() -> Toast.makeText(HomeActivity.this,
                            getString(R.string.evidence_sent), Toast.LENGTH_SHORT).show());
                });
    }

    private void getLiveLocationAndSendSMS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            currentLocationLink = (loc != null)
                    ? "http://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude()
                    : "Location unavailable";
            sendSOSMessageToSavedContacts();
        });
    }

    private void sendSOSMessageToSavedContacts() {
        if (auth.getCurrentUser() == null) return;

        String savedPhotoLink = getSharedPreferences("NariRakshakPrefs", MODE_PRIVATE)
                .getString("last_photo_link", "");

        String message = getString(R.string.sos_message) + currentLocationLink;
        if (!savedPhotoLink.isEmpty()) {
            message += "\nPhoto: " + savedPhotoLink;
            getSharedPreferences("NariRakshakPrefs", MODE_PRIVATE)
                    .edit().remove("last_photo_link").apply();
        }

        final String finalMessage = message;
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("contacts")
                .get()
                .addOnSuccessListener(docs -> {
                    for (DocumentSnapshot d : docs) {
                        sendSMS(d.getString("number"), finalMessage);
                    }
                });
    }

    private void sendSMS(String n, String m) {
        try {
            android.telephony.SmsManager.getDefault().sendMultipartTextMessage(
                    n, null,
                    android.telephony.SmsManager.getDefault().divideMessage(m),
                    null, null);
        } catch (Exception e) {
            Log.e("SMS", "Failed to send: " + e.getMessage());
        }
    }

    private void toggleSiren() {
        if (mediaPlayer != null) {
            if (sirenPlaying) {
                mediaPlayer.pause();
                tvSirenText.setText(getString(R.string.btn_siren));
            } else {
                mediaPlayer.start();
                tvSirenText.setText(getString(R.string.btn_siren_stop));
            }
            sirenPlaying = !sirenPlaying;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAccelerometer != null)
            mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mShakeDetector);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
    }
}