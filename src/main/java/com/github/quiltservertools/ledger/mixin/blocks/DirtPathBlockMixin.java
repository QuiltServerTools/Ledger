package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.DirtPathBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(DirtPathBlock.class)
public abstract class DirtPathBlockMixin {

    @Inject(method = "scheduledTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/FarmlandBlock;setToDirt(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
            shift = At.Shift.AFTER))
    private void ledgerLogPathToDirt(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                state,
                world.getBlockState(pos),
                null,
                null,
                Sources.TRAMPLE);
    }
}
