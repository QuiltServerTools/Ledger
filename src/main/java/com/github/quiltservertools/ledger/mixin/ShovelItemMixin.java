package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ShovelItem.class)
public abstract class ShovelItemMixin {
    @ModifyArgs(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public void logPathFlattening(Args args, UseOnContext context) {
        var player = context.getPlayer();
        BlockState state = args.get(1);
        BlockPos pos = args.get(0);
        var world = context.getLevel();
        if (player != null) {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, world.getBlockState(pos), state, null, null, player);
        } else {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, world.getBlockState(pos), state, null, null, Sources.INTERACT);
        }
    }
}
