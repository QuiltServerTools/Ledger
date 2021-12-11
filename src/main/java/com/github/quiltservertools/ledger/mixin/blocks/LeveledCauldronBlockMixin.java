package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.LedgerKt;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.block.LeveledCauldronBlock.LEVEL;

@Mixin(LeveledCauldronBlock.class)
public abstract class LeveledCauldronBlockMixin {

    @Inject(method = "decrementFluidLevel",at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z",
            shift = At.Shift.AFTER))
    private static void ledgerLogDecrementLevelCauldron(BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        LedgerKt.logInfo("decrementFluidLevel");
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                state,
                world.getBlockState(pos),
                null,
                null,
                Sources.DRAIN);
            //Untested
    }
}
