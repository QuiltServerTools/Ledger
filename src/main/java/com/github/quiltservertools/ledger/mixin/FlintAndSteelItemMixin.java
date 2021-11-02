package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public abstract class FlintAndSteelItemMixin {
    @Inject(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", shift = At.Shift.AFTER))
    public void logFlintAndSteelUse(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        if (context.getPlayer() != null) {
            // Should never be null in vanilla, but maybe in modded?
            BlockPlaceCallback.EVENT.invoker().place(
                    context.getWorld(),
                    context.getBlockPos(),
                    context.getWorld().getBlockState(pos),
                    context.getWorld().getBlockEntity(context.getBlockPos()) != null ? context.getWorld().getBlockEntity(context.getBlockPos()) : null,
                    context.getPlayer() == null ? Sources.REDSTONE : Sources.PLAYER,
                    context.getPlayer()
            );
        }
    }
}
