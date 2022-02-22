package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {
    @Shadow
    private BlockState block;

    @ModifyArgs(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    private void ledgerBlockFallInvoker(Args args) {
        FallingBlockEntity entity = (FallingBlockEntity) (Object) this;
        if (args.get(0) instanceof BlockPos pos) {
            BlockBreakCallback.EVENT.invoker().breakBlock(entity.world, pos, this.block, this.block.hasBlockEntity() ? entity.world.getBlockEntity(pos) : null, Sources.GRAVITY);
        }
        // https://paste.alloffabric.com/suzobawugi.yaml
    }

    @ModifyArgs(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void ledgerBlockLandInvoker(Args args) {
        FallingBlockEntity entity = (FallingBlockEntity) (Object) this;
        if (args.get(0) instanceof BlockPos pos) {
            BlockPlaceCallback.EVENT.invoker().place(entity.world, pos, this.block, null, Sources.GRAVITY);
        }
    }
}
