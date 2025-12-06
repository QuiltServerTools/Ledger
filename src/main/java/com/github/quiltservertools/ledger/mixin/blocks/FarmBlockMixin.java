package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmBlock.class)
public abstract class FarmBlockMixin {

    @Inject(method = "turnToDirt", at = @At("HEAD"))
    private static void logSetToDirt(Entity entity, BlockState blockState, Level world, BlockPos pos, CallbackInfo ci) {
        if (entity instanceof Player player) {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, blockState, Blocks.DIRT.defaultBlockState(), null, null, Sources.TRAMPLE, player);
        } else {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, blockState, Blocks.DIRT.defaultBlockState(), null, null, Sources.TRAMPLE);
        }
    }
}
