package com.github.quiltservertools.ledger.mixin.logging;

import com.github.quiltservertools.ledger.callbacks.WorldBlockBreakCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import java.util.Random;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BambooBlock.class)
public class MixinBambooBlock {
    @Inject(method = "scheduledTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;breakBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    public void logBambooBreak(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        WorldBlockBreakCallback.EVENT.invoker().place(world, pos, state, Sources.BROKE, null);
    }
}
