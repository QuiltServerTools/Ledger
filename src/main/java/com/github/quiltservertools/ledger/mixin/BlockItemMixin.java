package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.LedgerKt;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {

    public BlockItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At(value = "INVOKE",target = "Lnet/minecraft/util/ActionResult;success(Z)Lnet/minecraft/util/ActionResult;"))
    public void ledgerPlayerPlaceBlockCallback(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        PlayerEntity player = context.getPlayer();
        BlockPlaceCallback.EVENT.invoker().place(
                world,
                pos,
                state,
                world.getBlockEntity(pos),
                player == null ? Sources.REDSTONE : Sources.PLAYER,
                player
        );

        // Issue:
        // the blockState in place may change due to blocks with the 'onBlockAdded' method changing their state during the other place method
        //
        //        if (!this.place(itemPlacementContext, blockState)) {
        //            return ActionResult.FAIL;
        //
        //    protected boolean place(ItemPlacementContext context, BlockState state) {
        //        return context.getWorld().setBlockState(context.getBlockPos(), state, Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
        //    }
        //
        // this results in logs being out of sync if you log 'onBlockAdded' & the block place log would show the updated block (Wet sponge -> Dry sponge in nether)
        // checking for block changes in 'place' doesn't work for some blocks that can change their state on placement but also after. (Dry sponge -> Wet Sponge | water before/after)

        // logging before this check means setBlockState checks cant be done.
    }
}