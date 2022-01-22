package com.github.quiltservertools.ledger.mixin.blocks.cauldron;

import com.github.quiltservertools.ledger.LedgerKt;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeveledCauldronBlock.class)
public abstract class LeveledCauldronBlockMixin {

    private static PlayerEntity playerEntity;

    @Inject(method = "decrementFluidLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z",
            shift = At.Shift.AFTER))
    private static void ledgerLogDecrementLevelCauldron(BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        if (playerEntity != null) {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    state,
                    world.getBlockState(pos),
                    null,
                    null,
                    Sources.DRAIN,
                    playerEntity);
            playerEntity = null;

        } else {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    state,
                    world.getBlockState(pos),
                    null,
                    null,
                    Sources.DRAIN);
        }
    }

    @Inject(method = "onEntityCollision", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/LeveledCauldronBlock;onFireCollision(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    private void ledgerLogPlayerExtinguish(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (entity instanceof PlayerEntity) {
            playerEntity = (PlayerEntity) entity;
        }
    }

    @Inject(method = "fillFromDripstone", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", shift = At.Shift.AFTER))
    private void ledgerLogIncrementLevelCauldron(BlockState state, World world, BlockPos pos, Fluid fluid, CallbackInfo ci) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                state,
                world.getBlockState(pos),
                null,
                null,
                Sources.DRIP);
    }

    @Inject(method = "precipitationTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", shift = At.Shift.AFTER))
    private void ledgerLogIncrementLevelCauldron(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation, CallbackInfo ci) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                state,
                world.getBlockState(pos),
                null,
                null,
                Sources.SNOW);
    }

}
