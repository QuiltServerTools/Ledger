package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FlowerPotBlock.class)
public abstract class FlowerPotBlockMixin {
    @ModifyArgs(method = "useWithoutItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public void logFlowerPotInteractions(Args args, BlockState oldState, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        BlockState state = args.get(1);
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, oldState, state, null, null, player);
    }
}
