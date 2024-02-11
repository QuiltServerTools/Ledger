package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WolfEntity.class)
public abstract class WolfEntityMixin {
    @Unique
    private NbtCompound oldEntityTags;

    @Inject(method = "interactMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WolfEntity;setCollarColor(Lnet/minecraft/util/DyeColor;)V"))
    private void ledgerOldEntity(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        oldEntityTags = entity.writeNbt(new NbtCompound());
    }

    @Inject(method = "interactMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WolfEntity;setCollarColor(Lnet/minecraft/util/DyeColor;)V", shift = At.Shift.AFTER))
    private void ledgerDogCollarColour(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityModifyCallback.EVENT.invoker().modify(player.getWorld(), entity.getBlockPos(), oldEntityTags, entity, player.getStackInHand(hand), player, Sources.DYE);
    }
}
