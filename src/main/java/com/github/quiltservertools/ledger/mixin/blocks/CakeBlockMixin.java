package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
    public static IntegerProperty BITES;

    @Inject(method = "eat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static void ledgerLogCakeEat(
            LevelAccessor world, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<InteractionResult> cir) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                player.level(),
                pos,
                world.getBlockState(pos),
                state.setValue(BITES, state.getValue(BITES) + 1),
                null,
                null,
                Sources.CONSUME,
                player);
    }

    @Inject(method = "eat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z",
            shift = At.Shift.AFTER))
    private static void ledgerLogCakeEatAndRemove(
            LevelAccessor world, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<InteractionResult> cir) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                player.level(),
                pos,
                state,
                world.getBlockState(pos),
                null,
                null,
                Sources.CONSUME,
                player);
    }

    @Inject(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            shift = At.Shift.AFTER))
    private void ledgerLogCakeAddCandle(
            ItemStack itemStack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                player.level(),
                pos,
                state,
                world.getBlockState(pos),
                null,
                null,
                Sources.INTERACT,
                player);
    }
}
