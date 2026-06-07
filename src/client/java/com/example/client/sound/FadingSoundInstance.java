package com.example.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class FadingSoundInstance extends AbstractTickableSoundInstance {
    private final long startTime;
    private final long fadeStartTimeMs;
    private final long fadeLengthMs;
    private final float maxVolume;

    public FadingSoundInstance(SoundEvent sound, SoundSource source, float volume, float pitch, double x, double y, double z, int fadeStartTick, int fadeLength) {
        super(sound, source, RandomSource.create());
        this.volume = volume;
        this.maxVolume = volume;
        this.pitch = pitch;
        this.x = x;
        this.y = y;
        this.z = z;
        this.looping = false;
        this.fadeStartTimeMs = fadeStartTick * 50L;
        this.fadeLengthMs = fadeLength * 50L;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void tick() {
        long elapsed = System.currentTimeMillis() - this.startTime;
        if (elapsed > this.fadeStartTimeMs) {
            float fadeProgress = (elapsed - this.fadeStartTimeMs) / (float) this.fadeLengthMs;
            this.volume = Math.max(0.0f, this.maxVolume * (1.0f - fadeProgress));
            if (this.volume <= 0.0f) {
                this.stop();
            }
        }
    }
}
