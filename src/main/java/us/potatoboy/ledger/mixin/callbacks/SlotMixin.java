package us.potatoboy.ledger.mixin.callbacks;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import us.potatoboy.ledger.Ledger;
import us.potatoboy.ledger.actionutils.DoubleInventoryHelper;
import us.potatoboy.ledger.actionutils.LocationalInventory;
import us.potatoboy.ledger.callbacks.PlayerInsertItemCallback;
import us.potatoboy.ledger.callbacks.PlayerRemoveItemCallback;
import us.potatoboy.ledger.utility.HandledSlot;
import us.potatoboy.ledger.utility.HandlerWithPlayer;

@Mixin(Slot.class)
public abstract class SlotMixin implements HandledSlot {
    private ScreenHandler handler = null;

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
    }

    @Inject(method = "setStack", at = @At(value = "HEAD"))
    private void ledgerLogSetStack(ItemStack newStack, CallbackInfo ci) {
        BlockPos pos = getInventoryLocation();
        HandlerWithPlayer handlerWithPlayer = (HandlerWithPlayer) handler;
        if (pos != null && handlerWithPlayer.getPlayer() != null) {
            ItemStack oldStack = this.getStack().copy();
            Ledger.INSTANCE.getLogger().info("new stack " + newStack);
            Ledger.INSTANCE.getLogger().info("old stack " + oldStack);
            logChange(handlerWithPlayer.getPlayer(), oldStack, newStack.copy(), pos);
        }
    }

    @Inject(
            method = "insertStack(Lnet/minecraft/item/ItemStack;I)Lnet/minecraft/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;increment(I)V"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void ledgerLogInsertStack(ItemStack cursorStack, int count, CallbackInfoReturnable<ItemStack> cir, ItemStack oldStack, int i) {
        BlockPos pos = getInventoryLocation();
        HandlerWithPlayer handlerWithPlayer = (HandlerWithPlayer) handler;
        if (pos != null && handlerWithPlayer.getPlayer() != null) {
            ItemStack newStack = oldStack.copy();
            newStack.increment(i);
            logChange(handlerWithPlayer.getPlayer(), oldStack.copy(), newStack, pos);
        }
    }

    /**
     * @author Potatoboy9999
     * @reason Log the item stack removed for ledger
     */
    @Overwrite
    public ItemStack takeStack(int amount) {
        // newStack and oldStack seem like they should be swapped. They should not
        ItemStack oldStack = this.inventory.getStack(this.index).copy();
        ItemStack removedStack = this.inventory.removeStack(this.index, amount);
        ItemStack newStack = this.inventory.getStack(this.index).copy();

        BlockPos pos = getInventoryLocation();
        HandlerWithPlayer handlerWithPlayer = (HandlerWithPlayer) handler;
        if (pos != null && handlerWithPlayer.getPlayer() != null) {
            logChange(handlerWithPlayer.getPlayer(), oldStack, newStack, pos);
        }
        return removedStack;
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
    private void logChange(PlayerEntity player, ItemStack oldStack, ItemStack newStack, BlockPos pos) {
        if (oldStack.isEmpty() && newStack.isEmpty()) return; // nothing to do

        if (!oldStack.isEmpty() && !newStack.isEmpty()) { // 2 non-empty stacks
            if (oldStack.getItem() == newStack.getItem()) { // add or remove to stack of same type
                int newCount = newStack.getCount();
                int oldCount = oldStack.getCount();
                if (newCount > oldCount) { // add items
                    logChange(player, ItemStack.EMPTY, new ItemStack(newStack.getItem(), newCount - oldCount), pos);
                } else { // remove items
                    logChange(player, new ItemStack(newStack.getItem(), oldCount - newCount), ItemStack.EMPTY, pos);
                }
            } else { // split up the actions
                logChange(player, oldStack, ItemStack.EMPTY, pos); // log taking out the old stack
                logChange(player, ItemStack.EMPTY, newStack, pos); // log putting in the new stack
            }
            return;
        }

        boolean oldEmpty = oldStack.isEmpty(); // we know only one is empty
        ItemStack changedStack = oldEmpty ? newStack : oldStack;
        Ledger.INSTANCE.getLogger().info("changed stack" + changedStack);

        if (oldEmpty) {
            PlayerInsertItemCallback.Companion.getEVENT().invoker().insert(changedStack, pos, (ServerPlayerEntity) player);
        } else {
            PlayerRemoveItemCallback.Companion.getEVENT().invoker().remove(changedStack, pos, (ServerPlayerEntity) player);
        }
    }
}
