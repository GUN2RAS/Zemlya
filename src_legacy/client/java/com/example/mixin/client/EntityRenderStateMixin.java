package com.example.mixin.client;

import com.example.access.EntityRenderStateExtension;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds gravity direction storage to EntityRenderState.
 * This allows the gravity direction to be carried from the entity (during
 * updateRenderState)
 * to the renderer (during render) in 1.21.11's decoupled render pipeline.
 */
@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements EntityRenderStateExtension {

    @Unique
    private Direction gravity$gravityDirection = Direction.DOWN;

    @Unique
    private Direction gravity$prevGravityDirection = Direction.DOWN;

    @Unique
    private float gravity$gravityTransitionProgress = 1.0f;

    @Override
    public Direction gravity_getGravityDirection() {
        return this.gravity$gravityDirection;
    }

    @Override
    public void gravity_setGravityDirection(Direction direction) {
        this.gravity$gravityDirection = direction;
    }

    @Override
    public Direction gravity_getPrevGravityDirection() {
        return this.gravity$prevGravityDirection;
    }

    @Override
    public void gravity_setPrevGravityDirection(Direction direction) {
        this.gravity$prevGravityDirection = direction;
    }

    @Override
    public float gravity_getGravityTransitionProgress() {
        return this.gravity$gravityTransitionProgress;
    }

    @Override
    public void gravity_setGravityTransitionProgress(float progress) {
        this.gravity$gravityTransitionProgress = progress;
    }
}