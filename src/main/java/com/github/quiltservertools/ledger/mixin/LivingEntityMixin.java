package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.EntityKillCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
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
}
