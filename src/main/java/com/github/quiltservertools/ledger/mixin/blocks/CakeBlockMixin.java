package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CakeBlock.class)
public abstract class CakeBlockMixin {
    @Shadow
    @Final
    public static IntProperty BITES;

    @Inject(method = "tryEat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/WorldAccess;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private static void ledgerLogCakeEat(
            WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfoReturnable<ActionResult> cir) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                player.getWorld(),
                pos,
                world.getBlockState(pos),
                state.with(BITES, state.get(BITES) + 1),
                null,
                null,
                Sources.CONSUME,
                player);
    }

    @Inject(method = "tryEat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/WorldAccess;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z",
            shift = At.Shift.AFTER))
    private static void ledgerLogCakeEatAndRemove(
            WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfoReturnable<ActionResult> cir) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                player.getWorld(),
                pos,
                state,
                world.getBlockState(pos),
                null,
                null,
                Sources.CONSUME,
                player);
    }

    @Inject(method = "onUseWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z",
            shift = At.Shift.AFTER))
    private void ledgerLogCakeAddCandle(
            ItemStack itemStack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<ItemActionResult> cir) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                player.getWorld(),
                pos,
                state,
                world.getBlockState(pos),
                null,
                null,
                Sources.INTERACT,
                player);
    }
}
