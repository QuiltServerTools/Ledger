package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {

    @WrapOperation(
            method = "turnToBaseBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private static boolean logSetToBaseBlock(
            Level world,
            BlockPos pos,
            BlockState newState,
            Operation<Boolean> original,
            @Local(argsOnly = true, name = "sourceEntity") Entity sourceEntity,
            @Local(argsOnly = true, name = "state") BlockState state
    ) {
        boolean success = original.call(world, pos, newState);
        if (success) {
            if (sourceEntity instanceof Player player) {
                BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, state, newState, null, null, Sources.TRAMPLE, player);
            } else {
                BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, state, newState, null, null, Sources.TRAMPLE);
            }
        }
        return success;
    }
}
