package com.example.narirakshak;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.content.Context;

// ✅ Firebase imports add kiye gaye hain
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class CameraActivity extends AppCompatActivity {

    ImageView imageView;
    Button btnCapture, btnUpload;
    Bitmap capturedImage;
    ActivityResultLauncher<Intent> cameraLauncher;

    String IMGBB_API_KEY = "5c3316252f288147a05c6b7800f01f7a";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        imageView = findViewById(R.id.imageView);
        btnCapture = findViewById(R.id.btnCapture);
        btnUpload = findViewById(R.id.btnUpload);

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            capturedImage = (Bitmap) extras.get("data");
                            imageView.setImageBitmap(capturedImage);
                        }
                    }
                });

        btnCapture.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, 100);
            } else {
                openCamera();
            }
        });

        btnUpload.setOnClickListener(v -> {
            if (capturedImage != null) {
                uploadToImgBB(capturedImage);
            } else {
                Toast.makeText(this, "Please capture a Photo!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(intent);
        } else {
            Toast.makeText(this, "Camera nahi mila!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadToImgBB(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", IMGBB_API_KEY)
                .addFormDataPart("image", base64Image)
                .build();

        Request request = new Request.Builder()
                .url("https://api.imgbb.com/1/upload")
                .post(requestBody)
                .build();

        Toast.makeText(this, "Picture is uploading....", Toast.LENGTH_SHORT).show();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(CameraActivity.this,
                        "Upload fail: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        String imageUrl = jsonObject.getJSONObject("data").getString("url");

                        // ✅ NAYA: Manual photo ko history me bhi save karo
                        saveEvidenceToHistory(imageUrl);

                        // Photo link SharedPreferences mein save karo
                        getSharedPreferences("NariRakshakPrefs", MODE_PRIVATE)
                                .edit()
                                .putString("last_photo_link", imageUrl)
                                .apply();

                        runOnUiThread(() -> {
                            Toast.makeText(CameraActivity.this,
                                    "✅ Photo is saved in SOS & History!", Toast.LENGTH_LONG).show();

                            new android.os.Handler(android.os.Looper.getMainLooper())
                                    .postDelayed(() -> finish(), 1500);
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(CameraActivity.this,
                                "Error!", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(CameraActivity.this,
                            "Upload error!", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // ✅ NAYA METHOD: Firestore mein link save karne ke liye
    private void saveEvidenceToHistory(String imageUrl) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        Map<String, Object> historyData = new HashMap<>();
        historyData.put("imageUrl", imageUrl);
        historyData.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("evidence_history")
                .add(historyData)
                .addOnSuccessListener(doc -> android.util.Log.d("Firestore", "Manual photo saved successfully!"))
                .addOnFailureListener(e -> android.util.Log.e("Firestore", "Manual photo save failed", e));
    }
}