package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @Shadow
    @Final
    private Fluid content;

    @Inject(method = "emptyContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private void logFluidBreak(LivingEntity user, Level world, BlockPos pos, BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir) {
        var blockstate = world.getBlockState(pos);
        if (!blockstate.isAir() && user instanceof Player player) {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, world.getBlockState(pos), world.getBlockEntity(pos), Sources.FLUID, player);
        }
    }

    @Inject(method = "emptyContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BucketItem;playEmptySound(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)V"))
    private void logFluidPlace(LivingEntity user, Level world, BlockPos pos, BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir) {
        if (user instanceof Player player) {
            BlockPlaceCallback.EVENT.invoker().place(world, pos, this.content.defaultFluidState().createLegacyBlock(), null, player);
        } else {
            BlockPlaceCallback.EVENT.invoker().place(world, pos, this.content.defaultFluidState().createLegacyBlock(), null, Sources.REDSTONE);
        }
    }

    @Inject(
            method = "emptyContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BucketItem;playEmptySound(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)V",
                    ordinal = 0
            )
    )
    private void logWaterlog(LivingEntity user, Level world, BlockPos pos, BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir, @Local BlockState blockState) {
        if (user instanceof Player player) {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    blockState,
                    world.getBlockState(pos),
                    world.getBlockEntity(pos),
                    world.getBlockEntity(pos),
                    player
            );
        } else {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    blockState,
                    world.getBlockState(pos),
                    world.getBlockEntity(pos),
                    world.getBlockEntity(pos),
                    Sources.REDSTONE
            ); //TODO This is dumb. Make some sort of Source wrapper
        }
    }

    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/stats/Stat;)V", ordinal = 0))
    private void logFluidPickup(Level world, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, @Local(ordinal = 0) BlockPos pos, @Local BlockState blockState) {
        if (blockState.getBlock() instanceof SimpleWaterloggedBlock) {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    blockState,
                    world.getBlockState(pos),
                    world.getBlockEntity(pos),
                    world.getBlockEntity(pos),
                    player
            );
        } else {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, blockState, world.getBlockEntity(pos), player);
        }

    }


}
