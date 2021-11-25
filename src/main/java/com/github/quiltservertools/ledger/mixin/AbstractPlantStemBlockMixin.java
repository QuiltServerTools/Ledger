package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import java.util.Random;
import net.minecraft.block.AbstractPlantStemBlock;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AbstractPlantStemBlock.class)
public abstract class AbstractPlantStemBlockMixin {
    @ModifyArgs(method = "grow", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    public void logPlantGrowth(Args args, ServerWorld world, Random random, BlockPos sourcePos, BlockState sourceState) {
        BlockPos pos = args.get(0);
        BlockState state = args.get(1);
        BlockPlaceCallback.EVENT.invoker().place(world, pos, state, null, Sources.GROW);
    }
}
