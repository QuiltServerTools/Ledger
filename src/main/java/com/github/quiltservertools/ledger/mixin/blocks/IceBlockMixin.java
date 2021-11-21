package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockMeltCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IceBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public abstract class IceBlockMixin {
    // Logs the decay of ice blocks.

    // Log decay into air
    @Inject(method = "melt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    public void logFrostedIceDecayAir(BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        BlockMeltCallback.EVENT.invoker().melt(world, pos, state, Blocks.AIR.getDefaultState(), world.getBlockEntity(pos));
    }

    // Log decay into other blocks
    @Inject(method = "melt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", shift = At.Shift.AFTER))
    public void logFrostedIceDecayWater(BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        BlockMeltCallback.EVENT.invoker().melt(world, pos, state, world.getBlockState(pos), world.getBlockEntity(pos));
    }
}
