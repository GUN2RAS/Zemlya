package com.example.mixin.client;

import com.example.access.EntityRenderStateExtension;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements EntityRenderStateExtension {

    @Unique
    private Direction gravity$gravityDirection = Direction.DOWN;

    @Unique
    private Direction gravity$prevGravityDirection = Direction.DOWN;

    @Unique
    private float gravity$gravityTransitionProgress = 1.0f;

    @Unique
    private boolean gravity$isPlayer = false;

    @Unique
    private float gravity$yRot = 0.0f;

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

    @Override
    public boolean gravity_isPlayer() {
        return this.gravity$isPlayer;
    }

    @Override
    public void gravity_setIsPlayer(boolean isPlayer) {
        this.gravity$isPlayer = isPlayer;
    }

    @Override
    public float gravity_getYRot() {
        return this.gravity$yRot;
    }

    @Override
    public void gravity_setYRot(float yRot) {
        this.gravity$yRot = yRot;
    }

    @Unique
    private float gravity$anomalyStrength = 0.0f;

    @Override
    public float gravity_getAnomalyStrength() {
        return this.gravity$anomalyStrength;
    }

    @Override
    public void gravity_setAnomalyStrength(float strength) {
        this.gravity$anomalyStrength = strength;
    }
}
