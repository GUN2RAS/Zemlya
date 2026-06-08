package com.example.access;

import net.minecraft.core.Direction;

/**
 * Extension interface for EntityRenderState to carry gravity direction from
 * entity to renderer.
 * In 1.21.11, the render pipeline separates state extraction
 * (updateRenderState) from rendering.
 * We store the gravity direction during updateRenderState so it's available
 * during render.
 */
public interface EntityRenderStateExtension {
    Direction gravity_getGravityDirection();

    void gravity_setGravityDirection(Direction direction);

    // Animation properties for slerp
    Direction gravity_getPrevGravityDirection();

    void gravity_setPrevGravityDirection(Direction direction);

    float gravity_getGravityTransitionProgress();

    void gravity_setGravityTransitionProgress(float progress);
}