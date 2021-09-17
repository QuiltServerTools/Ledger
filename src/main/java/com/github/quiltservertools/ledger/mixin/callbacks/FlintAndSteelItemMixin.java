package com.github.quiltservertools.ledger.mixin.callbacks;

import com.github.quiltservertools.ledger.callbacks.PlayerBlockPlaceCallback;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemPlacementContext;
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
            PlayerBlockPlaceCallback.EVENT.invoker().place(context.getWorld(), context.getPlayer(), pos, context.getWorld().getBlockState(pos), new ItemPlacementContext(context), null);
        }
    }
}
