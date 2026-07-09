package com.darkstarworks.flyingcarpet.mixin;

import com.darkstarworks.flyingcarpet.config.FlyingCarpetConfig;
import com.darkstarworks.flyingcarpet.entity.FlyingCarpetEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gates the sneak-to-dismount check inside {@link Player#rideTick()} so that
 * dismounting a Flying Carpet requires a confirming second sneak. We wrap the
 * {@code wantsToStopRiding()} call (a Player-declared method, so the target is
 * unambiguous) only within {@code rideTick}, leaving all other vehicles vanilla.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @WrapOperation(
        method = "rideTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;wantsToStopRiding()Z")
    )
    private boolean flyingcarpet$confirmDismount(final Player self, final Operation<Boolean> original) {
        boolean wants = original.call(self);
        if (!wants) {
            return false;
        }
        Entity vehicle = self.getVehicle();
        if (vehicle instanceof FlyingCarpetEntity carpet && FlyingCarpetConfig.get().confirmDismount) {
            return carpet.tryConfirmDismount(self);
        }
        return true;
    }
}
