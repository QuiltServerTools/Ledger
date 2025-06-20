package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.EntityDismountCallback;
import com.github.quiltservertools.ledger.callbacks.EntityKillCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(
            method = "onDeath",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;getAttacker()Lnet/minecraft/entity/Entity;")
    )
    private void ledgerEntityKillInvoker(DamageSource source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        EntityKillCallback.EVENT.invoker().kill(
                entity.getWorld(), entity.getBlockPos(), entity, source
        );
    }

    @Inject(method = "onDismounted", at = @At("RETURN"))
    private void onEntityDismount(Entity vehicle, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!(entity instanceof PlayerEntity player)) {
            return;
        }

        EntityDismountCallback.EVENT.invoker().dismount(vehicle, player);
    }
}
