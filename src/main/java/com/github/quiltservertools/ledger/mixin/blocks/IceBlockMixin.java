package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockMeltCallback;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public abstract class IceBlockMixin {
    // Logs the decay of ice blocks.

    // Log decay into air
    @Inject(method = "melt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    public void logFrostedIceDecayAir(BlockState state, Level world, BlockPos pos, CallbackInfo ci) {
        BlockMeltCallback.EVENT.invoker().melt(world, pos, state, Blocks.AIR.defaultBlockState(), world.getBlockEntity(pos));
    }

    // Log decay into other blocks
    @Inject(method = "melt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", shift = At.Shift.AFTER))
    public void logFrostedIceDecayWater(BlockState state, Level world, BlockPos pos, CallbackInfo ci) {
        BlockMeltCallback.EVENT.invoker().melt(world, pos, state, world.getBlockState(pos), world.getBlockEntity(pos));
    }
}
