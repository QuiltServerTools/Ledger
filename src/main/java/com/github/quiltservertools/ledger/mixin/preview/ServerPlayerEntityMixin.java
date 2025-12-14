package com.github.quiltservertools.ledger.mixin.preview;

import com.github.quiltservertools.ledger.Ledger;
import com.github.quiltservertools.ledger.actionutils.Preview;
import com.github.quiltservertools.ledger.utility.HandlerWithContext;
import com.llamalad7.mixinextras.sugar.Local;
import kotlin.Pair;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

import static com.github.quiltservertools.ledger.utility.ItemChangeLogicKt.addItem;
import static com.github.quiltservertools.ledger.utility.ItemChangeLogicKt.removeMatchingItem;

@Mixin(targets = "net.minecraft.server.level.ServerPlayer$1")
public abstract class ServerPlayerEntityMixin {

    // synthetic field ServerPlayerEntity from the outer class
    @Final
    @Shadow
    ServerPlayer field_58075;

    @ModifyArg(
            method = "sendInitialData",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ClientboundContainerSetContentPacket;<init>(IILjava/util/List;Lnet/minecraft/world/item/ItemStack;)V"
            ), index = 2
    )
    private List<ItemStack> modifyStacks(List<ItemStack> stacks, @Local(argsOnly = true) AbstractContainerMenu handler) {
        BlockPos pos = ((HandlerWithContext) handler).getPos();
        if (pos == null) return stacks;
        Preview preview = Ledger.previewCache.get(field_58075.getUUID());
        if (preview == null) return stacks;
        List<Pair<ItemStack, Boolean>> modifiedItems = preview.getModifiedItems().get(pos);
        if (modifiedItems == null) return stacks;
        SimpleContainer inventory = new SimpleContainer(stacks.toArray(new ItemStack[]{}));
        for (Pair<ItemStack, Boolean> modifiedItem : modifiedItems) {
            if (modifiedItem.component2()) {
                addItem(modifiedItem.component1(), inventory);
            } else {
                removeMatchingItem(modifiedItem.component1(), inventory);
            }
        }
        return inventory.getItems();
    }
}
