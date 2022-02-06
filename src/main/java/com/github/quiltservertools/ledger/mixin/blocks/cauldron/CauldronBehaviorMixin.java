package com.github.quiltservertools.ledger.mixin.blocks.cauldron;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(CauldronBehavior.class)
public interface CauldronBehaviorMixin {

    @Inject(method = "emptyCauldron", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
    private static void ledgerLogFullDrainCauldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, ItemStack output,
                                                   Predicate<BlockState> predicate, SoundEvent soundEvent, CallbackInfoReturnable<ActionResult> cir) {
        ledgerLogDrainCauldron(world, pos, state, player);
    }

    @Inject(method = "fillCauldron", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
    private static void ledgerLogFillCauldron(World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, BlockState state,
                                              SoundEvent soundEvent, CallbackInfoReturnable<ActionResult> cir) {
        ledgerLogFillCauldron(world, pos, world.getBlockState(pos), state, player);
    }

    @Inject(method = "method_32219", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z",
            shift = At.Shift.AFTER))
    private static void ledgerLogBottleFillWaterCauldron(BlockState state, World world, BlockPos pos,
                                                         PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        ledgerLogFillCauldron(world, pos, state, world.getBlockState(pos), player);
    }

    @Inject(method = "method_32222", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z",
            shift = At.Shift.AFTER))
    private static void ledgerLogBottleFillEmptyCauldron(BlockState state, World world, BlockPos pos,
                                                         PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        ledgerLogFillCauldron(world, pos, state, world.getBlockState(pos), player);
    }

    private static void ledgerLogDrainCauldron(World world, BlockPos pos, BlockState oldState, PlayerEntity player) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                oldState,
                Blocks.CAULDRON.getDefaultState(),
                null,
                null,
                Sources.DRAIN,
                player);
    }

    private static void ledgerLogFillCauldron(World world, BlockPos pos, BlockState oldState, BlockState newState, PlayerEntity player) {
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
