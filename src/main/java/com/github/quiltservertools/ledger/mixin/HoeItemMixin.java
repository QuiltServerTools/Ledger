package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemUsageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HoeItem.class)
public abstract class HoeItemMixin {
    // These unmapped methods are lambda expressions used by the game for hoe uses
    @Inject(method = "method_36984", at = @At("HEAD"))
    private static void logHoeInteraction(BlockState state, ItemUsageContext context, CallbackInfo ci) {
        log(state, context);
    }

    @Inject(method = "method_36986", at = @At("HEAD"))
    private static void logHoeInteraction(BlockState state, ItemConvertible itemConvertible, ItemUsageContext context, CallbackInfo ci) {
        log(state, context);
    }

    private static void log(BlockState state, ItemUsageContext context) {
        var player = context.getPlayer();
        if (player != null) {
            BlockChangeCallback.EVENT.invoker().changeBlock(context.getWorld(), context.getBlockPos(), context.getWorld().getBlockState(context.getBlockPos()), state, null, null, player);
        } else {
            BlockChangeCallback.EVENT.invoker().changeBlock(context.getWorld(), context.getBlockPos(), context.getWorld().getBlockState(context.getBlockPos()), state, null, null, Sources.INTERACT);
        }
    }
}
