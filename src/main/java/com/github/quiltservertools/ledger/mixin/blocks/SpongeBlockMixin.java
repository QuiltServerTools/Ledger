package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.SpongeBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(SpongeBlock.class)
public abstract class SpongeBlockMixin {

    private BlockState oldBlockState;

    @ModifyArgs(method = "absorbWater", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void logWaterDrainNonSource(Args args, World world, BlockPos actorBlockPos) {
        BlockPos pos = args.get(0);
        // pos is the blockpos for affected water
        BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, world.getBlockState(pos), null, Sources.SPONGE);
    }

    @ModifyArgs(method = "absorbWater", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/FluidDrainable;tryDrainFluid(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/item/ItemStack;"))
    public void logWaterDrainSource(Args args) {
        ServerWorld world = args.get(0);
        BlockPos pos = args.get(1);
        BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, world.getBlockState(pos), null, Sources.SPONGE);
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void ledgerStoreState(Args args) {
        World world = args.get(1);
        BlockPos pos = args.get(2);
        oldBlockState = world.getBlockState(pos);
        // first invocation will be sponge, all others after will be wet sponge
        // because sponges will execute this method & absorbWater for every face in contact with water.
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void ledgerLogSpongeToWetSponge(Args args) {
        World world = args.get(1);
        BlockPos pos = args.get(2);
        BlockState newBlockState = world.getBlockState(pos);
        if (oldBlockState == newBlockState) {return;} // if the sponge is already wet dont log
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, oldBlockState, newBlockState, null, null, Sources.WET);
        // logs if sponge comes into contact with water.
    }
}
