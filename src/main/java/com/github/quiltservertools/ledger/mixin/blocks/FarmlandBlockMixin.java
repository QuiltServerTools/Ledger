package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {

    @ModifyArgs(method = "turnToBaseBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private void logSetToBaseBlock(Args args, Entity entity, BlockState blockState, Level world, BlockPos pos) {
        BlockState newState = args.get(1);
        if (entity instanceof Player player) {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, blockState, newState, null, null, Sources.TRAMPLE, player);
        } else {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, blockState, newState, null, null, Sources.TRAMPLE);
        }
    }
}
