package com.example.narirakshak;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    // ✅ Naya TextView tvForgotPassword add kiya
    TextView tvGoSignup, tvForgotPassword;
    FirebaseAuth auth;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // 2. Persistent Login Check: If user is already logged in, go straight to Home
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // 3. Link XML UI elements to Java variables
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoSignup = findViewById(R.id.tvGoSignup);
        // ✅ Naya TextView link kiya
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // 4. Set Click Listeners
        btnLogin.setOnClickListener(v -> loginUser());

        tvGoSignup.setOnClickListener(v -> {
            // This will open the Signup screen
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });

        // ✅ FORGOT PASSWORD LOGIC ADDED HERE
        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            // Agar user ne email box khali rakha hai
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Enter registered email here first");
                Toast.makeText(LoginActivity.this, "Please enter your email above to reset password", Toast.LENGTH_LONG).show();
                return;
            }

            // Firebase ko password reset link bhejane ka command
            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Password reset link sent to your email!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Basic Validation
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter password");
            return;
        }

        // 5. Firebase Login Logic
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}