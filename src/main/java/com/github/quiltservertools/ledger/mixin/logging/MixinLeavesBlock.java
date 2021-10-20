package com.github.quiltservertools.ledger.mixin.logging;

import com.github.quiltservertools.ledger.callbacks.BlockDecayCallback;
import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeavesBlock.class)
public abstract class MixinLeavesBlock {
    @Inject(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    public void logLeafDecay(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        BlockDecayCallback.EVENT.invoker().decay(world, pos, state, world.getBlockEntity(pos));
    }
}
