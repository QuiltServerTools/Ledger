package com.github.quiltservertools.ledger.mixin.blocks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    @ModifyExpressionValue(method = "getNewLiquid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;getSource(Z)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState logFluidSourceCreation(FluidState original, Level world, BlockPos pos, BlockState state) {
        //BlockPlaceCallback.EVENT.invoker().place(world, pos, original.getBlockState(), null, Sources.FLUID); //TODO doesn't seem to fully work and is very spammy
        return original;
    }
}
