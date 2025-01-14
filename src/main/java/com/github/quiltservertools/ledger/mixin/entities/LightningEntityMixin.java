package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LightningEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningEntity.class)
public abstract class LightningEntityMixin {
    @Inject(
            method = "spawnFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void logFirePlacedByLightningBolt(int spreadAttempts, CallbackInfo ci, @Local BlockPos blockPos, @Local BlockState blockState) {
        LightningEntity entity = (LightningEntity) (Object) this;
        BlockPlaceCallback.EVENT.invoker().place(entity.getEntityWorld(), blockPos, blockState, null, Registries.ENTITY_TYPE.getId(entity.getType()).getPath());
    }

    @Inject(
            method = "spawnFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z",
                    ordinal = 1,
                    shift = At.Shift.AFTER
            )
    )
    private void logFirePlacedByLightningBoltSpread(int spreadAttempts, CallbackInfo ci, @Local(ordinal = 1) BlockPos blockPos, @Local BlockState blockState) {
        LightningEntity entity = (LightningEntity) (Object) this;
        BlockPlaceCallback.EVENT.invoker().place(entity.getEntityWorld(), blockPos, blockState, null, Registries.ENTITY_TYPE.getId(entity.getType()).getPath());
    }
}
