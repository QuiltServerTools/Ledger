package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.utility.HandledSlot;
import com.github.quiltservertools.ledger.utility.HandlerWithPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin implements HandlerWithPlayer {
    @Unique
    private ServerPlayerEntity player = null;

    @Inject(method = "addSlot", at = @At(value = "HEAD"))
    private void ledgerGiveSlotHandlerReference(Slot slot, CallbackInfoReturnable<Slot> cir) {
        ((HandledSlot) slot).setHandler((ScreenHandler) (Object) this);
    }

    @Inject(method = "onButtonClick", at = @At(value = "HEAD"))
    private void ledgerButtonClickGetPlayer(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        this.player = (ServerPlayerEntity) player;
    }

    @Inject(method = "internalOnSlotClick", at = @At(value = "HEAD"))
    private void internalOnSlotClickGetPlayer(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        this.player = (ServerPlayerEntity) player;
    }

    @Inject(method = "onSlotClick", at = @At(value = "HEAD"))
    private void ledgerSlotClickGetPlayer(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        this.player = (ServerPlayerEntity) player;
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
