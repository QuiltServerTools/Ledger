package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.ItemInsertCallback;
import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.ShelfBlock;
import net.minecraft.block.entity.ShelfBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShelfBlock.class)
public class ShelfBlockMixin {

    @ModifyExpressionValue(
            method = "swapSingleStack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/ShelfBlockEntity;swapStackNoMarkDirty(ILnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"
            )
    )
    private static ItemStack onSwapSingleStack(ItemStack shelfHeldStack, ItemStack playerStack, PlayerEntity player, ShelfBlockEntity shelfBlockEntity, int hitSlot, PlayerInventory playerInventory) {
        if (!shelfHeldStack.isEmpty()) {
            ItemRemoveCallback.EVENT.invoker().remove(shelfHeldStack, shelfBlockEntity.getPos(), (ServerWorld) player.getEntityWorld(), Sources.PLAYER, player);
        }
        if (!playerStack.isEmpty()) {
            ItemInsertCallback.EVENT.invoker().insert(playerStack, shelfBlockEntity.getPos(), (ServerWorld) player.getEntityWorld(), Sources.PLAYER, player);
        }

        return shelfHeldStack;
    }

    @ModifyExpressionValue(method = "swapAllStacks", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/ShelfBlockEntity;swapStackNoMarkDirty(ILnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"))
    private static ItemStack onSwapAllStacks(ItemStack shelfHeldStack, World world, BlockPos pos, PlayerInventory playerInventory, @Local ShelfBlockEntity shelfBlockEntity, @Local(ordinal = 0) ItemStack playerHeldStack) {
        if (!shelfHeldStack.isEmpty()) {
            ItemRemoveCallback.EVENT.invoker().remove(shelfHeldStack, shelfBlockEntity.getPos(), (ServerWorld) world, Sources.PLAYER, playerInventory.player);
        }
        if (!playerHeldStack.isEmpty()) {
            ItemInsertCallback.EVENT.invoker().insert(playerHeldStack, shelfBlockEntity.getPos(), (ServerWorld) world, Sources.PLAYER, playerInventory.player);
        }

        return shelfHeldStack;
    }
}
