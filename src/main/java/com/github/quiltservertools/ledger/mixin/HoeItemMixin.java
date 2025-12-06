package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HoeItem.class)
public abstract class HoeItemMixin {
    // These unmapped methods are lambda expressions used by the game for hoe uses
    @Inject(method = "method_36984", at = @At("HEAD"))
    private static void logHoeInteraction(BlockState state, UseOnContext context, CallbackInfo ci) {
        log(state, context);
    }

    @Inject(method = "method_36986", at = @At("HEAD"))
    private static void logHoeInteraction(BlockState state, ItemLike itemConvertible, UseOnContext context, CallbackInfo ci) {
        log(state, context);
    }

    @Unique
    private static void log(BlockState state, UseOnContext context) {
        var player = context.getPlayer();
        if (player != null) {
            BlockChangeCallback.EVENT.invoker().changeBlock(context.getLevel(), context.getClickedPos(), context.getLevel().getBlockState(context.getClickedPos()), state, null, null, player);
        } else {
            BlockChangeCallback.EVENT.invoker().changeBlock(context.getLevel(), context.getClickedPos(), context.getLevel().getBlockState(context.getClickedPos()), state, null, null, Sources.INTERACT);
        }
    }
}
