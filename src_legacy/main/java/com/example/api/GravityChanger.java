package com.example.api;

public interface GravityChanger {
    Gravity getGravityDirection();

    void setGravity(Gravity gravity);

    /**
     * Called when gravity changes to update internal states.
     */
    void onGravityChanged();

    /**
     * Returns the previous gravity direction (used for animation).
     */
    Gravity getPrevGravityDirection();

    /**
     * Returns the remaining ticks for gravity transition animation.
     */
    int getGravityTransitionTimer();

    /**
     * Set the gravity transition timer directly (e.g. for immune).
     */
    void setGravityTransitionTimer(int ticks);

    /**
     * Returns the transition progress from 0.0f (start) to 1.0f (finish) with
     * tickDelta.
     */
    float getGravityTransitionProgress(float tickDelta);

    /**
     * Checks if the entity is currently immune to fall damage due to a recent
     * gravity change.
     */
    boolean hasGravityImmunity();
}