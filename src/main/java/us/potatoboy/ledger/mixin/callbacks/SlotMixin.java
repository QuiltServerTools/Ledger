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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.potatoboy.ledger.actionutils.DoubleInventoryHelper;
import us.potatoboy.ledger.actionutils.LocationalInventory;
import us.potatoboy.ledger.callbacks.PlayerInsertItemCallback;
import us.potatoboy.ledger.callbacks.PlayerRemoveItemCallback;
import us.potatoboy.ledger.utility.HandledSlot;

@Mixin(Slot.class)
public abstract class SlotMixin implements HandledSlot {
    private ScreenHandler handler = null;

    @Shadow @Final public Inventory inventory;
    @Shadow @Final private int index;

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
    private void ledgerSetStackMixin(ItemStack stack, CallbackInfo ci) {
        BlockPos pos = getInventoryLocation();
        if (pos != null) {
            //logChange(handler, inventory.getStack(this.index), stack, pos);
        }
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

        if (oldEmpty) {
            PlayerInsertItemCallback.Companion.getEVENT().invoker().insert(changedStack, pos, (ServerPlayerEntity) player);
        } else {
            PlayerRemoveItemCallback.Companion.getEVENT().invoker().remove(changedStack, pos, (ServerPlayerEntity) player);
        }
    }
}
