package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.ItemBreakCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    // Every durability loss (tools, weapons, worn armor) ends up here. The player is null when the
    // item is not held or worn by a player, e.g. a mob's armor.
    @Inject(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void ledgerLogItemBreak(int damage, ServerPlayer player, Consumer<ItemStack> onBreak, CallbackInfo ci) {
        if (player != null) {
            ItemBreakCallback.EVENT.invoker().breakItem((ItemStack) (Object) this, player);
        }
    }
}
