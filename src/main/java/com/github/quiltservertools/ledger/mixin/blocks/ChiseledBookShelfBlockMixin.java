package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.ItemInsertCallback;
import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChiseledBookShelfBlock.class)
public class ChiseledBookShelfBlockMixin {

    /**
     * Fires ItemInsertCallback after a book is successfully placed into a chiseled bookshelf slot.
     * Hooks into addBook(), which only runs server-side, after setItem() stores the book.
     */
    @Inject(
        method = "addBook",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/ChiseledBookShelfBlockEntity;setItem(ILnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.AFTER
        )
    )
    private static void onAddBook(
        Level level,
        BlockPos pos,
        Player player,
        ChiseledBookShelfBlockEntity bookshelfBlock,
        ItemStack itemStack,
        int slot,
        CallbackInfo ci
    ) {
        ItemStack insertedBook = bookshelfBlock.getItem(slot);
        if (!insertedBook.isEmpty() && level instanceof ServerLevel serverLevel) {
            ItemInsertCallback.EVENT.invoker().insert(insertedBook, pos, serverLevel, Sources.PLAYER, player);
        }
    }

    /**
     * Fires ItemRemoveCallback when a book is removed from a chiseled bookshelf slot.
     * Hooks into removeBook() to capture the removed ItemStack returned by removeItem().
     */
    @ModifyExpressionValue(
        method = "removeBook",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/ChiseledBookShelfBlockEntity;removeItem(II)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private static ItemStack onRemoveBook(
        ItemStack removedStack,
        Level level,
        BlockPos pos,
        Player player,
        ChiseledBookShelfBlockEntity bookshelfBlock,
        int slot
    ) {
        if (!removedStack.isEmpty() && level instanceof ServerLevel serverLevel) {
            ItemRemoveCallback.EVENT.invoker().remove(removedStack, pos, serverLevel, Sources.PLAYER, player);
        }
        return removedStack;
    }
}
