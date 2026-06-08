package com.example.client.render;

import com.example.entity.RiftLightningEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

public class RiftLightningEntityRenderer extends EntityRenderer<RiftLightningEntity, RiftLightningEntityRenderer.State> {
    
    public static class State extends EntityRenderState {
        public float tickCount;
        public long seed;
        public Vec3 target;
        public float age;
    }

    public RiftLightningEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RiftLightningEntity entity, State state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.seed = entity.seed;
        state.target = entity.getTarget();
        state.tickCount = entity.tickCount + tickDelta;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        Vec3 target = state.target;
        Vec3 start = new Vec3(state.x, state.y, state.z);
        
        // Calculate relative target vector
        Vec3 dir = target.subtract(start);
        double distance = dir.length();
        
        System.out.println("RENDER LIGHTNING! Start: " + start + " Target: " + target + " Dist: " + distance + " Age: " + state.tickCount);
        
        // If target is uninitialized (0,0,0) and distance is huge, or too close
        if (distance < 0.1 || (target.x == 0 && target.y == 0 && target.z == 0)) return;
        
        // Fade out based on age
        float alphaTemp = 1.0f - (state.tickCount / 15.0f);
        if (alphaTemp < 0) alphaTemp = 0;
        final float alpha = alphaTemp;
        
        // Purple color
        int r = 150;
        int g = 25;
        int b = 230;
        int a = (int)(alpha * 255);
        if (a <= 0) return;

        RenderType layer = RenderTypes.entityTranslucentEmissive(net.minecraft.resources.Identifier.withDefaultNamespace("textures/entity/beacon_beam.png"));

        submitNodeCollector.submitCustomGeometry(poseStack, layer, (matricesEntry, vertexConsumer) -> {
            Matrix4f matrix = matricesEntry.pose();
            RandomSource random = RandomSource.create(state.seed);
            
            // Generate main branch points
            int segments = (int) Math.max(2, distance / 2.0); // 1 segment per 2 blocks
            Vec3[] points = new Vec3[segments + 1];
            points[0] = Vec3.ZERO;
            points[segments] = dir;
            
            for (int i = 1; i < segments; i++) {
                double progress = (double) i / segments;
                Vec3 basePoint = dir.scale(progress);
                
                double offsetAmt = 1.5;
                Vec3 offset = new Vec3(
                    (random.nextDouble() - 0.5) * offsetAmt,
                    (random.nextDouble() - 0.5) * offsetAmt,
                    (random.nextDouble() - 0.5) * offsetAmt
                );
                
                points[i] = basePoint.add(offset);
            }
            
            for (int l = 0; l < 3; l++) {
                float thickness = 0.1f + l * 0.15f;
                float currentA = alpha * (1.0f - l * 0.3f);
                if (currentA <= 0) continue;
                
                for (int i = 0; i < segments; i++) {
                    Vec3 p1 = points[i];
                    Vec3 p2 = points[i + 1];
                    
                    drawSegment(matricesEntry, matrix, vertexConsumer, p1, p2, thickness, 0.6f, 0.1f, 0.9f, currentA);
                    
                    if (random.nextDouble() < 0.3) {
                        Vec3 branchEnd = p1.add(
                            dir.normalize().scale(1.0 + random.nextDouble() * 2.0)
                        ).add(
                            (random.nextDouble() - 0.5) * 3.0,
                            (random.nextDouble() - 0.5) * 3.0,
                            (random.nextDouble() - 0.5) * 3.0
                        );
                        drawSegment(matricesEntry, matrix, vertexConsumer, p1, branchEnd, thickness * 0.5f, 0.6f, 0.1f, 0.9f, currentA * 0.7f);
                    }
                }
            }
        });
    }
    
    private void drawSegment(PoseStack.Pose matricesEntry, Matrix4f matrix, VertexConsumer consumer, Vec3 p1, Vec3 p2, float thickness, float r, float g, float b, float a) {
        Vec3 dir = p2.subtract(p1).normalize();
        Vec3 up = new Vec3(0, 1, 0);
        if (Math.abs(dir.y) > 0.9) {
            up = new Vec3(1, 0, 0);
        }
        Vec3 right = dir.cross(up).normalize().scale(thickness);
        up = right.cross(dir).normalize().scale(thickness);
        
        int rI = (int)(r * 255);
        int gI = (int)(g * 255);
        int bI = (int)(b * 255);
        int aI = (int)(a * 255);

        // Quad 1 (Front)
        addVertex(matricesEntry, consumer, (float)(p1.x - right.x), (float)(p1.y - right.y), (float)(p1.z - right.z), rI, gI, bI, aI);
        addVertex(matricesEntry, consumer, (float)(p1.x + right.x), (float)(p1.y + right.y), (float)(p1.z + right.z), rI, gI, bI, aI);
        addVertex(matricesEntry, consumer, (float)(p2.x + right.x), (float)(p2.y + right.y), (float)(p2.z + right.z), rI, gI, bI, aI);
        addVertex(matricesEntry, consumer, (float)(p2.x - right.x), (float)(p2.y - right.y), (float)(p2.z - right.z), rI, gI, bI, aI);
        
        // Quad 1 (Back)
        addVertex(matricesEntry, consumer, (float)(p1.x - right.x), (float)(p1.y - right.y), (float)(p1.z - right.z), rI, gI, bI, aI);
        addVertex(matricesEntry, consumer, (float)(p2.x - right.x), (float)(p2.y - right.y), (float)(p2.z - right.z), rI, gI, bI, aI);
        addVertex(matricesEntry, consumer, (float)(p2.x + right.x), (float)(p2.y + right.y), (float)(p2.z + right.z), rI, gI, bI, aI);
        addVertex(matricesEntry, consumer, (float)(p1.x + right.x), (float)(p1.y + right.y), (float)(p1.z + right.z), rI, gI, bI, aI);
        
        // Quad 2 (Front)
        addVertex(matricesEntry, consumer, (float)(p1.x - up.x), (float)(p1.y - up.y), (float)(p1.z - up.z), rI, gI, bI, aI);
        addVertex(matricesEntry, consumer, (float)(p1.x + up.x), (float)(p1.y + up.y), (float)(p1.z + up.z), rI, gI, bI, aI);
        addVertex(matricesEntry, consumer, (float)(p2.x + up.x), (float)(p2.y + up.y), (float)(p2.z + up.z), rI, gI, bI, aI);
        addVertex(matricesEntry, consumer, (float)(p2.x - up.x), (float)(p2.y - up.y), (float)(p2.z - up.z), rI, gI, bI, aI);
        
        // Quad 2 (Back)
        addVertex(matricesEntry, consumer, (float)(p1.x - up.x), (float)(p1.y - up.y), (float)(p1.z - up.z), rI, gI, bI, aI);
        addVertex(matricesEntry, consumer, (float)(p2.x - up.x), (float)(p2.y - up.y), (float)(p2.z - up.z), rI, gI, bI, aI);
        addVertex(matricesEntry, consumer, (float)(p2.x + up.x), (float)(p2.y + up.y), (float)(p2.z + up.z), rI, gI, bI, aI);
        addVertex(matricesEntry, consumer, (float)(p1.x + up.x), (float)(p1.y + up.y), (float)(p1.z + up.z), rI, gI, bI, aI);
    }

    private void addVertex(PoseStack.Pose matricesEntry, VertexConsumer consumer, float x, float y, float z, int r, int g, int b, int a) {
        consumer.addVertex(matricesEntry, x, y, z).setColor(r, g, b, a).setUv(0.5f, 0.5f)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(15728880).setNormal(matricesEntry, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public boolean shouldRender(RiftLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }
}
