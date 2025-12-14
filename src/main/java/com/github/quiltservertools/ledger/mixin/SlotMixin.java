package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.actionutils.DoubleInventoryHelper;
import com.github.quiltservertools.ledger.actionutils.LocationalInventory;
import com.github.quiltservertools.ledger.utility.HandledSlot;
import com.github.quiltservertools.ledger.utility.HandlerWithContext;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.BlockPos;
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
    @Unique
    private AbstractContainerMenu handler = null;
    @Unique
    private ItemStack oldStack = null;

    @Shadow
    @Final
    public Container container;
    @Shadow
    @Final
    private int slot;

    @Shadow
    public abstract ItemStack getItem();

    @NotNull
    @Override
    public AbstractContainerMenu getHandler() {
        return handler;
    }

    @Override
    public void setHandler(@NotNull AbstractContainerMenu handler) {
        this.handler = handler;
        oldStack = this.getItem() == null ? ItemStack.EMPTY : this.getItem().copy();
    }

    @Inject(method = "setChanged", at = @At(value = "HEAD"))
    private void ledgerLogChanges(CallbackInfo ci) {
        BlockPos pos = getInventoryLocation();
        HandlerWithContext handlerWithContext = (HandlerWithContext) handler;

        if (pos != null && handlerWithContext.getPlayer() != null) {
            handlerWithContext.onStackChanged(oldStack, this.getItem().copy(), pos);
        }

        oldStack = this.getItem().copy();
    }

    @Unique
    @Nullable
    private BlockPos getInventoryLocation() {
        Container slotInventory = this.container;
        if (slotInventory instanceof DoubleInventoryHelper) {
            slotInventory = ((DoubleInventoryHelper) slotInventory).getInventory(this.slot);
        }
        if (slotInventory instanceof LocationalInventory) {
            return ((LocationalInventory) slotInventory).getLocation();
        }

        return null;
    }
}
