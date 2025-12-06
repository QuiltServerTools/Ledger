package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.WetSpongeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WetSpongeBlock.class)
public abstract class WetSpongeBlockMixin {

    @Inject(method = "onPlace", at = @At(value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/level/Level;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public void logSpongeToWetSponge(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
        BlockState newBlockState = world.getBlockState(pos);
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, state, newBlockState, null, null, Sources.DRY);
    }
}
