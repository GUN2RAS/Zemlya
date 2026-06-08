package com.example.entity;

import com.example.api.Gravity;
import com.example.api.GravityChanger;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;

import java.util.List;

import net.minecraft.network.syncher.EntityDataAccessor;


public class RiftEntity extends Entity {
    public static final EntityDataAccessor<Integer> SYNCED_AGE = SynchedEntityData.defineId(RiftEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> FIGHT_STATE = SynchedEntityData.defineId(RiftEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> WAVE_NUMBER = SynchedEntityData.defineId(RiftEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> WAVE_TIMER = SynchedEntityData.defineId(RiftEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    public static final int STATE_IDLE = 0;
    public static final int STATE_FIGHT = 1;
    public static final int STATE_IMPLODING = 2;
    public static final int STATE_DEAD = 3;

    private int lastRepelSoundAge = 0;
    private int attackCooldown = 0;
    
    public float clientRotation = 0.0f;
    public float currentSpinSpeed = 2.0f;

    private static class HoveringChunk {
        public java.util.List<net.minecraft.world.entity.item.FallingBlockEntity> blocks = new java.util.ArrayList<>();
        public java.util.List<net.minecraft.world.phys.Vec3> offsets = new java.util.ArrayList<>();
        public int phase = 0;
        public int timer = 0;
        public net.minecraft.world.phys.Vec3 hoverPos;
        public net.minecraft.world.phys.Vec3 currentPos;
        public net.minecraft.world.phys.Vec3 velocity = net.minecraft.world.phys.Vec3.ZERO;
        
        public HoveringChunk(net.minecraft.world.phys.Vec3 startPos, net.minecraft.world.phys.Vec3 hp) {
            this.currentPos = startPos;
            this.hoverPos = hp;
        }
    }
    private final java.util.List<HoveringChunk> hoveringChunks = new java.util.ArrayList<>();

    public RiftEntity(EntityType<? extends Entity> entityType, Level world) {
        super(entityType, world);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SYNCED_AGE, 0);
        builder.define(FIGHT_STATE, STATE_IDLE);
        builder.define(WAVE_NUMBER, 0);
        builder.define(WAVE_TIMER, 0);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput view) {
        this.tickCount = view.getIntOr("RiftAge", 0);
        this.lastRepelSoundAge = view.getIntOr("LastRepelSoundAge", 0);
        this.entityData.set(SYNCED_AGE, this.tickCount);
        this.entityData.set(FIGHT_STATE, view.getIntOr("FightState", STATE_IDLE));
        this.entityData.set(WAVE_NUMBER, view.getIntOr("WaveNumber", 0));
        this.entityData.set(WAVE_TIMER, view.getIntOr("WaveTimer", 0));
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput view) {
        view.putInt("RiftAge", this.tickCount);
        view.putInt("LastRepelSoundAge", this.lastRepelSoundAge);
        view.putInt("FightState", this.entityData.get(FIGHT_STATE));
        view.putInt("WaveNumber", this.entityData.get(WAVE_NUMBER));
        view.putInt("WaveTimer", this.entityData.get(WAVE_TIMER));
    }

    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        // Boss fight is triggered by proximity now, and progresses via waves on a timer.
        // We can just return false to make it invincible, or true to play the hit sound.
        // Returning false makes it clear it's a survival event.
        return false;
    }

    public boolean isPickable() {
        return true;
    }
    
    public boolean canCollideWith(Entity other) {
        return false;
    }
    
    public boolean isPushable() {
        return false;
    }

    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (SYNCED_AGE.equals(data)) {
            this.tickCount = this.entityData.get(SYNCED_AGE);
        }
    }

    @Override
    public void tick() {
        super.tick();

        int state = this.entityData.get(FIGHT_STATE);
        int wave = this.entityData.get(WAVE_NUMBER);
        int timer = this.entityData.get(WAVE_TIMER);

          if (state == STATE_IDLE) {
            currentSpinSpeed = 2.0f;
          } else if (state == STATE_FIGHT) {
            if (wave == 4) {
                // Phase 4: Transition (timer goes from 300 to 0)
                currentSpinSpeed = 35.0f; // Death spin speed
            } else {
                currentSpinSpeed = 8.0f;
            }
          } else if (state == STATE_IMPLODING) {
            currentSpinSpeed = 35.0f;
        } else {
            currentSpinSpeed = 0.0f;
        }
        
        clientRotation += currentSpinSpeed;

        if (this.level().isClientSide()) {
            if (this.tickCount == 100) {
                // com.example.world.EndIslandVisualManager.startReveal(this.getX(), this.getY(), this.getZ());
            }
        }

        if (!this.level().isClientSide()) {
            if (this.tickCount % 20 == 0) {
                this.entityData.set(SYNCED_AGE, this.tickCount);
            }
            System.out.println("Rift Entity Server Ticking! Age: " + this.tickCount);
            ServerLevel serverWorld = (ServerLevel) this.level();
            // T=0: Start Earthquake + sound
            if (this.tickCount == 1) {
                serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_EXPLODE,
                        SoundSource.HOSTILE, 10.0f, 0.5f);
                for (ServerPlayer player : serverWorld.players()) {
                    if (player.level() == this.level()) {

                    }
                }
            }

            // Continuous rumbling during shake phase (first 5s)
            if (this.tickCount % 20 == 0 && this.tickCount < 100) {
                serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.MINECART_RIDING,
                        SoundSource.HOSTILE, 3.0f, 0.5f);
            }

            // Rift visual: Pulsar core
            double cx = this.getX();
            double cy = this.getY();
            double cz = this.getZ();

            // Repel living entities that get too close to the pulsar
            AABB repelBox = this.getBoundingBox().inflate(10.0);
            List<Entity> nearbyEntities = serverWorld.getEntities(this, repelBox);
            net.minecraft.world.phys.Vec3 centerPos = new net.minecraft.world.phys.Vec3(this.getX(), this.getY(), this.getZ());
            boolean repelledAny = false;

            for (Entity e : nearbyEntities) {
                if (e instanceof LivingEntity) {
                    net.minecraft.world.phys.Vec3 ePos = new net.minecraft.world.phys.Vec3(e.getX(), e.getY(), e.getZ());
                    double dist = centerPos.distanceTo(ePos);
                    if (dist < 8.5) { // Pulsar radius is ~7, so 8.5 is right at the edge
                        net.minecraft.world.phys.Vec3 pushDir = ePos.subtract(centerPos).normalize();
                        // Strong push outwards and slightly upwards (throws back 5-10 blocks)
                        e.addDeltaMovement(new net.minecraft.world.phys.Vec3(pushDir.x * 4.0, pushDir.y * 4.0 + 1.0, pushDir.z * 4.0));
                        e.hurtMarked = true;
                        
                        if (e instanceof ServerPlayer player) {
                            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
                        }
                        repelledAny = true;
                    }
                }
            }

            // Repel from Exit Portal and trigger flip
            if (this.tickCount < 400) {
                for (ServerPlayer player : serverWorld.players()) {
                    if (player.level() == this.level()) {
                        double px = player.getX();
                        double py = player.getY();
                        double pz = player.getZ();
                        // Exit portal is at 0, ~65, 0. Check radius 10, Y between 50 and 80.
                        if (px * px + pz * pz < 100 && py > 50 && py < 80) {
                            // Repel ~30 blocks away
                            net.minecraft.world.phys.Vec3 pushDir = new net.minecraft.world.phys.Vec3(px, 0, pz).normalize();
                            if (pushDir.lengthSqr() < 0.01) {
                                pushDir = new net.minecraft.world.phys.Vec3(1, 0, 0);
                            }
                            player.addDeltaMovement(new net.minecraft.world.phys.Vec3(pushDir.x * 5.0, 1.5, pushDir.z * 5.0));
                            player.hurtMarked = true;
                            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
                            
                            // Stop shake if it was still shaking
                            if (this.tickCount < 100) {
                                for (ServerPlayer p : serverWorld.players()) {
                                    if (p.level() == this.level()) {

                                    }
                                }
                            }
                            
                            // Trigger flip immediately
                            this.tickCount = 399;
                            this.entityData.set(SYNCED_AGE, this.tickCount);
                            
                            repelledAny = true;
                        }
                    }
                }
            }

            if (repelledAny && this.tickCount - lastRepelSoundAge > 20) {
                serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,
                        net.minecraft.sounds.SoundSource.HOSTILE, 3.0f, 1.0f);
                lastRepelSoundAge = this.tickCount;
            }

            // T=5s (100 ticks): Stop shake!
            if (this.tickCount == 100) {
                for (ServerPlayer player : serverWorld.players()) {
                    if (player.level() == this.level()) {

                    }
                }
            }

            // T=20s (400 ticks): Flip gravity, teleport players, big boom, start clearing
            // fog
            if (this.tickCount == 400) {

                AABB box = this.getBoundingBox().inflate(200.0);
                List<Entity> entities = this.level().getEntities(this, box);

                for (Entity e : entities) {
                    if (e instanceof GravityChanger changer) {
                        changer.setGravity(Gravity.UP);
                    }
                    if (e instanceof LivingEntity living) {
                        living.setInvulnerable(true);
                    }
                }

                serverWorld.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, this.getX(),
                        this.getY(), this.getZ(), 5, 0.0, 0.0, 0.0, 0.0);
                serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,
                        net.minecraft.sounds.SoundSource.HOSTILE, 15.0f, 0.5f);
            }

            // T=25s (500 ticks): Remove invulnerability
            if (this.tickCount == 500) {
                for (ServerPlayer player : serverWorld.players()) {
                    if (player.level() == this.level()) {
                        player.setInvulnerable(false);
                    }
                }

                AABB box = this.getBoundingBox().inflate(200.0);
                for (Entity e : this.level().getEntities(this, box)) {
                    if (e instanceof LivingEntity living && !(e instanceof ServerPlayer)) {
                        living.setInvulnerable(false);
                    }
                }

                // Removed this.discard() to make the black hole permanent
            }

            // Boss Fight Logic
            if (state == STATE_IDLE && this.tickCount > 500) {
                // Trigger fight if player is within 32 blocks (2 chunks)
                List<ServerPlayer> closePlayers = serverWorld.getPlayers(p -> p.distanceToSqr(this) < 1024);
                if (!closePlayers.isEmpty()) {
                    this.entityData.set(FIGHT_STATE, STATE_FIGHT);
                    this.entityData.set(WAVE_NUMBER, 0); // Phase 0: Enderman Purge
                    this.entityData.set(WAVE_TIMER, 100); // 5 seconds
                    serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                            net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                            net.minecraft.sounds.SoundSource.HOSTILE, 5.0f, 0.8f);
                }
            } else if (state == STATE_FIGHT) {
                if (timer > 0) {
                    this.entityData.set(WAVE_TIMER, timer - 1);
                    
                    if (wave == 0) {
                        // Enderman Purge: Massive burst on tick 99
                        if (timer == 99) {
                            List<net.minecraft.world.entity.monster.EnderMan> endermen = serverWorld.getEntitiesOfClass(
                                net.minecraft.world.entity.monster.EnderMan.class, 
                                this.getBoundingBox().inflate(150.0), 
                                e -> true
                            );
                            for (net.minecraft.world.entity.monster.EnderMan target : endermen) {
                                strikeLightning(serverWorld, target);
                                target.hurtServer(serverWorld, serverWorld.damageSources().magic(), 1000.0f);
                                target.kill(serverWorld);
                            }
                            serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.HOSTILE, 10.0f, 0.5f);
                        }
                    } else if (wave == 1) {
                        // Removed Minions
                        if (attackCooldown > 0) {
                            attackCooldown--;
                        } else {
                            attackCooldown = 40;
                        }
                    } else if (wave == 2) {
                        // Block Chunks
                        if (timer % 20 == 0 && timer > 100) { // Don't spawn in last 5 seconds
                            ripChunk(serverWorld);
                        }
                        updateHoveringChunks(serverWorld);
                      } else if (wave == 3) {
                          // Lightning
                          // Randomize lightning strikes instead of exact 20 tick cooldown
                          if (this.random.nextInt(20) == 0) {
                              ServerPlayer p = getRandomPlayer(serverWorld);
                              if (p != null) strikeLightning(serverWorld, p);
                          }
                        } else if (wave == 4) {
                            // Transition phase (15 seconds total / 300 ticks)
                            
                            // 1. First 10 seconds (ticks 300 to 100): Lift players up by 20 blocks
                            if (timer > 100) {
                                for (ServerPlayer player : serverWorld.players()) {
                                    if (player.distanceToSqr(this) < 10000) {
                                        // Lift player up gently (0.05 is enough to overcome gravity and float up)
                                        player.addDeltaMovement(new net.minecraft.world.phys.Vec3(0, 0.05, 0));
                                        player.hurtMarked = true;
                                    }
                                }
                                
                                // Visual effect: Fake chunk plates flying outwards
                                if (timer % 20 == 0) { // Every second
                                    int plates = 2; // 2 plates per second
                                    
                                    for (int i = 0; i < plates; i++) {
                                        double angle = Math.toRadians(this.random.nextDouble() * 360.0);
                                        double dist = 25.0 + this.random.nextDouble() * 10.0; // Start at edges of island
                                        
                                        int plateCx = (int) (this.getX() + Math.cos(angle) * dist);
                                        int plateCz = (int) (this.getZ() + Math.sin(angle) * dist);
                                        int plateCy = (int) (this.getY() + 10 + this.random.nextInt(15)); 
                                        
                                        net.minecraft.world.phys.Vec3 plateCenterPos = new net.minecraft.world.phys.Vec3(plateCx, plateCy, plateCz);
                                        net.minecraft.world.phys.Vec3 bossPos = new net.minecraft.world.phys.Vec3(this.getX(), this.getY(), this.getZ());
                                        // Fly outwards and slightly upwards
                                        net.minecraft.world.phys.Vec3 outwardVel = plateCenterPos.subtract(bossPos).normalize().scale(0.4 + this.random.nextDouble() * 0.2).add(0, 0.1, 0);
                                        
                                        // Spawn a fake plate (e.g. 5x2x5) using FallingBlockEntity so it actually moves
                                        for (int x = -2; x <= 2; x++) {
                                            for (int y = 0; y <= 1; y++) {
                                                for (int z = -2; z <= 2; z++) {
                                                    // Make it slightly irregular
                                                    if (this.random.nextFloat() > 0.8f) continue;
                                                    
                                                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(plateCx + x, plateCy + y, plateCz + z);
                                                    
                                                    // Spawn falling block entity without breaking the world
                                                    net.minecraft.world.entity.item.FallingBlockEntity fallingBlock = net.minecraft.world.entity.EntityType.FALLING_BLOCK.create(serverWorld, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                                                    if (fallingBlock != null) {
                                                        // Set block state via NBT
                                                        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
                                                        nbt.put("BlockState", net.minecraft.nbt.NbtUtils.writeBlockState(net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState()));
                                                        nbt.putInt("Time", 1); // Must be > 0 so it doesn't despawn immediately
                                                        nbt.putBoolean("DropItem", false);
                                                        
                                                        net.minecraft.util.ProblemReporter.Collector reporter = new net.minecraft.util.ProblemReporter.Collector();
                                                        fallingBlock.load(net.minecraft.world.level.storage.TagValueInput.create(reporter, serverWorld.registryAccess(), nbt));
                                                        
                                                        fallingBlock.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                                                        fallingBlock.setNoGravity(true);
                                                        fallingBlock.setDeltaMovement(outwardVel);
                                                        fallingBlock.hurtMarked = true;
                                                        serverWorld.addFreshEntity(fallingBlock);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                  } else {
                    // Next wave or win
                    if (wave == 0) {
                        this.entityData.set(WAVE_NUMBER, 1);
                        this.entityData.set(WAVE_TIMER, 600); // 30s
                        serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN, net.minecraft.sounds.SoundSource.HOSTILE, 5.0f, 1.0f);
                    } else if (wave == 1) {
                        this.entityData.set(WAVE_NUMBER, 2);
                        this.entityData.set(WAVE_TIMER, 400); // 20s
                        serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN, net.minecraft.sounds.SoundSource.HOSTILE, 5.0f, 1.0f);
                    } else if (wave == 2) {
                        this.entityData.set(WAVE_NUMBER, 3);
                        this.entityData.set(WAVE_TIMER, 300); // 15s
                        serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.HOSTILE, 5.0f, 1.0f);
                      } else if (wave == 3) {
                          this.entityData.set(WAVE_NUMBER, 4);
                          this.entityData.set(WAVE_TIMER, 300); // 15 seconds for transition
                          serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.HOSTILE, 5.0f, 1.0f);
                      } else if (wave >= 4) {
                        this.entityData.set(FIGHT_STATE, STATE_IMPLODING);
                        this.entityData.set(WAVE_TIMER, 100); // 5 seconds implosion
                        serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                                net.minecraft.sounds.SoundEvents.ENDER_DRAGON_DEATH,
                                net.minecraft.sounds.SoundSource.HOSTILE, 10.0f, 0.5f);
                    }
                }
            } else if (state == STATE_IMPLODING) {
                if (timer > 0) {
                    this.entityData.set(WAVE_TIMER, timer - 1);
                    // Implosion effects
                    if (timer % 5 == 0) {
                        spawnForcedParticles(serverWorld, (net.minecraft.core.particles.ParticleOptions)net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(), this.getZ(), 100, 5.0, 5.0, 5.0, 0.5);
                        serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                                net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                                net.minecraft.sounds.SoundSource.HOSTILE, 3.0f, 2.0f);
                    }
                } else {
                    this.entityData.set(FIGHT_STATE, STATE_DEAD);
                    
                    // Big explosion effect
                    serverWorld.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, this.getX(),
                            this.getY(), this.getZ(), 20, 0.0, 0.0, 0.0, 0.0);
                    serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                            net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                            net.minecraft.sounds.SoundSource.HOSTILE, 15.0f, 0.5f);
                            
                    // Drop item
                    net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(serverWorld, this.getX(), this.getY(), this.getZ(), new net.minecraft.world.item.ItemStack(com.example.ExampleMod.SINGULARITY_CORE));
                    // Set gravity of the dropped item to UP
                    if (drop instanceof com.example.api.GravityChanger changer) {
                        changer.setGravity(com.example.api.Gravity.UP);
                    }
                    serverWorld.addFreshEntity(drop);
                    
                    this.discard();
                }
            }
        }
    }

    private ServerPlayer getRandomPlayer(ServerLevel world) {
        List<ServerPlayer> players = world.getPlayers(p -> p.distanceToSqr(this) < 10000);
        if (players.isEmpty()) return null;
        return players.get(this.random.nextInt(players.size()));
    }

    private void ripChunk(ServerLevel world) {
        double angle = this.random.nextDouble() * Math.PI * 2;
        double radius = 15.0 + this.random.nextDouble() * 15.0;
        double px = this.getX() + Math.cos(angle) * radius;
        double pz = this.getZ() + Math.sin(angle) * radius;
        
        int blockX = (int) Math.floor(px);
        int blockZ = (int) Math.floor(pz);
        
        // Find surface around the cube's Y level
        int startY = (int) this.getY() - 10;
        int endY = (int) this.getY() + 80;
        int surfaceY = -1;
        
        for (int y = startY; y < endY; y++) {
            if (!world.getBlockState(new net.minecraft.core.BlockPos(blockX, y, blockZ)).isAir()) {
                surfaceY = y;
                break;
            }
        }
        
        if (surfaceY == -1) return; // No block found
        
        net.minecraft.core.BlockPos startPos = new net.minecraft.core.BlockPos(blockX, surfaceY, blockZ);
        java.util.List<net.minecraft.core.BlockPos> toRip = new java.util.ArrayList<>();
        java.util.List<net.minecraft.core.BlockPos> queue = new java.util.ArrayList<>();
        queue.add(startPos);
        
        int targetSize = 5 + this.random.nextInt(3); // 5 to 7 blocks
        
        while (!queue.isEmpty() && toRip.size() < targetSize) {
            net.minecraft.core.BlockPos p = queue.remove(0);
            if (!toRip.contains(p)) {
                net.minecraft.world.level.block.state.BlockState state = world.getBlockState(p);
                if (!state.isAir() && state.getFluidState().isEmpty()) {
                    toRip.add(p);
                    // Add neighbors
                    java.util.List<net.minecraft.core.BlockPos> neighbors = new java.util.ArrayList<>();
                    neighbors.add(p.above());
                    neighbors.add(p.below());
                    neighbors.add(p.north());
                    neighbors.add(p.south());
                    neighbors.add(p.east());
                    neighbors.add(p.west());
                    java.util.Collections.shuffle(neighbors);
                    queue.addAll(neighbors);
                }
            }
        }
        
        if (toRip.isEmpty()) return;
        
        // Calculate center
        double cx = 0, cy = 0, cz = 0;
        for (net.minecraft.core.BlockPos p : toRip) {
            cx += p.getX(); cy += p.getY(); cz += p.getZ();
        }
        cx /= toRip.size(); cy /= toRip.size(); cz /= toRip.size();
        net.minecraft.world.phys.Vec3 center = new net.minecraft.world.phys.Vec3(cx + 0.5, cy + 0.5, cz + 0.5);
        
        // Calculate hover pos further away from the cube (radius 15-25)
        double hoverAngle = this.random.nextDouble() * Math.PI * 2;
        double hoverRadius = 15.0 + this.random.nextDouble() * 10.0;
        double hoverY = (this.random.nextDouble() - 0.5) * 20.0;
        net.minecraft.world.phys.Vec3 hoverPos = new net.minecraft.world.phys.Vec3(
            Math.cos(hoverAngle) * hoverRadius,
            hoverY,
            Math.sin(hoverAngle) * hoverRadius
        );
        
        HoveringChunk chunk = new HoveringChunk(center, hoverPos);
        
        for (net.minecraft.core.BlockPos p : toRip) {
            net.minecraft.world.level.block.state.BlockState bState = world.getBlockState(p);
            world.destroyBlock(p, false);
            
            net.minecraft.world.entity.item.FallingBlockEntity fallingBlock = net.minecraft.world.entity.item.FallingBlockEntity.fall(world, p, bState);
            fallingBlock.dropItem = false;
            fallingBlock.setNoGravity(true);
            fallingBlock.time = 1; // Keep it alive
            
            chunk.blocks.add(fallingBlock);
            chunk.offsets.add(new net.minecraft.world.phys.Vec3(p.getX() + 0.5 - center.x, p.getY() + 0.5 - center.y, p.getZ() + 0.5 - center.z));
        }
        
        hoveringChunks.add(chunk);
    }

    private void updateHoveringChunks(ServerLevel world) {
        java.util.Iterator<HoveringChunk> it = hoveringChunks.iterator();
        while(it.hasNext()) {
            HoveringChunk hc = it.next();
            
            hc.blocks.removeIf(Entity::isRemoved);
            if (hc.blocks.isEmpty()) {
                it.remove();
                continue;
            }

            if (hc.phase == 0) {
                // Pull to cube
                net.minecraft.world.phys.Vec3 target = new net.minecraft.world.phys.Vec3(this.getX(), this.getY(), this.getZ()).add(hc.hoverPos);
                net.minecraft.world.phys.Vec3 dir = target.subtract(hc.currentPos);
                if (dir.lengthSqr() < 4.0) {
                    hc.phase = 1;
                    hc.velocity = net.minecraft.world.phys.Vec3.ZERO;
                } else {
                    hc.velocity = dir.normalize().scale(1.5);
                }
                hc.currentPos = hc.currentPos.add(hc.velocity);
            } else if (hc.phase == 1) {
                // Hover
                hc.timer++;
                hc.velocity = net.minecraft.world.phys.Vec3.ZERO;
                hc.currentPos = new net.minecraft.world.phys.Vec3(this.getX(), this.getY(), this.getZ()).add(hc.hoverPos);
                
                if (hc.timer > 20) {
                    hc.phase = 2;
                    ServerPlayer player = getRandomPlayer(world);
                    if (player != null) {
                        net.minecraft.world.phys.Vec3 pPos = new net.minecraft.world.phys.Vec3(player.getX(), player.getY(), player.getZ()).add(0, player.getBbHeight() / 2.0, 0);
                        net.minecraft.world.phys.Vec3 shootDir = pPos.subtract(hc.currentPos).normalize();
                        hc.velocity = shootDir.scale(2.5);
                    } else {
                        hc.velocity = new net.minecraft.world.phys.Vec3(0, -1, 0);
                    }
                }
            } else if (hc.phase == 2) {
                // Shooting at player
                hc.timer++;
                hc.currentPos = hc.currentPos.add(hc.velocity);
                
                // Check collision with players
                boolean hit = false;
                for (ServerPlayer p : world.players()) {
                    if (p.distanceToSqr(hc.currentPos) < 16.0) { // 4 block radius for chunk
                        p.hurtServer(world, world.damageSources().magic(), 6.0f); // 3 hearts damage
                        hit = true;
                        break;
                    }
                }
                
                // Remove if it hits player or after 100 ticks (5 seconds)
                if (hit || hc.timer > 120) {
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, hc.currentPos.x, hc.currentPos.y, hc.currentPos.z, 5, 1.0, 1.0, 1.0, 0.0);
                    for (var b : hc.blocks) b.discard();
                    it.remove();
                    continue;
                }
            }
            
            // Update block positions and velocities
            for (int i = 0; i < hc.blocks.size(); i++) {
                var b = hc.blocks.get(i);
                var offset = hc.offsets.get(i);
                b.time = 1; // keep alive
                
                if (hc.phase == 0 || hc.phase == 2) {
                    // Moving: use velocity for smooth interpolation
                    b.setDeltaMovement(hc.velocity);
                    b.hurtMarked = true;
                } else {
                    // Hovering: snap to exact position relative to cube
                    b.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    b.hurtMarked = true;
                    b.setPos(hc.currentPos.x + offset.x, hc.currentPos.y + offset.y, hc.currentPos.z + offset.z);
                }
            }
        }
    }

    private void damagePlayersWithBeams(ServerLevel world) {
        float phaseTime = 300 - this.entityData.get(WAVE_TIMER);
        if (phaseTime < 80) return;
        
        float yOff = 0;
        float angleOffset = 45.0f;
        if (phaseTime > 40 && phaseTime <= 80) {
            angleOffset = 45.0f * (1.0f - (phaseTime - 40) / 40.0f);
        } else if (phaseTime > 80) {
            angleOffset = 0.0f;
            yOff = (float) Math.sin((phaseTime - 80) * 0.1) * 60.0f; // Sweep up and down by 60 blocks
        }
        
        net.minecraft.world.phys.Vec3 center = new net.minecraft.world.phys.Vec3(this.getX(), this.getY(), this.getZ()).add(0, yOff, 0);
        
        for (ServerPlayer p : world.players()) {
            if (p.distanceToSqr(center) > 10000) continue;
            
            for (int i=0; i<4; i++) {
                float angle = this.clientRotation + i * 90.0f + angleOffset;
                net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(Math.cos(Math.toRadians(angle)), 0, Math.sin(Math.toRadians(angle)));
                
                net.minecraft.world.phys.Vec3 pVec = new net.minecraft.world.phys.Vec3(p.getX(), p.getY(), p.getZ()).add(0, p.getBbHeight()/2.0, 0).subtract(center);
                double projection = pVec.dot(dir);
                if (projection > 0) {
                    net.minecraft.world.phys.Vec3 closest = dir.scale(projection);
                    double distSq = pVec.subtract(closest).lengthSqr();
                    if (distSq < 3.0 * 3.0) { // 3 block radius
                        p.hurtServer(world, world.damageSources().magic(), 1.0f); // 0.5 hearts
                    }
                }
            }
        }
    }

    private net.minecraft.world.phys.Vec3 getHoleCoordinates(int index) {
        // The End spikes are always generated at a distance of 42 from the center (0,0)
        double angle = 2.0 * (-Math.PI + 0.1 * Math.PI * index);
        double px = Math.floor(42.0 * Math.cos(angle));
        double pz = Math.floor(42.0 * Math.sin(angle));
        return new net.minecraft.world.phys.Vec3(px, 0, pz);
    }


    private void spawnMinion(ServerLevel world, ServerPlayer target) {
        // Disabled minion spawning completely
    }

    private void strikeLightning(ServerLevel world, LivingEntity target) {
        System.out.println("STRIKING LIGHTNING AT TARGET!");
        net.minecraft.world.phys.Vec3 startPos = new net.minecraft.world.phys.Vec3(this.getX(), this.getY(), this.getZ());
        net.minecraft.world.phys.Vec3 targetPos = new net.minecraft.world.phys.Vec3(target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ());
        
        RiftLightningEntity lightning = ModEntities.RIFT_LIGHTNING.create(world, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (lightning != null) {
            lightning.setPos(startPos.x, startPos.y, startPos.z);
            lightning.setTarget(targetPos);
            world.addFreshEntity(lightning);
        }
        
        // Apply damage directly
        target.hurtServer(world, world.damageSources().magic(), 6.0f);
        
        // Play thunder sound at the cube and the target
        world.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.HOSTILE, 5.0f, 2.0f);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, net.minecraft.sounds.SoundSource.HOSTILE, 2.0f, 2.0f);
    }

    private <T extends net.minecraft.core.particles.ParticleOptions> void spawnForcedParticles(
            net.minecraft.server.level.ServerLevel world, T particle, double x, double y, double z, int count,
            double dx, double dy, double dz, double speed) {
        for (net.minecraft.server.level.ServerPlayer player : world.players()) {
            world.sendParticles(player, particle, true, true, x, y, z, count, dx, dy, dz, speed);
        }
    }
}
