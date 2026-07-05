package com.example.narirakshak;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import android.content.Context;

public class SignupActivity extends AppCompatActivity {

    EditText etName, etPhone, etEmail, etPassword;
    Button btnSignup;

    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignup = findViewById(R.id.btnSignup);

        btnSignup.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = (etName != null) ? etName.getText().toString().trim() : "";
        String phone = (etPhone != null) ? etPhone.getText().toString().trim() : "";
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter password");
            return;
        }

        // 1. BUTTON KO LOCK KARO TAAKI DOUBLE CLICK NA HO
        btnSignup.setEnabled(false);
        btnSignup.setText("Please wait...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        String uid = auth.getCurrentUser().getUid();

                        Map<String, Object> userProfile = new HashMap<>();
                        userProfile.put("name", name);
                        userProfile.put("phone", phone);
                        userProfile.put("email", email);

                        db.collection("users").document(uid).set(userProfile)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(SignupActivity.this, "Signup Successful!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SignupActivity.this, HomeActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    // Agar yahan fail hua, toh button wapas chalu karo
                                    btnSignup.setEnabled(true);
                                    btnSignup.setText("Sign Up");
                                    Toast.makeText(SignupActivity.this, "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                    } else {
                        // Agar Signup fail hua, toh button wapas chalu karo
                        btnSignup.setEnabled(true);
                        btnSignup.setText("Sign Up");
                        Toast.makeText(SignupActivity.this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}