package com.github.quiltservertools.ledger.mixin.blocks.lectern;

import com.github.quiltservertools.ledger.utility.PlayerLecternHook;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {
    @Inject(method = "onClosed", at = @At(value = "HEAD"))
    public void onClosed(PlayerEntity player, CallbackInfo ci) {
        if (player.currentScreenHandler instanceof LecternScreenHandler) {
            PlayerLecternHook.getActiveHandlers().remove(player);
        }
    }
}
