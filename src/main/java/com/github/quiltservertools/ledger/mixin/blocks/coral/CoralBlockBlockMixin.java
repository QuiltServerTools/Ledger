package com.github.quiltservertools.ledger.mixin.blocks.coral;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.CoralBlockBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(CoralBlockBlock.class)
public abstract class CoralBlockBlockMixin {
    @ModifyArgs(method = "scheduledTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void logCoralDeath(Args args, BlockState oldState, ServerWorld world, BlockPos pos, Random random) {
        BlockState newState = args.get(1);
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, oldState, newState, null, null, Sources.DECAY);
    }
}
