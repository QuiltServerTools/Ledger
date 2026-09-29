package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockTransformer.class)
public abstract class BlockTransformerMixin {
    // Axes, hoes, shovels and data pack transformations share this mutation path.
    @WrapOperation(
            method = "transformBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            )
    )
    private boolean logBlockTransformation(
            Level world,
            BlockPos pos,
            BlockState newState,
            int flags,
            Operation<Boolean> original,
            @Local(argsOnly = true, name = "context") UseOnContext context
    ) {
        BlockState oldState = world.getBlockState(pos);
        boolean success = original.call(world, pos, newState, flags);
        if (success) {
            Player player = context.getPlayer();
            if (player != null) {
                BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, oldState, newState, null, null, player);
            } else {
                BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, oldState, newState, null, null, Sources.INTERACT);
            }
        }
        return success;
    }
}
