package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.DecorationEntityKillCallback;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractDecorationEntity.class)
public abstract class AbstractDecorationEntityMixin {

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/AbstractDecorationEntity;onBreak(Lnet/minecraft/entity/Entity;)V"))
    private void ledgerEntityTickInvoker(CallbackInfo ci) {
        AbstractDecorationEntity entity = (AbstractDecorationEntity) (Object) this;
        DecorationEntityKillCallback.EVENT.invoker().kill(
                entity.getWorld(), entity.getBlockPos(), entity, null
        );
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/AbstractDecorationEntity;onBreak(Lnet/minecraft/entity/Entity;)V"))
    private void ledgerEntityDamageInvoker(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        AbstractDecorationEntity entity = (AbstractDecorationEntity) (Object) this;
        DecorationEntityKillCallback.EVENT.invoker().kill(
                entity.getWorld(), entity.getBlockPos(), entity, source.getAttacker()
        );
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/AbstractDecorationEntity;onBreak(Lnet/minecraft/entity/Entity;)V"))
    private void ledgerEntityMoveInvoker(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        AbstractDecorationEntity entity = (AbstractDecorationEntity) (Object) this;
        DecorationEntityKillCallback.EVENT.invoker().kill(
                entity.getWorld(), entity.getBlockPos(), entity, null
        );
    }

    @Inject(method = "addVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/AbstractDecorationEntity;onBreak(Lnet/minecraft/entity/Entity;)V"))
    private void ledgerEntityAddVelocityInvoker(double deltaX, double deltaY, double deltaZ, CallbackInfo ci) {
        AbstractDecorationEntity entity = (AbstractDecorationEntity) (Object) this;
        DecorationEntityKillCallback.EVENT.invoker().kill(
                entity.getWorld(), entity.getBlockPos(), entity, null
        );
    }

}
