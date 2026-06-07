package com.example.client.render;

import com.example.entity.GravityFieldEntity;
import com.example.api.Gravity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class GravityFieldRenderer extends EntityRenderer<GravityFieldEntity, GravityFieldRenderer.GravityFieldState> {
    private static final com.mojang.blaze3d.pipeline.RenderPipeline CRACK_PIPELINE = com.mojang.blaze3d.pipeline.RenderPipeline.builder()
            .withLocation(net.minecraft.resources.Identifier.fromNamespaceAndPath("zemlya", "gravity_crack"))
            .withVertexShader(net.minecraft.resources.Identifier.fromNamespaceAndPath("zemlya", "core/rendertype_gravity_crack"))
            .withFragmentShader(net.minecraft.resources.Identifier.fromNamespaceAndPath("zemlya", "core/rendertype_gravity_crack"))
            .withSampler("Sampler0")
            .withUniform("DynamicTransforms", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(
                com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT
            ))
            .withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(
                com.mojang.blaze3d.platform.CompareOp.LESS_THAN_OR_EQUAL, false
            ))
            .withVertexFormat(
                com.mojang.blaze3d.vertex.DefaultVertexFormat.ENTITY, 
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS
            )
            .withCull(false)
            .build();
    private static final java.util.Map<net.minecraft.resources.Identifier, RenderType> RENDER_TYPE_CACHE = new java.util.HashMap<>();

    public static RenderType getCrackRenderType(net.minecraft.resources.Identifier destTex) {
        return RENDER_TYPE_CACHE.computeIfAbsent(destTex, tex -> RenderType.create(
            "zemlya_crack",
            net.minecraft.client.renderer.rendertype.RenderSetup.builder(CRACK_PIPELINE)
                .withTexture("Sampler0", tex)
                .createRenderSetup()
        ));
    }
    
    public static class CrackSegment {
        public float x1, z1, y1;
        public float x2, z2, y2;
        public float thickness;
    }
    
    public static class BlockCenterBeam {
        public int dx, dz;
        public float y;
        public float distance;
        public boolean isPlayerOnIt;
    }

    public static class FaceNode {
        public net.minecraft.core.BlockPos pos;
        public net.minecraft.core.Direction normal;
        public float centerU;
        public float centerV;
        public net.minecraft.core.Direction uAxis;
        public net.minecraft.core.Direction vAxis;
        public float distance;
    }

    public static class GravityFieldState extends EntityRenderState {
        public float tickCount;
        public Gravity fieldGravity;
        public float lifetime;
        public long entityId = -1;
        public net.minecraft.core.Direction landingDirection = net.minecraft.core.Direction.UP;
        
        public final List<CrackSegment> segments = new ArrayList<>();
        public final List<BlockCenterBeam> beams = new ArrayList<>();
        public final List<FaceNode> faces = new ArrayList<>();

        public float entityX, entityY, entityZ;
        public boolean hasPlayedStartSound = false;
        public boolean hasPlayedEndSound = false;
    }

    public GravityFieldRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public GravityFieldState createRenderState() {
        return new GravityFieldState();
    }

    @Override
    public void extractRenderState(GravityFieldEntity entity, GravityFieldState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.tickCount = entity.tickCount + tickDelta;
        state.fieldGravity = entity.getFieldGravity();
        state.lifetime = entity.getLifetime();
        state.landingDirection = entity.getLandingDirection();
        
        if (state.faces.isEmpty() || state.entityId != entity.getId()) {
            state.entityId = entity.getId();
            generateCache(entity, state);
        }

        if (entity.tickCount == 1 && !entity.hasPlayedClientStartSound) {
            entity.hasPlayedClientStartSound = true;
            net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                new com.example.client.sound.FadingSoundInstance(
                    net.minecraft.sounds.SoundEvents.WARDEN_DIG, net.minecraft.sounds.SoundSource.BLOCKS, 
                    1.0F, 0.8F, entity.getX(), entity.getY(), entity.getZ(), 40, 20
                )
            );
        }

        if (entity.tickCount >= entity.getLifetime() - 25 && !entity.hasPlayedClientEndSound) {
            entity.hasPlayedClientEndSound = true;
            net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                new com.example.client.sound.FadingSoundInstance(
                    net.minecraft.sounds.SoundEvents.WARDEN_DIG, net.minecraft.sounds.SoundSource.BLOCKS, 
                    1.0F, 0.6F, entity.getX(), entity.getY(), entity.getZ(), 40, 20
                )
            );
        }
    }

    private static net.minecraft.core.Direction rotateAxis(net.minecraft.core.Direction axisToRotate, 
                                                           net.minecraft.core.Direction stepDir, 
                                                           net.minecraft.core.Direction oldNormal, 
                                                           int cornerType) { 
        if (cornerType == 0) return axisToRotate;
        if (axisToRotate == stepDir) {
            return cornerType == 1 ? oldNormal : oldNormal.getOpposite();
        }
        if (axisToRotate == stepDir.getOpposite()) {
            return cornerType == 1 ? oldNormal.getOpposite() : oldNormal;
        }
        return axisToRotate;
    }

    private void generateCache(GravityFieldEntity entity, GravityFieldState state) {
        state.faces.clear();
        
        net.minecraft.world.level.Level level = entity.level();
        net.minecraft.core.BlockPos centerPos = entity.blockPosition();
        state.entityX = (float) entity.getX();
        state.entityY = (float) entity.getY();
        state.entityZ = (float) entity.getZ();

        net.minecraft.core.Direction hitFace = entity.getLandingDirection();
        
        net.minecraft.core.Direction uAxis, vAxis;
        if (hitFace.getAxis() == net.minecraft.core.Direction.Axis.Y) {
            uAxis = net.minecraft.core.Direction.EAST;
            vAxis = hitFace == net.minecraft.core.Direction.UP ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
        } else if (hitFace.getAxis() == net.minecraft.core.Direction.Axis.X) {
            uAxis = hitFace == net.minecraft.core.Direction.EAST ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
            vAxis = net.minecraft.core.Direction.DOWN;
        } else {
            uAxis = hitFace == net.minecraft.core.Direction.SOUTH ? net.minecraft.core.Direction.WEST : net.minecraft.core.Direction.EAST;
            vAxis = net.minecraft.core.Direction.DOWN;
        }

        net.minecraft.core.BlockPos startPos = centerPos;
        if (!isBlockSolid(level, startPos)) {
            startPos = centerPos.relative(hitFace.getOpposite());
            if (!isBlockSolid(level, startPos)) return;
        }

        record FaceKey(net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face) {}
        java.util.Set<FaceKey> visited = new java.util.HashSet<>();
        java.util.Queue<FaceNode> queue = new java.util.ArrayDeque<>();

        FaceNode startNode = new FaceNode();
        startNode.pos = startPos;
        startNode.normal = hitFace;
        startNode.centerU = 0.5f;
        startNode.centerV = 0.5f;
        startNode.uAxis = uAxis;
        startNode.vAxis = vAxis;
        startNode.distance = 0.0f;

        visited.add(new FaceKey(startPos, hitFace));
        queue.add(startNode);
        state.faces.add(startNode);

        float texScale = 0.1f;
        float maxDistance = 8.0f;

        while (!queue.isEmpty()) {
            FaceNode node = queue.poll();

            net.minecraft.core.Direction[] dirs = {node.uAxis, node.uAxis.getOpposite(), node.vAxis, node.vAxis.getOpposite()};
            
            for (net.minecraft.core.Direction stepDir : dirs) {
                net.minecraft.core.BlockPos emptyPos = node.pos.relative(node.normal).relative(stepDir);
                boolean emptySolid = isBlockSolid(level, emptyPos);
                boolean nextSolid = isBlockSolid(level, node.pos.relative(stepDir));
                
                net.minecraft.core.BlockPos newPos;
                net.minecraft.core.Direction newNormal;
                int cornerType = 0;
                
                if (emptySolid) {
                    newPos = emptyPos;
                    newNormal = stepDir.getOpposite();
                    cornerType = 1;
                } else if (nextSolid) {
                    newPos = node.pos.relative(stepDir);
                    newNormal = node.normal;
                    cornerType = 0;
                } else {
                    newPos = node.pos;
                    newNormal = stepDir;
                    cornerType = 2;
                }
                
                float newDist = node.distance + 1.0f;
                if (newDist > maxDistance) continue;
                
                FaceKey key = new FaceKey(newPos, newNormal);
                if (visited.add(key)) {
                    FaceNode nextNode = new FaceNode();
                    nextNode.pos = newPos;
                    nextNode.normal = newNormal;
                    nextNode.distance = newDist;
                    
                    nextNode.uAxis = rotateAxis(node.uAxis, stepDir, node.normal, cornerType);
                    nextNode.vAxis = rotateAxis(node.vAxis, stepDir, node.normal, cornerType);
                    
                    nextNode.centerU = node.centerU;
                    nextNode.centerV = node.centerV;
                    if (stepDir == node.uAxis) nextNode.centerU += texScale;
                    else if (stepDir == node.uAxis.getOpposite()) nextNode.centerU -= texScale;
                    else if (stepDir == node.vAxis) nextNode.centerV += texScale;
                    else if (stepDir == node.vAxis.getOpposite()) nextNode.centerV -= texScale;
                    
                    queue.add(nextNode);
                    state.faces.add(nextNode);
                }
            }
        }
        
        BlockCenterBeam dummy = new BlockCenterBeam();
        dummy.dx = 0;
        dummy.dz = 0;
        dummy.y = 0;
        dummy.distance = 0;
        state.beams.add(dummy);
    }



    private float calculateInteraction(float x, float y, float z, double px, double py, double pz) {
        double dx = x - px;
        double dy = y - py;
        double dz = z - pz;
        double distSqr = dx*dx + dy*dy + dz*dz;
        if (distSqr < 9.0) {
            return (float) (1.0 - Math.sqrt(distSqr) / 3.0);
        }
        return 0.0f;
    }

    private void addCrackGeometry(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face, float depth, Object os) {
        net.minecraft.world.level.block.state.BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir()) return;
        if (!blockState.getFluidState().isEmpty()) return;
        net.minecraft.world.phys.shapes.VoxelShape shape = blockState.getCollisionShape(level, pos);
    }

    private static boolean isBlockSolid(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir()) return false;
        if (!blockState.getFluidState().isEmpty()) return false;
        net.minecraft.world.phys.shapes.VoxelShape shape = blockState.getCollisionShape(level, pos);
        return !shape.isEmpty();
    }

    @Override
    public void submit(GravityFieldState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        float lifetime = state.lifetime <= 0 ? 220.0f : state.lifetime;

        float spreadingTicks = 25.0f;
        float decayTicks = 25.0f;
        float progress;
        
        if (state.tickCount < spreadingTicks) {
            progress = state.tickCount / spreadingTicks;
        } else {
            float remaining = lifetime - state.tickCount;
            if (remaining < decayTicks) {
                progress = remaining / decayTicks;
            } else {
                progress = 1.0f;
            }
        }

        int variant = (int) (Math.abs(state.entityId) % 8);
        int globalStage = Math.max(0, Math.min(7, Math.round(progress * 7.0f)));
        float waveRadius = progress * 8.0f;

        int alpha = 255;
        if (state.tickCount >= lifetime - decayTicks) {
            float remaining = lifetime - state.tickCount;
            alpha = (int) (Math.max(0.0f, Math.min(1.0f, remaining / decayTicks)) * 255.0f);
        }

        net.minecraft.resources.Identifier destTex = net.minecraft.resources.Identifier.fromNamespaceAndPath(
            "zemlya", "textures/block/unified_destroy_stage_" + variant + "_" + globalStage + ".png"
        );
        RenderType crackLayer = getCrackRenderType(destTex);

        float halfTex = 0.1f / 2.0f;
        int finalAlpha = alpha;
        
        net.minecraft.world.entity.player.Player player = net.minecraft.client.Minecraft.getInstance().player;
        double px = player != null ? player.getX() : 0;
        double py = player != null ? player.getY() : 0;
        double pz = player != null ? player.getZ() : 0;
        
        submitNodeCollector.submitCustomGeometry(poseStack, crackLayer, (matricesEntry, consumer) -> {
            for (FaceNode face : state.faces) {
                if (face.distance > waveRadius) continue;
                
                if (face.centerU < -0.05f || face.centerU > 1.05f || face.centerV < -0.05f || face.centerV > 1.05f) continue;

                float cx = face.pos.getX() + 0.5f + face.normal.getStepX() * 0.505f - state.entityX;
                float cy = face.pos.getY() + 0.5f + face.normal.getStepY() * 0.505f - state.entityY;
                float cz = face.pos.getZ() + 0.5f + face.normal.getStepZ() * 0.505f - state.entityZ;
                net.minecraft.world.phys.Vec3 center3D = new net.minecraft.world.phys.Vec3(cx, cy, cz);
                
                net.minecraft.world.phys.Vec3 v0 = center3D.add(getStep(face.uAxis, -0.5f)).add(getStep(face.vAxis, -0.5f));
                net.minecraft.world.phys.Vec3 v1 = center3D.add(getStep(face.uAxis, -0.5f)).add(getStep(face.vAxis, 0.5f));
                net.minecraft.world.phys.Vec3 v2 = center3D.add(getStep(face.uAxis, 0.5f)).add(getStep(face.vAxis, 0.5f));
                net.minecraft.world.phys.Vec3 v3 = center3D.add(getStep(face.uAxis, 0.5f)).add(getStep(face.vAxis, -0.5f));
                
                float u0 = face.centerU - halfTex; float v0_uv = face.centerV - halfTex;
                float u1 = face.centerU - halfTex; float v1_uv = face.centerV + halfTex;
                float u2 = face.centerU + halfTex; float v2_uv = face.centerV + halfTex;
                float u3 = face.centerU + halfTex; float v3_uv = face.centerV - halfTex;
                
                float distNorm = face.distance / 8.0f;
                if (distNorm > 1.0f) distNorm = 1.0f;
                float distFade = 1.0f - distNorm;
                
                int blockAlpha = (int) (finalAlpha * distFade);
                if (blockAlpha <= 0) continue;

                addVertex(matricesEntry, consumer, (float)v0.x, (float)v0.y, (float)v0.z, u0, v0_uv, blockAlpha, state.entityX, state.entityY, state.entityZ, px, py, pz, face.normal, 0.0f, 0.0f, distFade);
                addVertex(matricesEntry, consumer, (float)v1.x, (float)v1.y, (float)v1.z, u1, v1_uv, blockAlpha, state.entityX, state.entityY, state.entityZ, px, py, pz, face.normal, 0.0f, 0.0f, distFade);
                addVertex(matricesEntry, consumer, (float)v2.x, (float)v2.y, (float)v2.z, u2, v2_uv, blockAlpha, state.entityX, state.entityY, state.entityZ, px, py, pz, face.normal, 0.0f, 0.0f, distFade);
                addVertex(matricesEntry, consumer, (float)v3.x, (float)v3.y, (float)v3.z, u3, v3_uv, blockAlpha, state.entityX, state.entityY, state.entityZ, px, py, pz, face.normal, 0.0f, 0.0f, distFade);

                int glowLayers = 12;
                float maxHeight = 0.6f;
                for (int i = 1; i <= glowLayers; i++) {
                    float t = i / (float)glowLayers;
                    float height = t * maxHeight;
                    float layerAlpha = (1.0f - t) * (1.0f - t); 
                    int alphaLayer = (int) (blockAlpha * layerAlpha * 0.4f); 
                    
                    if (alphaLayer > 0) {
                        addVertex(matricesEntry, consumer, (float)v0.x, (float)v0.y, (float)v0.z, u0, v0_uv, alphaLayer, state.entityX, state.entityY, state.entityZ, px, py, pz, face.normal, height, t, distFade);
                        addVertex(matricesEntry, consumer, (float)v1.x, (float)v1.y, (float)v1.z, u1, v1_uv, alphaLayer, state.entityX, state.entityY, state.entityZ, px, py, pz, face.normal, height, t, distFade);
                        addVertex(matricesEntry, consumer, (float)v2.x, (float)v2.y, (float)v2.z, u2, v2_uv, alphaLayer, state.entityX, state.entityY, state.entityZ, px, py, pz, face.normal, height, t, distFade);
                        addVertex(matricesEntry, consumer, (float)v3.x, (float)v3.y, (float)v3.z, u3, v3_uv, alphaLayer, state.entityX, state.entityY, state.entityZ, px, py, pz, face.normal, height, t, distFade);
                    }
                }
            }
        });
        
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static net.minecraft.world.phys.Vec3 getStep(net.minecraft.core.Direction dir, float scale) {
        return new net.minecraft.world.phys.Vec3(dir.getStepX() * scale, dir.getStepY() * scale, dir.getStepZ() * scale);
    }
    
    private void addVertex(PoseStack.Pose matricesEntry, VertexConsumer consumer, 
                             float x, float y, float z, float u, float v, int a, 
                             float entityX, float entityY, float entityZ, 
                             double px, double py, double pz,
                             net.minecraft.core.Direction normal, float heightOffset, float t, float distFade) {
        
        float interaction = calculateInteraction(x + entityX, y + entityY, z + entityZ, px, py, pz);
        
        float distFromCenter = (float) Math.sqrt(x*x + y*y + z*z);
        float centerGlow = 1.0f - Math.max(0.0f, distFromCenter - 1.0f);
        if (centerGlow > 0.0f) {
            interaction = Math.max(interaction, centerGlow);
        }
        
        int r = (int) (Math.min(1.0f, interaction) * 255.0f);
        int g = (int) (t * 255.0f);
        int b = (int) (distFade * 255.0f);

        float nx = normal.getStepX() * heightOffset;
        float ny = normal.getStepY() * heightOffset;
        float nz = normal.getStepZ() * heightOffset;

        consumer.addVertex(matricesEntry.pose(), x + nx, y + ny, z + nz).setColor(r, g, b, a).setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(15728880).setNormal(matricesEntry, normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }

    @Override
    public boolean shouldRender(GravityFieldEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }
}
