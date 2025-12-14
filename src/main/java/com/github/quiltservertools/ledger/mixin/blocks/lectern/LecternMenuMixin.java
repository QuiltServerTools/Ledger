package com.github.quiltservertools.ledger.mixin.blocks.lectern;

import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback;
import com.github.quiltservertools.ledger.utility.PlayerLecternHook;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternMenu.class)
public class LecternMenuMixin {
    @Inject(method = "clickMenuButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setChanged()V"))
    public void logPickBook(Player player, int id, CallbackInfoReturnable<Boolean> cir, @Local ItemStack itemStack) {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        BlockEntity blockEntity = PlayerLecternHook.getActiveHandlers().get(player);
        ItemRemoveCallback.EVENT.invoker().remove(itemStack, blockEntity.getBlockPos(), serverPlayer.level(), Sources.PLAYER, serverPlayer);
    }
}
