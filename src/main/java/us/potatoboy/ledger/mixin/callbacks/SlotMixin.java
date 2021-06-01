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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
    }

    @Inject(method = "setStack", at = @At(value = "HEAD"))
    private void ledgerLogSetStack(ItemStack newStack, CallbackInfo ci) {
        if (oldStack == null) {
            oldStack = this.getStack().copy();
        }

        BlockPos pos = getInventoryLocation();
        HandlerWithPlayer handlerWithPlayer = (HandlerWithPlayer) handler;
        if (pos != null && handlerWithPlayer.getPlayer() != null) {
            Ledger.INSTANCE.getLogger().info("new stack " + newStack);
            Ledger.INSTANCE.getLogger().info("old stack " + oldStack);
            logChange(handlerWithPlayer.getPlayer(), oldStack, newStack.copy(), pos);
        }
    }

    @Inject(method = "setStack", at = @At(value = "RETURN"))
    private void ledgerUpdateOldStack(ItemStack stack, CallbackInfo ci) {
        oldStack = this.getStack().copy();
    }

    @Inject(method = "markDirty", at = @At(value = "HEAD"))
    private void ledgerUpdateOldStackChange(CallbackInfo ci) {
        oldStack = this.getStack().copy();
    }

    /*
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
     */

    /**
     * @author Potatoboy9999
     * @reason Log the item stack removed for ledger
     */
    @Overwrite
    public ItemStack takeStack(int amount) {
        ItemStack stack = this.inventory.getStack(this.index).copy();
        ItemStack removedStack = this.inventory.removeStack(this.index, amount);

        if (oldStack == null) {
            oldStack = this.getStack().copy();
        }

        BlockPos pos = getInventoryLocation();
        HandlerWithPlayer handlerWithPlayer = (HandlerWithPlayer) handler;
        if (pos != null && handlerWithPlayer.getPlayer() != null) {
            logChange(handlerWithPlayer.getPlayer(), oldStack, stack, pos);
        }

        oldStack = this.getStack().copy();
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
    private void logChange(PlayerEntity player, ItemStack stack, ItemStack newStack, BlockPos pos) {
        if (stack.isEmpty() && newStack.isEmpty()) return; // nothing to do

        if (!stack.isEmpty() && !newStack.isEmpty()) { // 2 non-empty stacks
            if (stack.getItem() == newStack.getItem()) { // add or remove to stack of same type
                int newCount = newStack.getCount();
                int oldCount = stack.getCount();
                if (newCount > oldCount) { // add items
                    logChange(player, ItemStack.EMPTY, new ItemStack(newStack.getItem(), newCount - oldCount), pos);
                } else { // remove items
                    logChange(player, new ItemStack(newStack.getItem(), oldCount - newCount), ItemStack.EMPTY, pos);
                }
            } else { // split up the actions
                logChange(player, stack, ItemStack.EMPTY, pos); // log taking out the old stack
                logChange(player, ItemStack.EMPTY, newStack, pos); // log putting in the new stack
            }
            return;
        }

        boolean oldEmpty = stack.isEmpty(); // we know only one is empty
        ItemStack changedStack = oldEmpty ? newStack : stack;
        Ledger.INSTANCE.getLogger().info("changed stack " + changedStack);

        if (oldEmpty) {
            PlayerInsertItemCallback.Companion.getEVENT().invoker().insert(changedStack, pos, (ServerPlayerEntity) player);
        } else {
            PlayerRemoveItemCallback.Companion.getEVENT().invoker().remove(changedStack, pos, (ServerPlayerEntity) player);
        }
    }
}
