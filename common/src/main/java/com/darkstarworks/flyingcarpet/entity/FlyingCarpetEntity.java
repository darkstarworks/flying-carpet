package com.darkstarworks.flyingcarpet.entity;

import com.darkstarworks.flyingcarpet.config.FlyingCarpetConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Flying Carpet ride entity. Parked when landed, flies when a player mounts.
 *
 * <p>Flight overrides ({@link #travel}, {@link #getRiddenInput}, {@link #getControllingPassenger})
 * mirror the vanilla Happy Ghast, but the "is controlled" condition is simply
 * "a player is riding" (no harness needed), and the speed is scaled by the
 * config's {@code flightSpeedMultiplier} so it flies faster than a Happy Ghast.</p>
 */
public class FlyingCarpetEntity extends Mob {

    // Happy Ghast baseline FLYING_SPEED is 0.05; we multiply by the config value.
    private static final double GHAST_FLYING_SPEED = 0.05D;

    private static final EntityDataAccessor<DyeColor> DATA_COLOR =
        SynchedEntityData.defineId(FlyingCarpetEntity.class, EntityDataSerializers.DYE_COLOR);

    public FlyingCarpetEntity(final EntityType<? extends FlyingCarpetEntity> type, final Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Base attribute stays at the Happy Ghast value; the config multiplier is
        // applied LIVE in the flight math (see flightSpeed()) so changing it in the
        // settings screen takes effect immediately without a restart or re-register.
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 8.0)
            .add(Attributes.FLYING_SPEED, GHAST_FLYING_SPEED)
            .add(Attributes.MOVEMENT_SPEED, GHAST_FLYING_SPEED)
            .add(Attributes.FOLLOW_RANGE, 16.0)
            .add(Attributes.CAMERA_DISTANCE, 8.0);
    }

    /** Effective flying speed = base attribute × the live config multiplier. */
    private double flightSpeed() {
        return this.getAttributeValue(Attributes.FLYING_SPEED) * FlyingCarpetConfig.get().flightSpeedMultiplier;
    }

    public DyeColor getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setColor(final DyeColor color) {
        this.entityData.set(DATA_COLOR, color);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLOR, DyeColor.WHITE);
    }

    // ---- Riding: sit by mounting, sneak handled in dismount task ----------

    @Override
    public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        if (!player.isSecondaryUseActive() && !this.isVehicle()) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // ---- Flight controls (verified against HappyGhast 26.3) ----------------

    @Override
    public void travel(final Vec3 input) {
        float speed = (float) this.flightSpeed() * 5.0F / 3.0F;
        this.travelFlying(input, speed, speed, speed);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
    }

    @Override
    protected Vec3 getRiddenInput(final Player controller, final Vec3 selfInput) {
        float strafe = controller.xxa;
        float forward = 0.0F;
        float up = 0.0F;
        if (controller.zza != 0.0F) {
            float forwardLook = Mth.cos(controller.getXRot() * (float) (Math.PI / 180.0));
            float upLook = -Mth.sin(controller.getXRot() * (float) (Math.PI / 180.0));
            if (controller.zza < 0.0F) {
                forwardLook *= -0.5F;
                upLook *= -0.5F;
            }
            up = upLook;
            forward = forwardLook;
        }
        if (controller.isJumping()) {
            up += 0.5F;
        }
        return new Vec3(strafe, up, forward).scale(3.9F * this.flightSpeed());
    }

    protected Vec2 getRiddenRotation(final LivingEntity controller) {
        return new Vec2(controller.getXRot() * 0.5F, controller.getYRot());
    }

    /**
     * Applies steering: eases the carpet's yaw toward the rider's view yaw (and
     * pitch toward half the rider's pitch) at 8% per tick — the same smooth
     * "look-to-turn" feel as a Happy Ghast. Without this override the default
     * {@code Mob.tickRidden} is empty and the carpet never turns.
     */
    @Override
    protected void tickRidden(final Player controller, final Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        Vec2 rotation = this.getRiddenRotation(controller);
        float yRot = this.getYRot();
        float diff = Mth.wrapDegrees(rotation.y - yRot);
        yRot += diff * 0.08F;
        this.setRot(yRot, rotation.x);
        this.yRotO = this.yBodyRot = this.yHeadRot = yRot;
    }

    // ---- Housekeeping ------------------------------------------------------

    @Override
    protected void checkFallDamage(final double ya, final boolean onGround, final net.minecraft.world.level.block.state.BlockState onState, final BlockPos pos) {
        // Never take fall damage — it's a carpet.
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("color", DyeColor.CODEC, this.getColor());
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setColor(input.read("color", DyeColor.CODEC).orElse(DyeColor.WHITE));
    }

    // ---- Sneak-again-to-confirm dismount ----------------------------------

    private transient boolean awaitingConfirm = false;
    private transient long confirmDeadline = 0L;
    private transient long lastSneakTick = Long.MIN_VALUE;

    /**
     * Called (server-side, while the rider is sneaking) to decide whether the
     * sneak should actually dismount. Returns {@code true} only on a confirming
     * second sneak within the window; the first sneak just arms the prompt.
     *
     * <p>Edge detection: this is polled every tick the sneak key is held, so we
     * treat a gap of &gt;1 tick since the last call as a fresh key press (the
     * player released and pressed sneak again).</p>
     */
    public boolean tryConfirmDismount(final Player rider) {
        long now = this.level().getGameTime();
        boolean freshPress = (now - this.lastSneakTick) > 1L;
        this.lastSneakTick = now;

        if (this.awaitingConfirm && now <= this.confirmDeadline) {
            if (freshPress) {
                this.awaitingConfirm = false;
                return true; // confirmed -> allow dismount
            }
            return false; // still holding the first press
        }

        // Arm a new confirmation attempt and prompt the rider.
        this.awaitingConfirm = true;
        this.confirmDeadline = now + FlyingCarpetConfig.get().confirmWindowTicks;
        if (rider instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                Component.literal("Sneak again to drop the Flying Carpet"), true);
        }
        return false;
    }

    // ---- Dismount / break: safe-revert vs plain-cushion drop --------------

    /**
     * Sneak-dismount branching:
     * <ul>
     *   <li><b>Safe</b> (solid ground within {@code safeDistance} below, and
     *       revert enabled): the carpet stays parked as a Flying Carpet — we do
     *       nothing, the entity persists and can be re-mounted.</li>
     *   <li><b>Air / unsafe</b>: the carpet is "dropped" — the entity is removed
     *       and a plain (non-enchanted) cushion of the matching color drops. The
     *       player falls, because the carpet has no solid platform on top.</li>
     * </ul>
     */
    @Override
    protected void removePassenger(final Entity passenger) {
        super.removePassenger(passenger);
        if (this.level().isClientSide() || this.getRemovalReason() != null) {
            return;
        }
        FlyingCarpetConfig cfg = FlyingCarpetConfig.get();
        boolean safe = cfg.safeRevertEnabled && this.hasSolidGroundWithin(cfg.safeDistance);
        if (!safe && this.level() instanceof ServerLevel serverLevel) {
            this.dropPlainCushion(serverLevel);
            this.discard();
        }
    }

    /** Breaking the carpet (any lethal-ish hit) drops it as a plain cushion. */
    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
        boolean result = super.hurtServer(level, source, damage);
        if (this.isAlive() && !this.isRemoved()) {
            this.dropPlainCushion(level);
            this.discard();
        }
        return result;
    }

    private void dropPlainCushion(final ServerLevel level) {
        // Plain cushion of the matching color — loses the enchanted "carpet" upgrade.
        this.spawnAtLocation(level, Items.CUSHION.pick(this.getColor()));
    }

    /** True if any block with a non-empty collision shape sits within {@code distance} below. */
    private boolean hasSolidGroundWithin(final double distance) {
        BlockPos base = this.blockPosition();
        int steps = (int) Math.ceil(distance);
        for (int d = 1; d <= steps; d++) {
            BlockPos pos = base.below(d);
            if (!this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
