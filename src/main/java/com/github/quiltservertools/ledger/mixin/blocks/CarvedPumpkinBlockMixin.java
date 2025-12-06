package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {
    @Inject(method = "clearPatternBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", shift = At.Shift.AFTER))
    private static void logStatueBreak(Level world, BlockPattern.BlockPatternMatch patternResult, CallbackInfo ci, @Local BlockInWorld cachedBlockPosition) {
        if (cachedBlockPosition.getState().isAir()) {
            return;
        }
        BlockBreakCallback.EVENT.invoker().breakBlock(world, cachedBlockPosition.getPos(), cachedBlockPosition.getState(), cachedBlockPosition.getEntity(), Sources.STATUE);
    }
}
