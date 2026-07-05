package com.example.narirakshak;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import android.content.Context;
public class TipsActivity extends AppCompatActivity {

    private ImageView imgThumbnail1, imgThumbnail2, imgThumbnail3, imgThumbnail4;

    // Aapke YouTube video IDs
    private final String video1 = "T7aNSRoDCmg";
    private final String video2 = "XQyDiAqXUGY";
    private final String video3 = "fr2dahob6g0";
    private final String video4 = "NNHqECnLLwI";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips);

        // XML se ImageViews connect karna
        imgThumbnail1 = findViewById(R.id.img_thumbnail_1);
        imgThumbnail2 = findViewById(R.id.img_thumbnail_2);
        imgThumbnail3 = findViewById(R.id.img_thumbnail_3);
        imgThumbnail4 = findViewById(R.id.img_thumbnail_4);

        // Thumbnail images load karna
        loadYouTubeThumbnail(video1, imgThumbnail1);
        loadYouTubeThumbnail(video2, imgThumbnail2);
        loadYouTubeThumbnail(video3, imgThumbnail3);
        loadYouTubeThumbnail(video4, imgThumbnail4);

        // Click karne par YouTube app me open karna
        imgThumbnail1.setOnClickListener(v -> openYouTubeVideo(video1));
        imgThumbnail2.setOnClickListener(v -> openYouTubeVideo(video2));
        imgThumbnail3.setOnClickListener(v -> openYouTubeVideo(video3));
        imgThumbnail4.setOnClickListener(v -> openYouTubeVideo(video4));
    }

    // Helper Method: YouTube App open karne ke liye
    private void openYouTubeVideo(String videoId) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + videoId));
        startActivity(intent);
    }

    // Helper Method: Internet se Thumbnail nikal kar lagane ke liye
    private void loadYouTubeThumbnail(String videoId, ImageView imageView) {
        // High quality thumbnail ka default URL format
        String thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";

        new Thread(() -> {
            try {
                InputStream in = new java.net.URL(thumbnailUrl).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);

                runOnUiThread(() -> {
                    imageView.setImageBitmap(bitmap);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}