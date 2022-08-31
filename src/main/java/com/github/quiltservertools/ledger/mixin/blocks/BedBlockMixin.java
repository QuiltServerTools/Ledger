package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin {
    @ModifyArgs(method = "onBreak", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void logBedBreak(Args args, World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockPos blockPos = args.get(0);
        BlockBreakCallback.EVENT.invoker().breakBlock(world, blockPos, world.getBlockState(blockPos), world.getBlockEntity(blockPos), player);
    }

    @ModifyArgs(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    public void logBedExplosion(Args args, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockPos blockPos = args.get(0);
        BlockBreakCallback.EVENT.invoker().breakBlock(world, blockPos, world.getBlockState(blockPos), world.getBlockEntity(blockPos), Sources.INTERACT, player);
    }
}
