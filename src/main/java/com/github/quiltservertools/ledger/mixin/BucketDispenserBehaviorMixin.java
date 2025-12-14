package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.core.dispenser.DispenseItemBehavior$5")
public abstract class BucketDispenserBehaviorMixin extends DefaultDispenseItemBehavior {

    @Inject(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;",
                    shift = At.Shift.AFTER
            )
    )
    private void logFluidPickup(BlockSource pointer, ItemStack stack, CallbackInfoReturnable<ItemStack> cir, @Local(argsOnly = true) ItemStack itemStack, @Local BlockPos pos, @Local BlockState blockState) {
        var world = pointer.level();
        if (!itemStack.isEmpty()) {
            if (!blockState.getFluidState().isEmpty() || blockState.is(Blocks.POWDER_SNOW)) {
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
