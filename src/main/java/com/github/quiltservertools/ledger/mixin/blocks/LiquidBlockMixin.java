package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {
    @ModifyArgs(
            method = "shouldSpreadLiquid",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z")
    )
    private void ledgerLogFluidBlockForm(Args args, Level world, BlockPos pos, BlockState state) {
        BlockPos blockPos = args.get(0);
        BlockState blockState = args.get(1);

        BlockPlaceCallback.EVENT.invoker().place(
                world,
                blockPos,
                blockState,
                null,
                Sources.FLUID
        );
    }
}
