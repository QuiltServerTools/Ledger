package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.SpongeBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpongeBlock.class)
public abstract class SpongeBlockMixin {

    private BlockState oldBlockState;

    @Inject(method = "method_49829", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private static void logWaterDrainNonSource(BlockPos actorBlockPos, World world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // pos is the blockpos for affected water
        BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, world.getBlockState(pos), null, Sources.SPONGE);
    }

    @Inject(method = "method_49829", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/FluidDrainable;tryDrainFluid(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/item/ItemStack;"))
    private static void logWaterDrainSource(BlockPos actorBlockPos, World world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, world.getBlockState(pos), null, Sources.SPONGE);
    }

    @Inject(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void ledgerStoreState(World world, BlockPos pos, CallbackInfo ci) {
        oldBlockState = world.getBlockState(pos);
        // first invocation will be sponge, all others after will be wet sponge
        // because sponges will execute this method & absorbWater for every face in contact with water.
    }

    @Inject(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void ledgerLogSpongeToWetSponge(World world, BlockPos pos, CallbackInfo ci) {
        BlockState newBlockState = world.getBlockState(pos);
        if (oldBlockState == newBlockState) {return;} // if the sponge is already wet dont log
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, oldBlockState, newBlockState, null, null, Sources.WET);
        // logs if sponge comes into contact with water.
    }
}
