package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin {
    @Inject(
            method = "spawnFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void logFirePlacedByLightningBolt(int spreadAttempts, CallbackInfo ci, @Local BlockPos blockPos, @Local BlockState blockState) {
        LightningBolt entity = (LightningBolt) (Object) this;
        BlockPlaceCallback.EVENT.invoker().place(entity.level(), blockPos, blockState, null, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath());
    }

    @Inject(
            method = "spawnFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 1,
                    shift = At.Shift.AFTER
            )
    )
    private void logFirePlacedByLightningBoltSpread(int spreadAttempts, CallbackInfo ci, @Local(ordinal = 1) BlockPos blockPos, @Local BlockState blockState) {
        LightningBolt entity = (LightningBolt) (Object) this;
        BlockPlaceCallback.EVENT.invoker().place(entity.level(), blockPos, blockState, null, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath());
    }
}
