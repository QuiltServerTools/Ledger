package us.potatoboy.ledger.mixin.callbacks;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import us.potatoboy.ledger.actionutils.DoubleInventoryHelper;
import us.potatoboy.ledger.actionutils.LocationalInventory;
import us.potatoboy.ledger.callbacks.PlayerInsertItemCallback;
import us.potatoboy.ledger.callbacks.PlayerRemoveItemCallback;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {
	//FIXME Broken in 1.17

	@Shadow
	@Final
	public DefaultedList<Slot> slots;

	@Redirect(method = "internalOnSlotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;takeStack(I)Lnet/minecraft/item/ItemStack;"))
	public ItemStack ledgerLogStackTake(Slot slot, int amount, int i, int j, SlotActionType slotActionType, PlayerEntity player) {
		ItemStack takenStack = slot.takeStack(amount);

		BlockPos pos = getInventoryLocation(slot, i);
		if (pos != null) {
			logChange(player, takenStack, ItemStack.EMPTY, pos);
		}
		return takenStack;
	}

	@Redirect(method = "internalOnSlotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;setStack(Lnet/minecraft/item/ItemStack;)V"))
	public void ledgerLogStackSet(Slot slot, ItemStack stack, int i, int j, SlotActionType slotActionType, PlayerEntity player) {
		BlockPos pos = getInventoryLocation(slot, i);
		if (pos != null) {
			logChange(player, slot.getStack(), stack, pos);
		}

		slot.setStack(stack);
	}

	@Redirect(method = "internalOnSlotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;transferSlot(Lnet/minecraft/entity/player/PlayerEntity;I)Lnet/minecraft/item/ItemStack;"))
	public ItemStack ledgerLogStackTransfer(ScreenHandler handler, PlayerEntity player, int slotIndex, int i, int j, SlotActionType slotActionType, PlayerEntity transferPlayer) {
		Slot slot = this.slots.get(slotIndex);
		int origStackCount = slot.getStack().getCount();
		ItemStack result = handler.transferSlot(player, slotIndex);

		BlockPos location = getInventoryLocation(this.slots.get(0), 0);
		if (location == null) return result;

		int newStackCount = slot.getStack().getCount();
		ItemStack clonedResult = result.copy();
		boolean isAdd = slot.inventory instanceof PlayerInventory;
		clonedResult.setCount(origStackCount - newStackCount);
		logChange(player, isAdd ? ItemStack.EMPTY : clonedResult, isAdd ? clonedResult : ItemStack.EMPTY, location);

		return result;
	}

	@Inject(method = "internalOnSlotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;increment(I)V", ordinal = 0), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	public void ledgerLogStackIncrement(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci, ItemStack itemStack, PlayerInventory playerInventory, Slot slot, ItemStack itemStack7, ItemStack itemStack8, int q) {
		BlockPos pos = getInventoryLocation(slot, slotIndex);
		if (pos != null) {
			ItemStack newItemStack = itemStack7.copy();
			newItemStack.increment(q);
			logChange(player, itemStack7, newItemStack, pos);
		}
	}

	@Unique
	@Nullable
	private BlockPos getInventoryLocation(Slot slot, int slotId) {
		Inventory inventory = slot.inventory;
		if (slot.inventory instanceof DoubleInventoryHelper) {
			inventory = ((DoubleInventoryHelper) slot.inventory).getInventory(slotId);
		}
		if (inventory instanceof LocationalInventory) {
			return ((LocationalInventory) inventory).getLocation();
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
