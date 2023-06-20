package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidDrainable;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/block/dispenser/DispenserBehavior$9")
public abstract class BucketDispenserBehaviorMixin extends ItemDispenserBehavior {
    @WrapOperation(method = "dispenseSilently(Lnet/minecraft/util/math/BlockPointer;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/FluidDrainable;tryDrainFluid(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack logFluidPickup(FluidDrainable instance, WorldAccess worldAccess, BlockPos pos, BlockState blockState, Operation<ItemStack> original, BlockPointer pointer, ItemStack stack) {
        var world = pointer.getWorld();
        var oldBlockEntity = world.getBlockEntity(pos);
        var itemStack = original.call(instance, worldAccess, pos, blockState);
        if (!itemStack.isEmpty()) {
            if (blockState.isLiquid() || blockState.isOf(Blocks.POWDER_SNOW)) {
                BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, blockState, world.getBlockEntity(pos), Sources.REDSTONE);
            } else {
                BlockChangeCallback.EVENT.invoker().changeBlock(
                        world,
                        pos,
                        blockState,
                        world.getBlockState(pos),
                        oldBlockEntity,
                        world.getBlockEntity(pos),
                        Sources.REDSTONE
                );
            }
        }
        return itemStack;
    }
}
