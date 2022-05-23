package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {
    @Inject(method = "onLandedUpon", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/FarmlandBlock;setToDirt(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    public void logFarmlandTrampling(World world, BlockState blockState, BlockPos pos, Entity entity, float fallDistance, CallbackInfo ci) {
        logFarmland(world, blockState, pos, entity);
    }

    @Inject(method = "scheduledTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/FarmlandBlock;setToDirt(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    public void logBlockAbove(BlockState blockState, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        logFarmland(world, blockState, pos, null);
    }

    @Inject(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/FarmlandBlock;setToDirt(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    public void logRandomDecay(BlockState blockState, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        logFarmland(world, blockState, pos, null);
    }

    private void logFarmland(World world, BlockState blockState, BlockPos pos, Entity entity) {
        if (entity instanceof PlayerEntity player) {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, blockState, Blocks.DIRT.getDefaultState(), null, null, Sources.TRAMPLE, player);
        } else {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, blockState, Blocks.DIRT.getDefaultState(), null, null, Sources.TRAMPLE);
        }
    }
}
