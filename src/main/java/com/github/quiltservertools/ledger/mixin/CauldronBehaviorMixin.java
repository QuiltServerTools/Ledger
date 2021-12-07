package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.LedgerKt;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(CauldronBehavior.class)
public interface CauldronBehaviorMixin {

    @Inject(method = "fillCauldron",at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
            private static void ledgerLogFillCauldron(World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, BlockState state,
                                                      SoundEvent soundEvent, CallbackInfoReturnable<ActionResult> cir) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                world.getBlockState(pos),
                state,
                null,
                null,
                Sources.FILL,
                player);
    }

    @Inject(method = "emptyCauldron",at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
    private static void ledgerLogFillCauldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, ItemStack output,
                                              Predicate<BlockState> predicate, SoundEvent soundEvent, CallbackInfoReturnable<ActionResult> cir) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                state,
                Blocks.CAULDRON.getDefaultState(),
                null,
                null,
                Sources.REMOVE,
                player);
    }

    @Inject(method = "method_32208",at = @At(value = "HEAD"))
    private static void ledgerLogFillCauldron(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        LedgerKt.logInfo("remove lava final");
    }
    @Inject(method = "method_32209",at = @At(value = "HEAD"))
    private static void ledgerLogFillCauldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        LedgerKt.logInfo("leather armor");
    }
    @Inject(method = "method_32211",at = @At(value = "HEAD"))
    private static void ledgerLogFil2lCauldron(Object2ObjectOpenHashMap map, CallbackInfo ci) {
        LedgerKt.logInfo("");
    }
    @Inject(method = "method_32213",at = @At(value = "HEAD"))
    private static void ledgerLogFill3Cauldron(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        LedgerKt.logInfo("remove water final");
    }
    @Inject(method = "method_32214",at = @At(value = "HEAD"))
    private static void ledgerLogFillC4auldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        LedgerKt.logInfo("banner clean");
    }
    @Inject(method = "method_32215",at = @At(value = "HEAD"))
    private static void ledgerLogFwillCauldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        LedgerKt.logInfo("shulker clean");
    }
    @Inject(method = "method_32216",at = @At(value = "HEAD"))
    private static void ledgerLogFillwCauldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        LedgerKt.logInfo("lava bucket fill");
    }
    @Inject(method = "method_32218",at = @At(value = "HEAD"))
    private static void ledgerLogFiwllCauldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        LedgerKt.logInfo("remove lava start");
    }
    @Inject(method = "method_32219",at = @At(value = "HEAD"))
    private static void ledgerLogFillCdauldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        LedgerKt.logInfo("water bottle fill");
    }
    @Inject(method = "method_32217",at = @At(value = "HEAD"))
    private static void ledgerLogFillCdafuldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        LedgerKt.logInfo("water bucket fill");
    }
    @Inject(method = "method_32220",at = @At(value = "HEAD"))
    private static void ledgerLogFilwlCdafuldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        LedgerKt.logInfo("empty bottle drain");
    }
    @Inject(method = "method_32221",at = @At(value = "HEAD"))
    private static void ledgerLogFilwlCdagfuldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        LedgerKt.logInfo("water remove start");
    }
    @Inject(method = "method_32222",at = @At(value = "HEAD"))
    private static void ledgerLogFilwlCdadfuldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        LedgerKt.logInfo("Potion fail");
    }
    @Inject(method = "method_32223",at = @At(value = "HEAD"))
    private static void ledgerLogFilwglCdafuldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        LedgerKt.logInfo("FAIL");
    }
    @Inject(method = "method_32696",at = @At(value = "HEAD"))
    private static void ledgerLogFilwlCdrafuldron(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        LedgerKt.logInfo("remove snow final");
    }
    @Inject(method = "method_32697",at = @At(value = "HEAD"))
    private static void ledgerLgogFilwlCdafuldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        LedgerKt.logInfo("snow fill");
    }
    @Inject(method = "method_32698",at = @At(value = "HEAD"))
    private static void ledgerLogFtilwlCdafuldron(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        LedgerKt.logInfo("remove snow start");
    }

}
