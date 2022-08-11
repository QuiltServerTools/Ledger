package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.WitherSkullBlock;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(WitherSkullBlock.class)
public abstract class WitherSkullBlockMixin {
    @ModifyArgs(method = "onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/SkullBlockEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private static void logWitherStatueBreak(Args args, World world, BlockPos pos, SkullBlockEntity blockEntity) {
        BlockPos blockPos = args.get(0);
        BlockState oldState = world.getBlockState(blockPos);
        BlockState newState = args.get(1);
        if (oldState != newState) {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, blockPos, oldState, world.getBlockEntity(blockPos), Sources.INTERACT);
        }
    }
}
