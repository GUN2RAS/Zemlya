package com.example.access;

import net.minecraft.core.Direction;

public interface EntityRenderStateExtension {
    Direction gravity_getGravityDirection();

    void gravity_setGravityDirection(Direction direction);

    Direction gravity_getPrevGravityDirection();

    void gravity_setPrevGravityDirection(Direction direction);

    float gravity_getGravityTransitionProgress();

    void gravity_setGravityTransitionProgress(float progress);

    boolean gravity_isPlayer();

    void gravity_setIsPlayer(boolean isPlayer);

    float gravity_getYRot();

    void gravity_setYRot(float yRot);

    float gravity_getAnomalyStrength();

    void gravity_setAnomalyStrength(float strength);
}
