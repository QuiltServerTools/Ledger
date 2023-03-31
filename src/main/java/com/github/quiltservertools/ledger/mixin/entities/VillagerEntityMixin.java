package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.EntityKillCallback;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {
    @Inject(method = "onStruckByLightning", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntityAndPassengers(Lnet/minecraft/entity/Entity;)V"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void ledgerVillagerToWitch(ServerWorld world, LightningEntity lightning, CallbackInfo ci, WitchEntity witchEntity) {
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityKillCallback.EVENT.invoker().kill(entity.world, entity.getBlockPos(), entity, world.getDamageSources().lightningBolt());
    }
}
