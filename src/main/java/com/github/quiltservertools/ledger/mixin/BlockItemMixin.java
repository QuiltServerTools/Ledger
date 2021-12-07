package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.LedgerKt;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {
    @Shadow public abstract Block getBlock();

    public BlockItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/util/ActionResult;success(Z)Lnet/minecraft/util/ActionResult;"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void ledgerPlayerPlaceBlockCallback(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir, ItemPlacementContext i, BlockState blockState, BlockPos blockPos, World world) {
        BlockPlaceCallback.EVENT.invoker().place(
                context.getWorld(),
                blockPos,
                blockState,
                context.getWorld().getBlockEntity(blockPos) != null ? context.getWorld().getBlockEntity(blockPos) : null,
                context.getPlayer() == null ? Sources.REDSTONE : Sources.PLAYER,
                context.getPlayer()
        );
        BlockState newState = context.getWorld().getBlockState(context.getBlockPos());
        if (blockState != newState && newState.getBlock() == Blocks.SPONGE) {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    context.getWorld(),
                    blockPos,
                    blockState,
                    newState,
                    null,
                    null,
                    Sources.DRY );
            //cant do wet here as theres two points to detect. water already in the area. water added after (which this would not log).
        }
    }
}