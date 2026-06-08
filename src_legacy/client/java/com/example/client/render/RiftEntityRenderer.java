package com.example.client.render;

import com.example.entity.RiftEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import org.joml.Matrix4f;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class RiftEntityRenderer extends EntityRenderer<RiftEntity, RiftEntityRenderer.RiftState> {
    public static class RiftState extends EntityRenderState {
        public float tickCount;
        public float age;
        public float rotation;
        public int combatState;
        public int waveNumber;
        public int waveTimer;
        public float phaseTime;
    }

    public RiftEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public RiftState createRenderState() {
        return new RiftState();
    }

    @Override
    public void extractRenderState(RiftEntity entity, RiftState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.tickCount = entity.tickCount + tickDelta;
        state.rotation = entity.clientRotation + entity.currentSpinSpeed * tickDelta;
        
        state.combatState = entity.getEntityData().get(RiftEntity.FIGHT_STATE);
        state.waveNumber = entity.getEntityData().get(RiftEntity.WAVE_NUMBER);
        state.waveTimer = entity.getEntityData().get(RiftEntity.WAVE_TIMER);
        state.phaseTime = 300 - (state.waveTimer - tickDelta);
    }

    @Override
    public void submit(RiftState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        float cubeSize = Math.min(6.0f, state.tickCount * 0.1f);
        float innerSize = cubeSize * 0.52f; // Уменьшено на 35% от 0.8f (0.8 * 0.65 = 0.52)

        // 1. Draw the Cubes
        poseStack.pushPose();
        
        // Вращаем вокруг вертикальной оси Y с учетом состояния босса
        poseStack.mulPose(Axis.YP.rotationDegrees(state.rotation));
        
        // Поворачиваем так, чтобы куб встал идеально на свой угол (вершину)
        poseStack.mulPose(Axis.ZP.rotationDegrees(54.735f));
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0f));

        // Inner Core (Crying Obsidian) - используем entityTranslucentEmissive
        RenderType innerLayer = RenderTypes.entityTranslucentEmissive(net.minecraft.resources.Identifier.withDefaultNamespace("textures/block/crying_obsidian.png"));
        submitNodeCollector.submitCustomGeometry(poseStack, innerLayer, (matricesEntry, consumer) -> {
            renderCube(matricesEntry, consumer, innerSize, 255, 255, 255, 255);
        });

        // Outer Shell (Tinted Glass) - используем entityTranslucent
        RenderType outerLayer = RenderTypes.entityTranslucent(net.minecraft.resources.Identifier.withDefaultNamespace("textures/block/tinted_glass.png"));
        submitNodeCollector.submitCustomGeometry(poseStack, outerLayer, (matricesEntry, consumer) -> {
            renderCube(matricesEntry, consumer, cubeSize, 255, 255, 255, 160);
        });

        poseStack.popPose(); // Pop cube rotation

        // 2. Draw the Pulsar Jets
        float jetAlphaMult = 1.0f;
        if (state.tickCount > 400.0f) {
            jetAlphaMult = Math.max(0.0f, 1.0f - (state.tickCount - 400.0f) / 40.0f);
        }

        if (jetAlphaMult > 0.0f) {
            poseStack.pushPose();
            RenderType jetLayer = RenderTypes.entityTranslucentEmissive(net.minecraft.resources.Identifier.withDefaultNamespace("textures/entity/beacon_beam.png"));
            final float finalJetAlphaMult = jetAlphaMult;
            submitNodeCollector.submitCustomGeometry(poseStack, jetLayer, (matricesEntry, consumer) -> {
                renderPillarBeams(matricesEntry, consumer, state.age, finalJetAlphaMult);
            });
            poseStack.popPose(); // Pop jets translation
        }

        poseStack.popPose(); // Pop main translation

        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private void renderCube(PoseStack.Pose matricesEntry, VertexConsumer consumer, float size, int r, int g, int b, int a) {
        Matrix4f matrix = matricesEntry.pose();
        
        // Top face (Y+)
        addVertexUV(matricesEntry, matrix, consumer, -size, size, -size, r, g, b, a, 0, 0);
        addVertexUV(matricesEntry, matrix, consumer, -size, size, size, r, g, b, a, 0, 1);
        addVertexUV(matricesEntry, matrix, consumer, size, size, size, r, g, b, a, 1, 1);
        addVertexUV(matricesEntry, matrix, consumer, size, size, -size, r, g, b, a, 1, 0);

        // Bottom face (Y-)
        addVertexUV(matricesEntry, matrix, consumer, -size, -size, -size, r, g, b, a, 0, 0);
        addVertexUV(matricesEntry, matrix, consumer, size, -size, -size, r, g, b, a, 1, 0);
        addVertexUV(matricesEntry, matrix, consumer, size, -size, size, r, g, b, a, 1, 1);
        addVertexUV(matricesEntry, matrix, consumer, -size, -size, size, r, g, b, a, 0, 1);

        // Front face (Z+)
        addVertexUV(matricesEntry, matrix, consumer, -size, -size, size, r, g, b, a, 0, 1);
        addVertexUV(matricesEntry, matrix, consumer, size, -size, size, r, g, b, a, 1, 1);
        addVertexUV(matricesEntry, matrix, consumer, size, size, size, r, g, b, a, 1, 0);
        addVertexUV(matricesEntry, matrix, consumer, -size, size, size, r, g, b, a, 0, 0);

        // Back face (Z-)
        addVertexUV(matricesEntry, matrix, consumer, -size, -size, -size, r, g, b, a, 1, 1);
        addVertexUV(matricesEntry, matrix, consumer, -size, size, -size, r, g, b, a, 1, 0);
        addVertexUV(matricesEntry, matrix, consumer, size, size, -size, r, g, b, a, 0, 0);
        addVertexUV(matricesEntry, matrix, consumer, size, -size, -size, r, g, b, a, 0, 1);

        // Left face (X-)
        addVertexUV(matricesEntry, matrix, consumer, -size, -size, -size, r, g, b, a, 0, 1);
        addVertexUV(matricesEntry, matrix, consumer, -size, -size, size, r, g, b, a, 1, 1);
        addVertexUV(matricesEntry, matrix, consumer, -size, size, size, r, g, b, a, 1, 0);
        addVertexUV(matricesEntry, matrix, consumer, -size, size, -size, r, g, b, a, 0, 0);

        // Right face (X+)
        addVertexUV(matricesEntry, matrix, consumer, size, -size, -size, r, g, b, a, 1, 1);
        addVertexUV(matricesEntry, matrix, consumer, size, size, -size, r, g, b, a, 1, 0);
        addVertexUV(matricesEntry, matrix, consumer, size, size, size, r, g, b, a, 0, 0);
        addVertexUV(matricesEntry, matrix, consumer, size, -size, size, r, g, b, a, 0, 1);
    }

    private void renderPillarBeams(PoseStack.Pose matricesEntry, VertexConsumer consumer, float age, float alphaMult) {
        float jetLength = 300.0f; // 300 блоков
        float jetWidth = 3.0f; // Fixed width for pillar beams
        Matrix4f matrix = matricesEntry.pose();
        
        for (int i = 0; i < 10; i++) {
            double angle = 2.0 * (-Math.PI + 0.1 * Math.PI * i);
            float px = (float) Math.floor(42.0 * Math.cos(angle));
            float pz = (float) Math.floor(42.0 * Math.sin(angle));
            
            drawBeamAt(matricesEntry, matrix, consumer, px, pz, jetWidth, jetLength, alphaMult);
        }
    }

    private void drawBeamAt(PoseStack.Pose matricesEntry, Matrix4f matrix, VertexConsumer consumer, float offsetX, float offsetZ, float width, float length, float alphaMult) {
        float yStart = -300.0f; // 300 блоков вниз
        float yEnd = 300.0f;    // 300 блоков вверх
        
        int r = 200, g = 100, b = 255; 
        int a = (int)(200 * alphaMult);
        
        // Plane 1 (X-Y)
        addVertex(matricesEntry, matrix, consumer, offsetX - width, yStart, offsetZ, r, g, b, a);
        addVertex(matricesEntry, matrix, consumer, offsetX + width, yStart, offsetZ, r, g, b, a);
        addVertex(matricesEntry, matrix, consumer, offsetX + width, yEnd, offsetZ, r, g, b, 0);
        addVertex(matricesEntry, matrix, consumer, offsetX - width, yEnd, offsetZ, r, g, b, 0);
        
        // Plane 2 (Z-Y)
        addVertex(matricesEntry, matrix, consumer, offsetX, yStart, offsetZ - width, r, g, b, a);
        addVertex(matricesEntry, matrix, consumer, offsetX, yStart, offsetZ + width, r, g, b, a);
        addVertex(matricesEntry, matrix, consumer, offsetX, yEnd, offsetZ + width, r, g, b, 0);
        addVertex(matricesEntry, matrix, consumer, offsetX, yEnd, offsetZ - width, r, g, b, 0);
        
        // Plane 3 (Diagonal)
        float diag = width * 0.707f;
        addVertex(matricesEntry, matrix, consumer, offsetX - diag, yStart, offsetZ - diag, r, g, b, a);
        addVertex(matricesEntry, matrix, consumer, offsetX + diag, yStart, offsetZ + diag, r, g, b, a);
        addVertex(matricesEntry, matrix, consumer, offsetX + diag, yEnd, offsetZ + diag, r, g, b, 0);
        addVertex(matricesEntry, matrix, consumer, offsetX - diag, yEnd, offsetZ - diag, r, g, b, 0);

        // Plane 4 (Diagonal)
        addVertex(matricesEntry, matrix, consumer, offsetX - diag, yStart, offsetZ + diag, r, g, b, a);
        addVertex(matricesEntry, matrix, consumer, offsetX + diag, yStart, offsetZ - diag, r, g, b, a);
        addVertex(matricesEntry, matrix, consumer, offsetX + diag, yEnd, offsetZ - diag, r, g, b, 0);
        addVertex(matricesEntry, matrix, consumer, offsetX - diag, yEnd, offsetZ + diag, r, g, b, 0);
    }

    private void addVertexUV(PoseStack.Pose matricesEntry, Matrix4f matrix, VertexConsumer consumer, float x, float y, float z, int r, int g, int b, int a, float u, float v) {
        consumer.addVertex(matricesEntry, x, y, z).setColor(r, g, b, a).setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(15728880).setNormal(matricesEntry, 0.0f, 1.0f, 0.0f);
    }

    private void addVertex(PoseStack.Pose matricesEntry, Matrix4f matrix, VertexConsumer consumer, float x, float y, float z, int r, int g, int b, int a) {
        addVertexUV(matricesEntry, matrix, consumer, x, y, z, r, g, b, a, 0.0f, 0.0f);
    }

    @Override
    public boolean shouldRender(RiftEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }
}
