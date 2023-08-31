package com.github.quiltservertools.ledger.mixin.blocks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlowableFluid.class)
public abstract class FlowableFluidMixin {
    @ModifyExpressionValue(method = "getUpdatedState", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FlowableFluid;getStill(Z)Lnet/minecraft/fluid/FluidState;"))
    private FluidState logFluidSourceCreation(FluidState original, World world, BlockPos pos, BlockState state) {
        //BlockPlaceCallback.EVENT.invoker().place(world, pos, original.getBlockState(), null, Sources.FLUID); //TODO doesn't seem to fully work and is very spammy
        return original;
    }
}
