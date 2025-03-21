package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.ItemDropCallback;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "dropItem", at = @At("RETURN"))
    private void logPlayerItemDrop(ItemStack stack, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        ItemDropCallback.EVENT.invoker().drop(cir.getReturnValue(), player);
    }
}
