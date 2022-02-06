package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FlintAndSteelItem.class)
public abstract class FlintAndSteelItemMixin {
    @ModifyArgs(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void log(Args args, ItemUsageContext context) {
        BlockState state = args.get(1);
        BlockPos pos = args.get(0);
        var player = context.getPlayer();
        var world = context.getWorld();
        var oldState = world.getBlockState(pos);
        var be = world.getBlockEntity(pos);

        if (oldState.getBlock().equals(state.getBlock())) {
            // Block types are the same, log interaction
            if (player != null) {
                BlockChangeCallback.EVENT.invoker().changeBlock(context.getWorld(), pos, oldState, state, be, be, player);
            } else {
                BlockChangeCallback.EVENT.invoker().changeBlock(context.getWorld(), pos, oldState, state, be, be, Sources.FIRE);
            }
        } else {
            if (player != null) {
                BlockPlaceCallback.EVENT.invoker().place(world, pos, state, be, Sources.FIRE, player);
            } else {
                BlockPlaceCallback.EVENT.invoker().place(world, pos, state, be, Sources.FIRE);
            }
        }
    }
}
