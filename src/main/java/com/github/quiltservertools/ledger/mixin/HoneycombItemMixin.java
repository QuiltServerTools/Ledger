package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(HoneycombItem.class)
public abstract class HoneycombItemMixin {
    @ModifyArgs(method = "method_34719", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static void logCopperWaxing(Args args, @Local Level world, @Local Player player) {
        BlockPos pos = args.get(0);
        BlockState oldState = world.getBlockState(pos);
        BlockState newState = args.get(1);
        if (player == null) {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, oldState, newState, null, null, Sources.INTERACT);
        } else {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, oldState, newState, null, null, player);
        }
    }
}
