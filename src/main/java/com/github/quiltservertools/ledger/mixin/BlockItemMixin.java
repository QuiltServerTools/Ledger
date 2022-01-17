package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {

    public BlockItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At(value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void ledgerPlayerPlaceBlockCallback(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir, ItemPlacementContext itemPlacementContext,
                                               BlockState blockState, BlockPos blockPos, World world, PlayerEntity playerEntity, ItemStack itemStack, BlockState blockState2) {
        BlockPlaceCallback.EVENT.invoker().place(
                world,
                blockPos,
                blockState,
                world.getBlockEntity(blockPos),
                playerEntity == null ? Sources.REDSTONE : Sources.PLAYER,
                playerEntity
        );
        if (blockState != blockState2) {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    context.getWorld(),
                    blockPos,
                    blockState,
                    blockState2,
                    null,
                    null,
                    playerEntity == null ? Sources.UNKNOWN : Sources.PLAYER,
                    playerEntity);
            // Block that is actually placed in the world can be changed during this method.
            // Example: Wet sponge in nether.
            // will cause blockPlace Wet Sponge then blockChange Wet Sponge -> Sponge

            // why cant it capture locals on the success action? game event looked valid, idea wont complain but throws ExceptionInInitializerError when ran.
            // for now the target is quite early on so might cause issue if mods also mixin to this, idea complains about the params but they work for that target?
        }
    }
}