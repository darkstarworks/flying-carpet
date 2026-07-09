package com.darkstarworks.flyingcarpet.item;

import com.darkstarworks.flyingcarpet.ModContent;
import com.darkstarworks.flyingcarpet.entity.FlyingCarpetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Colored Flying Carpet item (one instance per DyeColor). Mirrors vanilla
 * {@code CushionItem}: right-click the top face of a block to place the flying
 * carpet entity in this item's color. Carries an enchantment glint (set on its
 * Item.Properties at registration).
 */
public class FlyingCarpetItem extends Item {

    private final DyeColor color;

    public FlyingCarpetItem(final Item.Properties properties, final DyeColor color) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos blockPos = placeContext.getClickedPos();
        Vec3 entityPos = Vec3.atCenterOfWithY(blockPos, context.getClickLocation().y);

        ItemStack stack = context.getItemInHand();
        if (level instanceof ServerLevel serverLevel) {
            FlyingCarpetEntity carpet = ModContent.FLYING_CARPET.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
            if (carpet == null) {
                return InteractionResult.FAIL;
            }
            carpet.snapTo(entityPos, Direction.fromYRot(placeContext.getRotation()).toYRot(), 0.0F);
            carpet.setColor(this.color);
            serverLevel.addFreshEntity(carpet);
            level.playSound(null, carpet.getX(), carpet.getY(), carpet.getZ(),
                SoundEvents.CUSHION_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
            carpet.gameEvent(GameEvent.ENTITY_PLACE);
            stack.consume(1, placeContext.getPlayer());
        }

        return InteractionResult.SUCCESS;
    }
}
