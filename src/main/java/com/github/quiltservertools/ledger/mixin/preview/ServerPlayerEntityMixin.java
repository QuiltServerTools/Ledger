package com.github.quiltservertools.ledger.mixin.preview;

import com.github.quiltservertools.ledger.Ledger;
import com.github.quiltservertools.ledger.actionutils.Preview;
import com.github.quiltservertools.ledger.utility.HandlerWithContext;
import com.llamalad7.mixinextras.sugar.Local;
import kotlin.Pair;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

@Mixin(targets = "net.minecraft.server.network.ServerPlayerEntity$1")
public abstract class ServerPlayerEntityMixin {

    // synthetic field ServerPlayerEntity from the outer class
    @Final
    @Shadow
    ServerPlayerEntity field_29182;

    @ModifyArg(
            method = "updateState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;<init>(IILnet/minecraft/util/collection/DefaultedList;Lnet/minecraft/item/ItemStack;)V"
            ), index = 2
    )
    private DefaultedList<ItemStack> modifyStacks(DefaultedList<ItemStack> stacks, @Local(argsOnly = true) ScreenHandler handler) {
        BlockPos pos = ((HandlerWithContext) handler).getPos();
        if (pos == null) return stacks;
        Preview preview = Ledger.previewCache.get(field_29182.getUuid());
        if (preview == null) return stacks;
        List<Pair<ItemStack, Boolean>> modifiedItems = preview.getModifiedItems().get(pos);
        if (modifiedItems == null) return stacks;
        // Copy original list
        DefaultedList<ItemStack> previewStacks = DefaultedList.of();
        previewStacks.addAll(stacks);
        for (Pair<ItemStack, Boolean> modifiedItem : modifiedItems) {
            if (modifiedItem.component2()) {
                // Add item
                for (int i = 0; i < previewStacks.size(); i++) {
                    if (previewStacks.get(i).isEmpty()) {
                        previewStacks.set(i, modifiedItem.component1());
                        break;
                    }
                }
            } else {
                // Remove item
                for (int i = 0; i < previewStacks.size(); i++) {
                    if (ItemStack.areItemsEqual(previewStacks.get(i), modifiedItem.component1())) {
                        previewStacks.set(i, ItemStack.EMPTY);
                        break;
                    }
                }
            }
        }
        return previewStacks;
    }
}
