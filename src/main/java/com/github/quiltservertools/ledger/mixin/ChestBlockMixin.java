package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.utility.HandlerWithContext;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.block.ChestBlock$2$1")
public abstract class ChestBlockMixin {
    @Shadow
    ChestBlockEntity val$first;

    @Inject(
            method = "createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;",
            at = @At("RETURN")
    )
    private void addPositionContext(int i, Inventory playerInventory, Player playerEntity, CallbackInfoReturnable<AbstractContainerMenu> cir) {
        AbstractContainerMenu screenHandler = cir.getReturnValue();
        if (screenHandler != null) {
            ((HandlerWithContext) screenHandler).setPos(this.val$first.getBlockPos());
        }
    }
}
