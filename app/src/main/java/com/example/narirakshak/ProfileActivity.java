package com.example.narirakshak;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    EditText etName, etPhone, etEmail;
    Button btnSaveProfile, btnLogout;
    ImageView btnAboutUs, btnHistory; // ✅ 1. Naya History ImageView declare kiya

    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnAboutUs = findViewById(R.id.btnAboutUs);

        // ✅ 2. XML se History icon ko connect karo (Make sure XML mein id @+id/btnHistory ho)
        btnHistory = findViewById(R.id.btnHistory);

        loadProfile();

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnAboutUs.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, AboutUsActivity.class));
        });

        // ✅ 3. History icon click par EvidenceHistoryActivity khulega
        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, EvidenceHistoryActivity.class));
        });
    }

    // ... loadProfile aur saveProfile wahi rahenge ...
    private void loadProfile() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                etName.setText(doc.getString("name"));
                etPhone.setText(doc.getString("phone"));
                etEmail.setText(doc.getString("email"));
            }
        });
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(email)) return;

        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("phone", phone);
        user.put("email", email);

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show());
    }
}