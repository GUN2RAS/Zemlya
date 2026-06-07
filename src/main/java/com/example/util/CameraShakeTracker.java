package com.example.util;

public class CameraShakeTracker {
    public static float shakeIntensity = 0.0f;

    public static void addShake(float intensity) {
        shakeIntensity = Math.max(shakeIntensity, intensity);
    }
}