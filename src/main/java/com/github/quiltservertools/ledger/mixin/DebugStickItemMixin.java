package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DebugStickItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(DebugStickItem.class)
public abstract class DebugStickItemMixin {
    @ModifyArgs(
            method = "handleInteraction",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void logDebugStickUsage(Args args, ServerPlayer player, BlockState oldState, LevelAccessor worldAccess, BlockPos pos, boolean update, ItemStack stack) {
        if (worldAccess instanceof Level world) {
            BlockEntity blockEntity = oldState.hasBlockEntity() ? world.getBlockEntity(pos) : null;
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos.immutable(),
                    oldState,
                    args.get(1),
                    blockEntity,
                    blockEntity,
                    Sources.DEBUG_STICK,
                    player);
        }
    }
}
