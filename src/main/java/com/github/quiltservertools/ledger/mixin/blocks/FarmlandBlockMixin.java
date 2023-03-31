package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {

    @Inject(method = "setToDirt", at = @At("HEAD"))
    private static void logSetToDirt(Entity entity, BlockState blockState, World world, BlockPos pos, CallbackInfo ci) {
        if (entity instanceof PlayerEntity player) {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, blockState, Blocks.DIRT.getDefaultState(), null, null, Sources.TRAMPLE, player);
        } else {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, blockState, Blocks.DIRT.getDefaultState(), null, null, Sources.TRAMPLE);
        }
    }
}
