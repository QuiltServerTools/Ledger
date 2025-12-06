package com.github.quiltservertools.ledger.mixin.blocks.cauldron;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(CauldronInteraction.class)
public interface CauldronInteractionMixin {

    @Inject(method = "fillBucket", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"))
    private static void ledgerLogFullDrainCauldron(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, ItemStack output,
                                                   Predicate<BlockState> predicate, SoundEvent soundEvent, CallbackInfoReturnable<InteractionResult> cir) {
        ledgerLogDrainCauldron(world, pos, state, player);
    }

    @Inject(method = "emptyBucket", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"))
    private static void ledgerLogFillCauldron(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockState state,
                                              SoundEvent soundEvent, CallbackInfoReturnable<InteractionResult> cir) {
        ledgerLogFillCauldron(world, pos, world.getBlockState(pos), state, player);
    }

    @Inject(method = "method_32219", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            shift = At.Shift.AFTER))
    private static void ledgerLogBottleFillWaterCauldron(BlockState state, Level world, BlockPos pos,
                                                         Player player, InteractionHand hand, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir) {
        ledgerLogFillCauldron(world, pos, state, world.getBlockState(pos), player);
    }

    @Inject(method = "method_32222", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            shift = At.Shift.AFTER))
    private static void ledgerLogBottleFillEmptyCauldron(BlockState state, Level world, BlockPos pos,
                                                         Player player, InteractionHand hand, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir) {
        ledgerLogFillCauldron(world, pos, state, world.getBlockState(pos), player);
    }

    @Unique
    private static void ledgerLogDrainCauldron(Level world, BlockPos pos, BlockState oldState, Player player) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                oldState,
                Blocks.CAULDRON.defaultBlockState(),
                null,
                null,
                Sources.DRAIN,
                player);
    }

    @Unique
    private static void ledgerLogFillCauldron(Level world, BlockPos pos, BlockState oldState, BlockState newState, Player player) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                oldState,
                newState,
                null,
                null,
                Sources.FILL,
                player);
    }
}
