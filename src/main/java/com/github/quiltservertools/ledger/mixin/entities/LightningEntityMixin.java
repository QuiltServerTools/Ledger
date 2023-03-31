package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LightningEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LightningEntity.class)
public abstract class LightningEntityMixin {
    @Inject(method = "spawnFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", shift = At.Shift.AFTER, ordinal = 0), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void logFirePlacedByLightningBolt(int spreadAttempts, CallbackInfo ci, BlockPos blockPos, BlockState blockState) {
        LightningEntity entity = (LightningEntity) (Object) this;
        BlockPlaceCallback.EVENT.invoker().place(entity.getEntityWorld(), blockPos, blockState, null, Registries.ENTITY_TYPE.getId(entity.getType()).getPath());
    }

    @Inject(method = "spawnFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", shift = At.Shift.AFTER, ordinal = 1), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void logFirePlacedByLightningBolt(int spreadAttempts, CallbackInfo ci, BlockPos blockPos, BlockState blockState, int i, BlockPos blockPos2) {
        LightningEntity entity = (LightningEntity) (Object) this;
        BlockPlaceCallback.EVENT.invoker().place(entity.getEntityWorld(), blockPos2, blockState, null, Registries.ENTITY_TYPE.getId(entity.getType()).getPath());
    }
}
