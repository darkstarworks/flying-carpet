package com.darkstarworks.flyingcarpet.mixin;

import com.darkstarworks.flyingcarpet.config.FlyingCarpetConfig;
import com.darkstarworks.flyingcarpet.entity.FlyingCarpetEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Makes getting off a Flying Carpet need a second sneak. Other vehicles are untouched. */
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
