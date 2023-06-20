package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidDrainable;
import net.minecraft.block.FluidFillable;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @Shadow
    @Final
    private Fluid fluid;

    @Inject(method = "placeFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;breakBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    private void logFluidBreak(@Nullable PlayerEntity player, World world, BlockPos pos, @Nullable BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir) {
        var blockstate = world.getBlockState(pos);
        if (!blockstate.isAir()) {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, world.getBlockState(pos), world.getBlockEntity(pos), Sources.FLUID, player);
        }
    }
    
    @Inject(method = "placeFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void logFluidPlace(@Nullable PlayerEntity player, World world, BlockPos pos, @Nullable BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir) {
        var blockstate = world.getBlockState(pos);
        if (!blockstate.getFluidState().isStill()) {
            if (player != null) {
                BlockPlaceCallback.EVENT.invoker().place(world, pos, this.fluid.getDefaultState().getBlockState(), null, player);
            } else {
                BlockPlaceCallback.EVENT.invoker().place(world, pos, this.fluid.getDefaultState().getBlockState(), null, Sources.REDSTONE);
            }
        }
    }
    
    @Redirect(method = "placeFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/FluidFillable;tryFillWithFluid(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)Z"))
    private boolean logWaterlog(FluidFillable instance, WorldAccess worldAccess, BlockPos pos, BlockState blockState, FluidState fluidState, @Nullable PlayerEntity player, World world) {
        var oldBlockEntity = world.getBlockEntity(pos);
        boolean success = instance.tryFillWithFluid(world, pos, blockState, ((FlowableFluid)this.fluid).getStill(false));
        if (success) {
            if (player != null) BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    blockState,
                    world.getBlockState(pos),
                    oldBlockEntity,
                    world.getBlockEntity(pos),
                    player
            );
            else 
                BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    blockState,
                    world.getBlockState(pos),
                    oldBlockEntity,
                    world.getBlockEntity(pos),
                    Sources.REDSTONE
            ); //TODO This is dumb. Make some sort of Source wrapper
        }
        return success;
    }
    
    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/FluidDrainable;tryDrainFluid(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack logFluidPickup(FluidDrainable instance, WorldAccess worldAccess, BlockPos pos, BlockState blockState, Operation<ItemStack> original, World world, PlayerEntity player, Hand hand) {
        var oldBlockEntity = world.getBlockEntity(pos);
        var itemStack = original.call(instance, worldAccess, pos, blockState);
        if (!itemStack.isEmpty()) {
            if (blockState.getBlock() instanceof Waterloggable) {
                BlockChangeCallback.EVENT.invoker().changeBlock(
                        world,
                        pos,
                        blockState,
                        world.getBlockState(pos),
                        oldBlockEntity,
                        world.getBlockEntity(pos),
                        player
                );
            } else {
                BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, blockState, world.getBlockEntity(pos), player);

            }
        }
        return itemStack;
    }
}
