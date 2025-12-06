package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RootedDirtBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RootedDirtBlock.class)
public abstract class RootedDirtBlockMixin {
    @Inject(method = "performBonemeal", at = @At("HEAD"))
    public void logHangingRootsGrowth(ServerLevel world, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
        BlockPlaceCallback.EVENT.invoker().place(world, pos.below(), Blocks.HANGING_ROOTS.defaultBlockState(), null, Sources.GROW);
    }
}
