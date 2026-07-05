package com.example.narirakshak;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class EvidenceHistoryActivity extends AppCompatActivity {

    ListView listEvidence;
    ArrayList<String> evidenceDisplayList;
    ArrayList<String> urlList;
    ArrayAdapter<String> adapter;

    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evidence_history);

        listEvidence = findViewById(R.id.listEvidence);
        evidenceDisplayList = new ArrayList<>();
        urlList = new ArrayList<>();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, evidenceDisplayList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);

                text.setTextColor(Color.BLACK);
                text.setTextSize(16f);
                text.setPadding(40, 40, 40, 40);
                text.setLineSpacing(1.2f, 1.2f);

                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.RECTANGLE);
                shape.setCornerRadius(25f);
                shape.setColor(Color.WHITE);

                view.setBackground(shape);

                return view;
            }
        };

        listEvidence.setAdapter(adapter);

        listEvidence.setOnItemClickListener((parent, view, position, id) -> {
            String link = urlList.get(position);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            startActivity(browserIntent);
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadEvidenceHistory();
    }

    private void loadEvidenceHistory() {
        if (auth.getCurrentUser() == null) return;

        db.collection("users").document(auth.getCurrentUser().getUid())
                .collection("evidence_history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    evidenceDisplayList.clear();
                    urlList.clear();

                    // ✅ Yahan dono strings fetch kar li
                    String labelTaken = getString(R.string.evidence_taken);
                    String labelTapToView = getString(R.string.tap_to_view);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String url = doc.getString("imageUrl");
                        long timestamp = doc.getLong("timestamp");

                        String dateString = new SimpleDateFormat("dd/MM/yyyy  hh:mm a", Locale.getDefault())
                                .format(new Date(timestamp));

                        // ✅ Hard-coded text hata kar labelTapToView variable laga diya
                        String displayText = "🕒 " + labelTaken + " " + dateString + "\n\n" + "📸 " + labelTapToView;

                        evidenceDisplayList.add(displayText);
                        urlList.add(url);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load history", Toast.LENGTH_SHORT).show());
    }
}