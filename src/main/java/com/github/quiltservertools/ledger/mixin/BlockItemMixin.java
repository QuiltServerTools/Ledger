package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {
    public BlockItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(
            method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ActionResult;success(Z)Lnet/minecraft/util/ActionResult;"),
            cancellable = true
    )
    public void ledgerPlayerPlaceBlockCallback(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        BlockState blockState = context.getWorld().getBlockState(context.getBlockPos());

        BlockPlaceCallback.EVENT.invoker().place(
                context.getWorld(),
                context.getBlockPos(),
                blockState,
                context.getWorld().getBlockEntity(context.getBlockPos()) != null ? context.getWorld().getBlockEntity(context.getBlockPos()) : null,
                context.getPlayer() == null ? Sources.REDSTONE : Sources.PLAYER,
                context.getPlayer()
        );
    }
}