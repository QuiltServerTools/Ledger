package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.ItemInsertCallback;
import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback;
import com.github.quiltservertools.ledger.utility.HandledSlot;
import com.github.quiltservertools.ledger.utility.HandlerWithContext;
import com.github.quiltservertools.ledger.utility.ItemData;
import com.github.quiltservertools.ledger.utility.Sources;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin implements HandlerWithContext {
    @Unique
    Map<ItemData, Integer> changedStacks = new HashMap<>();
    @Unique
    private ServerPlayer player = null;
    
    @Unique
    private BlockPos pos = null;

    @Inject(method = "addSlot", at = @At(value = "HEAD"))
    private void ledgerGiveSlotHandlerReference(Slot slot, CallbackInfoReturnable<Slot> cir) {
        ((HandledSlot) slot).setHandler((AbstractContainerMenu) (Object) this);
    }

    @Inject(method = "clickMenuButton", at = @At(value = "HEAD"))
    private void ledgerButtonClickGetPlayer(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        this.player = (ServerPlayer) player;
    }

    @Inject(method = "doClick", at = @At(value = "HEAD"))
    private void internalOnSlotClickGetPlayer(int slotIndex, int button, ClickType actionType, Player player, CallbackInfo ci) {
        this.player = (ServerPlayer) player;
    }

    @Inject(method = "clicked", at = @At(value = "HEAD"))
    private void ledgerSlotClickGetPlayer(int slotIndex, int button, ClickType actionType, Player player, CallbackInfo ci) {
        this.player = (ServerPlayer) player;
    }

    @Inject(method = "clearContainer", at = @At(value = "HEAD"))
    private void ledgerDropInventoryGetPlayer(Player player, Container inventory, CallbackInfo ci) {
        this.player = (ServerPlayer) player;
    }

    @Inject(method = "removed", at = @At(value = "RETURN"))
    private void ledgerCloseScreenLogChanges(Player player, CallbackInfo ci) {
        if (!player.level().isClientSide() && pos != null) {
            for (var pair : changedStacks.keySet()) {
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.wrapAsHolder(pair.getItem()), 1, pair.getChanges());
                if (stack.isEmpty()) {
                    continue;
                }
                int count = changedStacks.get(pair);
                int countAbs = Math.abs(count);
                List<ItemStack> splitStacks = new ArrayList<>();
                while (countAbs > 0) {
                    ItemStack addStack = stack.copyWithCount(Math.min(countAbs, stack.getMaxStackSize()));
                    splitStacks.add(addStack);
                    countAbs -= addStack.getCount();
                }
                if (count > 0) {
                    for (ItemStack splitStack : splitStacks) {
                        ItemInsertCallback.EVENT.invoker().insert(splitStack, pos, (ServerLevel) player.level(), Sources.PLAYER, (ServerPlayer) player);
                    }
                } else {
                    for (ItemStack splitStack : splitStacks) {
                        ItemRemoveCallback.EVENT.invoker().remove(splitStack, pos, (ServerLevel) player.level(), Sources.PLAYER, (ServerPlayer) player);
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public ServerPlayer getPlayer() {
        return player;
    }

    @Nullable
    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public void setPos(@NotNull BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void onStackChanged(@NotNull ItemStack old, @NotNull ItemStack itemStack, @NotNull BlockPos pos) {
        if (old.isEmpty() && !itemStack.isEmpty()) {
            // Add item
            var key = new ItemData(itemStack.getItem(), itemStack.getComponentsPatch());
            if (changedStacks.containsKey(key)) {
                changedStacks.put(key, changedStacks.get(key) + itemStack.getCount());
            } else {
                changedStacks.put(key, itemStack.getCount());
            }
        } else if (!old.isEmpty() && itemStack.isEmpty()) {
            // Remove item
            var key = new ItemData(old.getItem(), old.getComponentsPatch());
            if (changedStacks.containsKey(key)) {
                changedStacks.put(key, changedStacks.get(key) - old.getCount());
            } else {
                changedStacks.put(key, -old.getCount());
            }
        } else if (!old.isEmpty() && !itemStack.isEmpty()) {
            // Item changed
            onStackChanged(old, ItemStack.EMPTY, pos);
            onStackChanged(ItemStack.EMPTY, itemStack, pos);
        }
    }
}
