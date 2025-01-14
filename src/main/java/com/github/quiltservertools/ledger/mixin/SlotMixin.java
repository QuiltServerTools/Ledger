package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.actionutils.DoubleInventoryHelper;
import com.github.quiltservertools.ledger.actionutils.LocationalInventory;
import com.github.quiltservertools.ledger.utility.HandledSlot;
import com.github.quiltservertools.ledger.utility.HandlerWithContext;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
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
    @Unique
    private ScreenHandler handler = null;
    @Unique
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
        HandlerWithContext handlerWithContext = (HandlerWithContext) handler;

        if (pos != null && handlerWithContext.getPlayer() != null) {
            handlerWithContext.onStackChanged(oldStack, this.getStack().copy(), pos);
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
}
