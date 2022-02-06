package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {
    @Shadow
    private BlockState block;

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void ledgerBlockFallInvoker(CallbackInfo ci, Block block, BlockPos blockPos) {
        FallingBlockEntity entity = (FallingBlockEntity) (Object) this;

        BlockBreakCallback.EVENT.invoker().breakBlock(entity.world, blockPos, this.block, this.block.hasBlockEntity() ? entity.world.getBlockEntity(blockPos) : null, Sources.GRAVITY);
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void ledgerBlockLandInvoker(CallbackInfo ci, Block block, BlockPos blockPos2, boolean bl, boolean bl2, double d, BlockState blockState) {
        FallingBlockEntity entity = (FallingBlockEntity) (Object) this;

        BlockPlaceCallback.EVENT.invoker().place(entity.world, blockPos2, this.block, null, Sources.GRAVITY);
    }
}
