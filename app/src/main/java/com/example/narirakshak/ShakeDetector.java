package com.example.narirakshak;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeDetector implements SensorEventListener {

    // Threshold KAM KAR DIYA HAI: Ab thode se jhatke mein detect ho jayega (Pehle 2.7 tha, ab 1.8 hai)
    private static final float SHAKE_THRESHOLD_GRAVITY = 1.8F;

    // Slop Time: Do shakes ke beech minimum gap
    private static final int SHAKE_SLOP_TIME_MS = 500;

    // AI LOGIC ADDED: Reset Time (Agar 3 seconds tak shake na ho toh reset kar do)
    private static final int SHAKE_COUNT_RESET_TIME_MS = 3000;

    private OnShakeListener mListener;
    private long mShakeTimestamp;

    // AI LOGIC ADDED: Counter lagaya hai
    private int mShakeCount = 0;

    public interface OnShakeListener {
        // Ab onShake(count) batayega ki kitni baar shake hua
        void onShake(int count);
    }

    public void setOnShakeListener(OnShakeListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mListener != null) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            // G-Force (Total Acceleration) calculate ho rahi hai
            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                final long now = System.currentTimeMillis();

                // Agar do shakes bohot jaldi hue, toh ignore karo
                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return;
                }

                // Agar pichle shake ko 3 seconds se zyada ho gaye, toh counter zero (0) se shuru karo
                if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                    mShakeCount = 0;
                }

                mShakeTimestamp = now;
                mShakeCount++; // Shake count badhao

                // Listener ko batao ki shake hua hai aur kitni baar hua hai
                mListener.onShake(mShakeCount);
            }
        }
    }
}