package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin {
    @Unique
    private BlockEntity oldBlockEntity = null;

    @Inject(method = "destroyOnUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    public void storeBlockEntity(BlockState state, Level world, BlockPos pos, Player player, CallbackInfoReturnable<InteractionResult> cir) {
        oldBlockEntity = world.getBlockEntity(pos);
    }

    @Inject(method = "destroyOnUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z", shift = At.Shift.AFTER))
    public void logBedExplosion(BlockState state, Level world, BlockPos pos, Player player, CallbackInfoReturnable<InteractionResult> cir) {
        BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, state, oldBlockEntity, Sources.INTERACT, player);
    }
}
