package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin {
    @Inject(method = "onBreak", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void logBedBreak(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfo ci, BedPart bedPart, BlockPos blockPos, BlockState blockState) {
        BlockBreakCallback.EVENT.invoker().breakBlock(world, blockPos, blockState, world.getBlockEntity(blockPos), player);
    }

    @Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z", shift = At.Shift.AFTER, ordinal = 0), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void logBedExplosion(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, state, world.getBlockEntity(pos), Sources.INTERACT, player);
    }

    @Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z", shift = At.Shift.AFTER, ordinal = 1), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void logBedExplosion(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir, BlockPos blockPos) {
        BlockBreakCallback.EVENT.invoker().breakBlock(world, blockPos, world.getBlockState(blockPos), world.getBlockEntity(blockPos), Sources.INTERACT, player);
    }
}
