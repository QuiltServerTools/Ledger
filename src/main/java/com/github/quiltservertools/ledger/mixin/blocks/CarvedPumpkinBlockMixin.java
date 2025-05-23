package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {
    @Inject(method = "breakPatternBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", shift = At.Shift.AFTER))
    private static void logStatueBreak(World world, BlockPattern.Result patternResult, CallbackInfo ci, @Local CachedBlockPosition cachedBlockPosition) {
        if (cachedBlockPosition.getBlockState().isAir()) {
            return;
        }
        BlockBreakCallback.EVENT.invoker().breakBlock(world, cachedBlockPosition.getBlockPos(), cachedBlockPosition.getBlockState(), cachedBlockPosition.getBlockEntity(), Sources.STATUE);
    }
}
