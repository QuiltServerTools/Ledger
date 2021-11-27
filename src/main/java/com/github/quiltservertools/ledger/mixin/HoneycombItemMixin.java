package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoneycombItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(HoneycombItem.class)
public abstract class HoneycombItemMixin {
    @ModifyArgs(method = "method_34719", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private static void logCopperWaxing(Args args, ItemUsageContext context, BlockPos pos, World world, BlockState state) {
        BlockState oldState = world.getBlockState(pos);
        BlockState newState = args.get(1);
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, oldState, newState, null, null, Sources.INTERACT);
        } else {
            BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, oldState, newState, null, null, player);
        }
    }
}
