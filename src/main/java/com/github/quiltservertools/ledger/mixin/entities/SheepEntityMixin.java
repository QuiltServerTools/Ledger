package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SheepEntity.class)
public abstract class SheepEntityMixin {
    @Unique
    private NbtCompound oldEntityTags;

    @Inject(method = "interactMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SheepEntity;sheared(Lnet/minecraft/sound/SoundCategory;)V"))
    private void ledgerLogOldEntity(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        oldEntityTags = entity.writeNbt(new NbtCompound());
    }

    @Inject(method = "interactMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SheepEntity;sheared(Lnet/minecraft/sound/SoundCategory;)V", shift = At.Shift.AFTER))
    private void ledgerSheepWoolShear(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityModifyCallback.EVENT.invoker().modify(player.getWorld(), entity.getBlockPos(), oldEntityTags, entity, null, player, Sources.SHEAR);
    }
}
