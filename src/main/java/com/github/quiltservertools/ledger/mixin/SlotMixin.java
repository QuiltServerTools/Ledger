package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.actionutils.DoubleInventoryHelper;
import com.github.quiltservertools.ledger.actionutils.LocationalInventory;
import com.github.quiltservertools.ledger.callbacks.ItemInsertCallback;
import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback;
import com.github.quiltservertools.ledger.utility.HandledSlot;
import com.github.quiltservertools.ledger.utility.HandlerWithPlayer;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
public abstract class SlotMixin implements HandledSlot {
    private ScreenHandler handler = null;
    private ItemStack oldStack = null;

    @Shadow
    @Final
    public Inventory inventory;
    @Shadow
    @Final
    private int index;

    @Shadow
    public abstract ItemStack getStack();

    @NotNull
    @Override
    public ScreenHandler getHandler() {
        return handler;
    }

    @Override
    public void setHandler(@NotNull ScreenHandler handler) {
        this.handler = handler;
        oldStack = this.getStack() == null ? ItemStack.EMPTY : this.getStack().copy();
    }

    @Inject(method = "markDirty", at = @At(value = "HEAD"))
    private void ledgerLogChanges(CallbackInfo ci) {
        BlockPos pos = getInventoryLocation();
        HandlerWithPlayer handlerWithPlayer = (HandlerWithPlayer) handler;

        if (pos != null && handlerWithPlayer.getPlayer() != null) {
            logChange(handlerWithPlayer.getPlayer(), oldStack, this.getStack().copy(), pos);
        }

        oldStack = this.getStack().copy();
    }

    @Unique
    @Nullable
    private BlockPos getInventoryLocation() {
        Inventory slotInventory = this.inventory;
        if (slotInventory instanceof DoubleInventoryHelper) {
            slotInventory = ((DoubleInventoryHelper) slotInventory).getInventory(this.index);
        }
        if (slotInventory instanceof LocationalInventory) {
            return ((LocationalInventory) slotInventory).getLocation();
        }

        return null;
    }

    @Unique
    private void logChange(ServerPlayerEntity player, ItemStack oldStack, ItemStack newStack, BlockPos pos) {

        if (newStack.isEmpty() && oldStack.isEmpty()) {return;}

        if (oldStack.isEmpty()) {
            ItemInsertCallback.EVENT.invoker().insert(newStack, pos, (ServerWorld) player.world, Sources.PLAYER, player);
        } else if (newStack.isEmpty()) {
            ItemRemoveCallback.EVENT.invoker().remove(oldStack, pos, (ServerWorld) player.world, Sources.PLAYER, player);
        }

        if (!newStack.isEmpty() && !oldStack.isEmpty()) { // prevents non 0 count air
            if (oldStack.getItem() == newStack.getItem()) {
                int newCount = newStack.getCount();
                int oldCount = oldStack.getCount();

                if (newCount > oldCount) { // add items to partial stack
                    ItemStack newNewStack = newStack.copy();
                    newNewStack.setCount(newCount - oldCount);
                    ItemInsertCallback.EVENT.invoker().insert(newNewStack, pos, (ServerWorld) player.world, Sources.PLAYER, player);
                } else if (newCount < oldCount) { // right-click remove items
                    ItemStack newOldStack = oldStack.copy();
                    newOldStack.setCount(oldCount - newCount);
                    ItemRemoveCallback.EVENT.invoker().remove(newOldStack, pos, (ServerWorld) player.world, Sources.PLAYER, player);
                } // equal values are ignored, they dont do anything (no change)

            } else { // Ctrl + 12345... swap. split up actions
                ItemRemoveCallback.EVENT.invoker().remove(oldStack, pos, (ServerWorld) player.world, Sources.PLAYER, player);
                ItemInsertCallback.EVENT.invoker().insert(newStack, pos, (ServerWorld) player.world, Sources.PLAYER, player);
            }
        }
    }
}
