package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.listeners.EntityCallbackListenerKt;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
    @Inject(
            method = "convertTo(Lnet/minecraft/entity/EntityType;Lnet/minecraft/entity/conversion/EntityConversionContext;Lnet/minecraft/entity/SpawnReason;Lnet/minecraft/entity/conversion/EntityConversionContext$Finalizer;)Lnet/minecraft/entity/mob/MobEntity;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/mob/MobEntity;discard()V"
            )
    )
    private <T extends MobEntity> void ledgerEntityConversion(EntityType<T> entityType, EntityConversionContext context, SpawnReason reason, EntityConversionContext.Finalizer<T> finalizer, CallbackInfoReturnable<T> cir) {
        MobEntity entity = (MobEntity) (Object) this;
        EntityCallbackListenerKt.onKill(entity.getWorld(), entity.getBlockPos(), entity, Sources.CONVERSION);
    }
}
