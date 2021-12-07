package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.LedgerKt;
import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.SpongeBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Queue;

@Mixin(SpongeBlock.class)
public abstract class SpongeBlockMixin {

    @Inject(method = "absorbWater", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void logWaterDrainNonSource(World world, BlockPos pos, CallbackInfoReturnable<Boolean> cir, Queue queue, int i, Pair pair, BlockPos blockPos,
                                   int j, Direction[] var8, int var9, int var10, Direction direction, BlockPos blockPos2, BlockState blockState){
        BlockBreakCallback.EVENT.invoker().breakBlock(world, blockPos2, world.getBlockState(blockPos2), null, Sources.SPONGE);
    }


    @Inject(method = "absorbWater", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/FluidDrainable;tryDrainFluid(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/item/ItemStack;"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void logWaterSource(World world, BlockPos pos, CallbackInfoReturnable<Boolean> cir, Queue queue, int i, Pair pair, BlockPos blockPos, int j,
                               Direction[] var8, int var9, int var10, Direction direction, BlockPos blockPos2, BlockState blockState, FluidState fluidState, Material material){
        BlockBreakCallback.EVENT.invoker().breakBlock(world, blockPos2, world.getBlockState(blockPos2), null, Sources.SPONGE);
    }

    @Inject(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void logWaterSource(World world, BlockPos pos, CallbackInfo ci){
        if (world.getBlockState(pos) == Blocks.WET_SPONGE.getDefaultState()) {return;}
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, world.getBlockState(pos), Blocks.WET_SPONGE.getDefaultState(), null,null, Sources.WET);
    }
}
