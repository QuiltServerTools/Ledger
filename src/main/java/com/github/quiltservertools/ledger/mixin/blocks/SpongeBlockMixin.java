package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SpongeBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(SpongeBlock.class)
public abstract class SpongeBlockMixin {

    @ModifyArgs(method = "absorbWater", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void logWaterDrainNonSource(Args args, World world, BlockPos actorBlockPos){
        BlockPos pos = args.get(0);
        // pos is the blockpos for affected water
        BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, world.getBlockState(pos), null, Sources.SPONGE);
    }

    @ModifyArgs(method = "absorbWater", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/FluidDrainable;tryDrainFluid(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/item/ItemStack;"))
    public void logWaterDrainSource(Args args){
        ServerWorld world = args.get(0);
        BlockPos pos = args.get(1);
        BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, world.getBlockState(pos), null, Sources.SPONGE);
    }

    @Inject(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void logSpongeToWetSponge(World world, BlockPos pos, CallbackInfo ci){
        BlockState newBlockState = world.getBlockState(pos);
        if (newBlockState.getBlock() != Blocks.SPONGE) { return; }
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, Blocks.SPONGE.getDefaultState(), newBlockState, null,null, Sources.WET);
        // logs if water comes into contact with sponge or sponge comes into contact with water.
        // caution: this gets spammed a few times by multiple sources until the sponge has become wet in the world. so it will return if the block is no longer sponge
    }
}
