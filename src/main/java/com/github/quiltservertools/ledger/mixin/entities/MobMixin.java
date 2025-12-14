package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.listeners.EntityCallbackListenerKt;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(
            method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Mob;discard()V"
            )
    )
    private <T extends Mob> void ledgerEntityConversion(EntityType<T> entityType, ConversionParams context, EntitySpawnReason reason, ConversionParams.AfterConversion<T> finalizer, CallbackInfoReturnable<T> cir) {
        Mob entity = (Mob) (Object) this;
        EntityCallbackListenerKt.onKill(entity.level(), entity.blockPosition(), entity, Sources.CONVERSION);
    }
}
