package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net/minecraft/block/dispenser/DispenserBehavior$16")
public abstract class BucketDispenserBehaviorMixin extends ItemDispenserBehavior {

    @Inject(
            method = "dispenseSilently",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/FluidDrainable;tryDrainFluid(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/item/ItemStack;",
                    shift = At.Shift.AFTER
            )
    )
    private void logFluidPickup(BlockPointer pointer, ItemStack stack, CallbackInfoReturnable<ItemStack> cir, @Local(argsOnly = true) ItemStack itemStack, @Local BlockPos pos, @Local BlockState blockState) {
        var world = pointer.world();
        if (!itemStack.isEmpty()) {
            if (blockState.isLiquid() || blockState.isOf(Blocks.POWDER_SNOW)) {
                BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, blockState, world.getBlockEntity(pos), Sources.REDSTONE);
            } else {
                BlockChangeCallback.EVENT.invoker().changeBlock(
                        world,
                        pos,
                        blockState,
                        world.getBlockState(pos),
                        world.getBlockEntity(pos),
                        world.getBlockEntity(pos),
                        Sources.REDSTONE
                );
            }
        }
    }

}
