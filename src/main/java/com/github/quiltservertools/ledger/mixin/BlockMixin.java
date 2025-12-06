package com.github.quiltservertools.ledger.mixin;


import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(
            method = "updateOrDestroy(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;breakBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z"))
    private static void ledgerTryLogSupportedBlockBreak(BlockState state, BlockState newState, LevelAccessor worldAccess, BlockPos pos, int flags, int maxUpdateDepth, CallbackInfo ci) {
        if (worldAccess instanceof Level world) {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, pos.immutable(), state, state.hasBlockEntity() ? world.getBlockEntity(pos) : null, Sources.GRAVITY);
        }
    }
}
