package com.example.util;

public class CameraShakeTracker {
    public static float shakeIntensity = 0.0f;
    public static float maxIntensity = 1.0f;
    public static float shakeTime = 0.0f;

    public static void addShake(float intensity) {
        shakeIntensity = Math.max(shakeIntensity, intensity);
        maxIntensity = Math.max(maxIntensity, intensity);
        shakeTime = 0.0f;
    }
}