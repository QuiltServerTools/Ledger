package com.github.quiltservertools.ledger.mixin.callbacks;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.github.quiltservertools.ledger.utility.HandledSlot;
import com.github.quiltservertools.ledger.utility.HandlerWithPlayer;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin implements HandlerWithPlayer {
    @Shadow public abstract Slot getSlot(int index);

    @Unique
    private ServerPlayerEntity player = null;

    @Inject(method = "onButtonClick", at = @At(value = "HEAD"))
    private void ledgerButtonClickGetPlayer(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        this.player = (ServerPlayerEntity) player;
    }

    @Inject(method = "transferSlot", at = @At(value = "HEAD"))
    private void ledgerTransferSlotGetPlayer(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
        ((HandledSlot) this.getSlot(index)).setHandler((ScreenHandler) (Object) this);
        this.player = (ServerPlayerEntity) player;
    }

    @Inject(method = "onSlotClick", at = @At(value = "HEAD"))
    private void ledgerSlotClickGetPlayer(int index, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (index == -999) {return;} // closed screen handler
        ((HandledSlot) this.getSlot(index)).setHandler((ScreenHandler) (Object) this);
        this.player = (ServerPlayerEntity) player;
        // onslotclick executes also when exiting a screenhandler -999
    }

    @Inject(method = "dropInventory", at = @At(value = "HEAD"))
    private void ledgerDropInventoryGetPlayer(PlayerEntity player, Inventory inventory, CallbackInfo ci) {
        this.player = (ServerPlayerEntity) player;
    }

    @Nullable
    @Override
    public ServerPlayerEntity getPlayer() {
        return player;
    }
}
