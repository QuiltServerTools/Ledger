package com.github.quiltservertools.ledger.mixin.preview;

import com.github.quiltservertools.ledger.utility.HandlerWithContext;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// An anonymous inner class inside an anonymous inner class
@Mixin(targets = "net/minecraft/block/ChestBlock$2$1")
public abstract class ChestBlockMixin {

    @Shadow
    ChestBlockEntity field_17358;

    @Inject(
            method = "createMenu(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/screen/ScreenHandler;",
            at = @At("RETURN")
    )
    private void addPositionContext(int i, PlayerInventory playerInventory, PlayerEntity playerEntity, CallbackInfoReturnable<ScreenHandler> cir) {
        ScreenHandler screenHandler = cir.getReturnValue();
        if (screenHandler != null) {
            ((HandlerWithContext) screenHandler).setPos(this.field_17358.getPos());
        }
    }

}
