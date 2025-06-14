package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.utility.NbtUtils;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowGolemEntity.class)
public abstract class SnowGolemEntityMixin {
    @Unique
    private NbtCompound oldEntityTags;

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", shift = At.Shift.AFTER))
    public void logSnowGolemSnow(CallbackInfo ci, @Local BlockState blockState, @Local BlockPos blockPos) {
        BlockPlaceCallback.EVENT.invoker().place(((LivingEntity) (Object) this).getWorld(), blockPos, blockState, null, Sources.SNOW_GOLEM);
    }

    @Inject(method = "interactMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SnowGolemEntity;sheared(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/sound/SoundCategory;Lnet/minecraft/item/ItemStack;)V"))
    private void ledgerOldEntity(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        oldEntityTags = NbtUtils.INSTANCE.createNbt(entity);
    }

    @Inject(method = "interactMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SnowGolemEntity;sheared(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/sound/SoundCategory;Lnet/minecraft/item/ItemStack;)V", shift = At.Shift.AFTER))
    private void ledgerSnowGolemPumpkinShear(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityModifyCallback.EVENT.invoker().modify(player.getWorld(), entity.getBlockPos(), oldEntityTags, entity, null, player, Sources.SHEAR);
    }
}
