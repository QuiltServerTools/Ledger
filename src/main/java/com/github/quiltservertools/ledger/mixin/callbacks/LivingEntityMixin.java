package com.github.quiltservertools.ledger.mixin.callbacks;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.github.quiltservertools.ledger.callbacks.EntityKillCallback;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(
            method = "onDeath",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;getAttacker()Lnet/minecraft/entity/Entity;")
    )
    private void ledgerEntityKillInvoker(DamageSource source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        EntityKillCallback.Companion.getEVENT().invoker().kill(
                entity.world, entity.getBlockPos(), entity, source
        );
    }
}
