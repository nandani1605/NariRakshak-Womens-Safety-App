package com.example.narirakshak;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;

public class AboutUsActivity extends AppCompatActivity {

    FirebaseFirestore db;
    FirebaseAuth auth;
    EditText etFeedbackMessage;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etFeedbackMessage = findViewById(R.id.etFeedbackMessage);
        RatingBar ratingBar = findViewById(R.id.ratingBar);
        Button btnFeedback = findViewById(R.id.btnFeedback);

        btnFeedback.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String message = etFeedbackMessage.getText().toString().trim();

            if (rating == 0 && TextUtils.isEmpty(message)) {
                Toast.makeText(this, "Please provide a rating or message!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (auth.getCurrentUser() == null) return;
            String uid = auth.getCurrentUser().getUid();
            String userEmail = auth.getCurrentUser().getEmail();

            // Data map for Firebase
            Map<String, Object> report = new HashMap<>();
            report.put("rating", rating);
            report.put("message", message); // Yeh aapka complaint data hai
            report.put("userEmail", userEmail);
            report.put("timestamp", com.google.firebase.Timestamp.now());

            db.collection("reports").document(uid)
                    .set(report)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "⭐⭐⭐⭐⭐\nReport Submitted Successfully!", Toast.LENGTH_LONG).show();
                        etFeedbackMessage.setText(""); // Box clear kar do
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // ✅ Action bar title ko bhi string reference se connect kar diya
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}