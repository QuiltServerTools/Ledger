package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StrawBedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StrawBedBlock.class)
public abstract class StrawBedBlockMixin {
    @WrapOperation(method = "destroyOnUse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/StrawBedBlock;destroyBed(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void logStrawBedUse(StrawBedBlock bed, Level world, BlockPos pos, Operation<Void> original,
                                @Local(argsOnly = true) Player player) {
        ledgerLogStrawBedBreak(bed, world, pos, original, player);
    }

    @WrapOperation(method = "destroyOnLeave", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/StrawBedBlock;destroyBed(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void logStrawBedLeave(StrawBedBlock bed, Level world, BlockPos pos, Operation<Void> original) {
        ledgerLogStrawBedBreak(bed, world, pos, original, null);
    }

    @Unique
    private void ledgerLogStrawBedBreak(StrawBedBlock bed, Level world, BlockPos pos, Operation<Void> original,
                                        @Nullable Player player) {
        BlockState state = world.getBlockState(pos);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        original.call(bed, world, pos);
        if (!state.isAir() && world.getBlockState(pos).isAir()) {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, state, blockEntity, Sources.INTERACT, player);
        }
    }
}
