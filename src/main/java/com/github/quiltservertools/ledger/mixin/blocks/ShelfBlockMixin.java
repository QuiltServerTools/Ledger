package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.ItemInsertCallback;
import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShelfBlock.class)
public class ShelfBlockMixin {

    @ModifyExpressionValue(
            method = "swapSingleItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/ShelfBlockEntity;swapItemNoUpdate(ILnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private static ItemStack onSwapSingleStack(ItemStack shelfHeldStack, ItemStack playerStack, Player player, ShelfBlockEntity shelfBlockEntity, int hitSlot, Inventory playerInventory) {
        if (!shelfHeldStack.isEmpty()) {
            ItemRemoveCallback.EVENT.invoker().remove(shelfHeldStack, shelfBlockEntity.getBlockPos(), (ServerLevel) player.level(), Sources.PLAYER, player);
        }
        if (!playerStack.isEmpty()) {
            ItemInsertCallback.EVENT.invoker().insert(playerStack, shelfBlockEntity.getBlockPos(), (ServerLevel) player.level(), Sources.PLAYER, player);
        }

        return shelfHeldStack;
    }

    @ModifyExpressionValue(method = "swapHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/ShelfBlockEntity;swapItemNoUpdate(ILnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack onSwapAllStacks(ItemStack shelfHeldStack, Level world, BlockPos pos, Inventory playerInventory, @Local ShelfBlockEntity shelfBlockEntity, @Local(ordinal = 0) ItemStack playerHeldStack) {
        if (!shelfHeldStack.isEmpty()) {
            ItemRemoveCallback.EVENT.invoker().remove(shelfHeldStack, shelfBlockEntity.getBlockPos(), (ServerLevel) world, Sources.PLAYER, playerInventory.player);
        }
        if (!playerHeldStack.isEmpty()) {
            ItemInsertCallback.EVENT.invoker().insert(playerHeldStack, shelfBlockEntity.getBlockPos(), (ServerLevel) world, Sources.PLAYER, playerInventory.player);
        }

        return shelfHeldStack;
    }
}
