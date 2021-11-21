package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.Blocks;
import net.minecraft.block.SpongeBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(SpongeBlock.class)
public abstract class SpongeBlockMixin {

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void logSpongeToWetSponge(World world, BlockPos pos, CallbackInfo ci) {
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, world.getBlockState(pos), Blocks.WET_SPONGE.getDefaultState(), null, null, Sources.SPONGE);
    }

    @ModifyArgs(method = "absorbWater", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void logWaterAbsorption(Args args, World world, BlockPos sourcePos) {
        BlockPos pos = args.get(0);
        BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, world.getBlockState(pos), null, Sources.SPONGE);
    }
}
