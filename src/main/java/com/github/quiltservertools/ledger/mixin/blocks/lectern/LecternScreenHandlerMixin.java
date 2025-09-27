package com.github.quiltservertools.ledger.mixin.blocks.lectern;

import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback;
import com.github.quiltservertools.ledger.utility.PlayerLecternHook;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternScreenHandler.class)
public class LecternScreenHandlerMixin {
    @Inject(method = "onButtonClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/Inventory;markDirty()V"))
    public void logPickBook(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir, @Local ItemStack itemStack) {
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        BlockEntity blockEntity = PlayerLecternHook.getActiveHandlers().get(player);
        ItemRemoveCallback.EVENT.invoker().remove(itemStack, blockEntity.getPos(), serverPlayer.getEntityWorld(), Sources.PLAYER, serverPlayer);
    }
}
