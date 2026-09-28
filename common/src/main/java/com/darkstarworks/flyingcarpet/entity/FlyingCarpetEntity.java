package com.darkstarworks.flyingcarpet.entity;

import com.darkstarworks.flyingcarpet.ModContent;
import com.darkstarworks.flyingcarpet.config.FlyingCarpetConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Flying Carpet ride entity. Parked when landed, flies like a Happy Ghast
 * (no harness needed) when a player mounts.
 */
public class FlyingCarpetEntity extends Mob {

    // Happy Ghast baseline FLYING_SPEED is 0.05; we multiply by the config value.
    private static final double GHAST_FLYING_SPEED = 0.05D;

    private static final EntityDataAccessor<DyeColor> DATA_COLOR =
        SynchedEntityData.defineId(FlyingCarpetEntity.class, EntityDataSerializers.DYE_COLOR);
    // The server's speed setting, synced so the rider's client (which runs the movement)
    // can't fly faster or slower than the server allows.
    private static final EntityDataAccessor<Float> DATA_SPEED =
        SynchedEntityData.defineId(FlyingCarpetEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_DISMOUNT_OUTCOME =
        SynchedEntityData.defineId(FlyingCarpetEntity.class, EntityDataSerializers.BYTE);

    /** What happens if the rider gets off right now. */
    public enum DismountOutcome { KEEP, DROP_CUSHION, DROP_CARPET, HOVER }

    private static final long HIT_RESET_TICKS = 100L;
    private int hits;
    private long lastHitTick;

    public FlyingCarpetEntity(final EntityType<? extends FlyingCarpetEntity> type, final Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // The config multiplier is applied live in flightSpeed(), not baked in here.
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 8.0)
            .add(Attributes.FLYING_SPEED, GHAST_FLYING_SPEED)
            .add(Attributes.MOVEMENT_SPEED, GHAST_FLYING_SPEED)
            .add(Attributes.FOLLOW_RANGE, 16.0)
            .add(Attributes.CAMERA_DISTANCE, 8.0);
    }

    private double flightSpeed() {
        return this.getAttributeValue(Attributes.FLYING_SPEED) * this.entityData.get(DATA_SPEED);
    }

    public DismountOutcome getDismountOutcome() {
        byte b = this.entityData.get(DATA_DISMOUNT_OUTCOME);
        DismountOutcome[] values = DismountOutcome.values();
        return b >= 0 && b < values.length ? values[b] : DismountOutcome.KEEP;
    }

    private DismountOutcome computeDismountOutcome() {
        FlyingCarpetConfig cfg = FlyingCarpetConfig.get();
        if (cfg.safeRevertEnabled && this.hasSolidGroundWithin(cfg.safeDistance)) {
            return DismountOutcome.KEEP;
        }
        return switch (cfg.airDismount) {
            case DROP_CUSHION -> DismountOutcome.DROP_CUSHION;
            case DROP_CARPET -> DismountOutcome.DROP_CARPET;
            case HOVER -> DismountOutcome.HOVER;
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_SPEED, (float) FlyingCarpetConfig.get().flightSpeedMultiplier);
            if (this.isVehicle()) {
                this.entityData.set(DATA_DISMOUNT_OUTCOME, (byte) this.computeDismountOutcome().ordinal());
            }
        }
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
        builder.define(DATA_SPEED, 1.5F);
        builder.define(DATA_DISMOUNT_OUTCOME, (byte) 0);
    }

    // ---- Riding and picking up -------------------------------------------

    @Override
    public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
        if (this.isVehicle()) {
            return InteractionResult.PASS;
        }
        if (!player.isSecondaryUseActive()) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }
        if (FlyingCarpetConfig.get().sneakPickup) {
            if (this.level() instanceof ServerLevel) {
                player.getInventory().placeItemBackInInventory(this.carpetItem(), Prediction.SERVER_ONLY);
                this.playSound(SoundEvents.ITEM_PICKUP, 0.5F, 1.0F);
                this.discard();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // ---- Flight controls ---------------------------------------------------

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

    /** Eases the carpet's yaw toward where the rider looks. Mob.tickRidden alone never turns it. */
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
        this.confirmDeadline = now + FlyingCarpetConfig.get().confirmTicks();
        if (rider instanceof ServerPlayer serverPlayer) {
            String key = this.computeDismountOutcome() == DismountOutcome.KEEP
                ? "message.flyingcarpet.confirm_land"
                : "message.flyingcarpet.confirm_air";
            serverPlayer.sendSystemMessage(Component.translatable(key), true);
        }
        return false;
    }

    // ---- Getting off and breaking ----------------------------------------

    @Override
    protected void removePassenger(final Entity passenger) {
        super.removePassenger(passenger);
        if (this.getRemovalReason() != null || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        switch (this.computeDismountOutcome()) {
            case KEEP, HOVER -> { }
            case DROP_CUSHION -> {
                this.spawnAtLocation(serverLevel, this.plainCushion());
                this.discard();
            }
            case DROP_CARPET -> {
                this.spawnAtLocation(serverLevel, this.carpetItem());
                this.discard();
            }
        }
    }

    @Override
    public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
        if (this.isRemoved() || this.isInvulnerableToBase(source)) {
            return false;
        }
        long now = level.getGameTime();
        if (now - this.lastHitTick > HIT_RESET_TICKS) {
            this.hits = 0;
        }
        this.lastHitTick = now;
        this.hits++;

        boolean creative = source.getEntity() instanceof Player player && player.isCreative();
        if (!creative && this.hits < FlyingCarpetConfig.get().hitsToBreak) {
            this.hurtTime = 10;
            this.playSound(SoundEvents.WOOL_HIT, 1.0F, 1.0F);
            return true;
        }

        if (!creative) {
            switch (FlyingCarpetConfig.get().breakDrop) {
                case CUSHION -> this.spawnAtLocation(level, this.plainCushion());
                case CARPET -> this.spawnAtLocation(level, this.carpetItem());
                case NOTHING -> { }
            }
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WOOL_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        this.discard();
        return true;
    }

    private ItemStack plainCushion() {
        return new ItemStack(Items.CUSHION.pick(this.getColor()));
    }

    private ItemStack carpetItem() {
        Item item = ModContent.CARPET_ITEMS.get(this.getColor());
        return item != null ? new ItemStack(item) : this.plainCushion();
    }

    @Override
    public ItemStack getPickResult() {
        return this.carpetItem();
    }

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
