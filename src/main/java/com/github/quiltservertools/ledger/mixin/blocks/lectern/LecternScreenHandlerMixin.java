package com.github.quiltservertools.ledger.mixin.blocks.lectern;

import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback;
import com.github.quiltservertools.ledger.utility.PlayerLecternHook;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LecternScreenHandler.class)
public class LecternScreenHandlerMixin {
    @Inject(method = "onButtonClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/Inventory;markDirty()V"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void logPickBook(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir, ItemStack itemStack) {
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        BlockEntity blockEntity = PlayerLecternHook.getActiveHandlers().get(player);
        ItemRemoveCallback.EVENT.invoker().remove(itemStack, blockEntity.getPos(), serverPlayer.getServerWorld(), Sources.PLAYER, serverPlayer);
    }
}
