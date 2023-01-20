package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {
    @ModifyArgs(method = "breakPatternBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private static void logStatueBreak(Args args, World world, BlockPattern.Result patternResult) {
        if (world.getBlockState(args.get(0)).isAir()) {
            return;
        }
        BlockBreakCallback.EVENT.invoker().breakBlock(world, args.get(0), world.getBlockState(args.get(0)), world.getBlockEntity(args.get(0)), Sources.GRAVITY);
    }
}
