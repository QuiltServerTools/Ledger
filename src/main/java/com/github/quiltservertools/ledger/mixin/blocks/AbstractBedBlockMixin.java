package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBedBlock.class)
public abstract class AbstractBedBlockMixin {
    @Unique
    private BlockEntity oldBlockEntity = null;

    @Inject(method = "playerWillDestroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public void storeBlockEntity(Level world, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> cir, @Local(ordinal = 1) BlockPos blockPos) {
        oldBlockEntity = world.getBlockEntity(blockPos);
    }

    @Inject(method = "playerWillDestroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", shift = At.Shift.AFTER))
    public void logBedBreak(Level world, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> cir, @Local(ordinal = 1) BlockPos blockPos, @Local(ordinal = 1) BlockState blockState) {
        BlockBreakCallback.EVENT.invoker().breakBlock(world, blockPos, blockState, oldBlockEntity, player);
    }
}
