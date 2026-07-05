package com.example.narirakshak;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

public class ShakeService extends Service {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private FusedLocationProviderClient fusedLocationClient;

    private static final String CHANNEL_ID = "ShakeServiceChannel";
    private static final String SOS_CHANNEL_ID = "SOSAlertChannel";
    private static final int SOS_NOTIF_ID = 99;

    // Baar baar SOS na jaaye — cooldown 30 sec
    private long lastSosTime = 0;
    private static final long SOS_COOLDOWN_MS = 30000;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }
    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();

        mShakeDetector.setOnShakeListener(count -> {
            if (count >= 3) {
                long now = System.currentTimeMillis();

                // ✅ Cooldown check — 30 sec mein dobara SOS na jaaye
                if (now - lastSosTime < SOS_COOLDOWN_MS) return;
                lastSosTime = now;

                // ✅ Screen off ho ya app band — yeh sab kaam karega:
                wakeScreen();       // Screen jagao
                vibratePhone();     // Vibrate karo
                showSOSNotification(); // Notification dikhao
                sendSOSDirectly();  // SOS + Location seedha bhejo
            }
        });

        if (mAccelerometer != null) {
            mSensorManager.registerListener(mShakeDetector, mAccelerometer,
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    // =========================================================
    // Screen off ho to screen jagao
    // =========================================================
    private void wakeScreen() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.ON_AFTER_RELEASE,
                    "NariRakshak:SOSWakeLock"
            );
            wakeLock.acquire(10000); // 10 sec ke liye screen on
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // Phone vibrate karo — user ko pata chale SOS trigger hua
    // =========================================================
    private void vibratePhone() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(
                            new long[]{0, 500, 200, 500, 200, 500}, -1));
                } else {
                    vibrator.vibrate(new long[]{0, 500, 200, 500, 200, 500}, -1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // SOS Notification dikhao — tap karne par app khulega
    // =========================================================
    private void showSOSNotification() {
        createSOSNotificationChannel();

        // App open karne ka intent
        Intent openAppIntent = new Intent(this, HomeActivity.class);
        openAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // ✅ Ye raha sahi code, bas ise copy karo:
        PendingIntent openAppPending = PendingIntent.getActivity(this, 1,
                openAppIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, SOS_CHANNEL_ID)
                .setContentTitle("SOS Alert Triggered!")
                .setContentText("Shake detected — SOS and Location is being sent!")
                .setSmallIcon(R.drawable.narirakshak)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(openAppPending)
                .build();

        // Permission check ke sath notification show karo
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(SOS_NOTIF_ID, notification);
        }
    }

    // =========================================================
    // ✅ App band ho tab bhi seedha SOS + Location bhejo
    // Dialog nahi dikhayenge — directly send
    // =========================================================
    // =========================================================
    // ✅ App band ho tab bhi seedha SOS + Location bhejo
    // NAYA LOGIC: GPS se fresh location aane ka wait karega
    // =========================================================
    private void sendSOSDirectly() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        // 1. Fresh aur sateek (High Accuracy) location ki request banai
        com.google.android.gms.location.LocationRequest locationRequest = com.google.android.gms.location.LocationRequest.create()
                .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000)
                .setFastestInterval(1000)
                .setNumUpdates(1); // Sirf 1 baar sateek location chahiye

        // 2. GPS ko jagaya aur location aane ka wait kiya
        fusedLocationClient.requestLocationUpdates(locationRequest, new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {

                    double lat = locationResult.getLastLocation().getLatitude();
                    double lon = locationResult.getLastLocation().getLongitude();
                    String locationLink = "http://maps.google.com/?q=" + lat + "," + lon;

                    // CameraActivity se pehle li hui photo link check karo
                    String savedPhotoLink = getSharedPreferences("NariRakshakPrefs", MODE_PRIVATE)
                            .getString("last_photo_link", "");

                    String message = "🆘 SOS! I am in danger. My Location: " + locationLink;
                    if (!savedPhotoLink.isEmpty()) {
                        message += "\nPhoto: " + savedPhotoLink;
                        // ✅ Use hone ke baad clear karo
                        getSharedPreferences("NariRakshakPrefs", MODE_PRIVATE)
                                .edit().remove("last_photo_link").apply();
                    }

                    // 3. Location milne ke baad hi SMS bheja
                    sendSMSToAllContacts(message);

                    // 4. Battery bachane ke liye GPS tracking band kar di
                    fusedLocationClient.removeLocationUpdates(this);
                }
            }
        }, android.os.Looper.getMainLooper());
    }

    private void sendSMSToAllContacts(String message) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("contacts")
                .get()
                .addOnSuccessListener(docs -> {
                    for (DocumentSnapshot d : docs) {
                        sendSMS(d.getString("number"), message);
                    }
                });
    }

    private void sendSMS(String number, String message) {
        try {
            android.telephony.SmsManager.getDefault().sendMultipartTextMessage(
                    number, null,
                    android.telephony.SmsManager.getDefault().divideMessage(message),
                    null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NariRakshak Active")
                .setContentText("Protecting you in background... Shake to trigger SOS.")
                .setSmallIcon(R.drawable.narirakshak)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        // ✅ START_STICKY — service band ho to Android khud restart kare
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Shake Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void createSOSNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    SOS_CHANNEL_ID,
                    "SOS Alert Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableVibration(true);
            channel.enableLights(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mShakeDetector);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}