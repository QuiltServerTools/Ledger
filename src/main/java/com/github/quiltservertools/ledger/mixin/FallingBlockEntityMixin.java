package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {
    @Shadow
    private BlockState block;

    @Inject(
            method = "spawnFromBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private static void ledgerBlockFallInvoker(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<FallingBlockEntity> cir) {
        BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, state, state.hasBlockEntity() ? world.getBlockEntity(pos) : null, Sources.GRAVITY);
    }

    @ModifyArgs(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void ledgerBlockLandInvoker(Args args) {
        FallingBlockEntity entity = (FallingBlockEntity) (Object) this;
        BlockPos pos = args.get(0);
        BlockPlaceCallback.EVENT.invoker().place(entity.getWorld(), pos, this.block, null, Sources.GRAVITY);
    }
}
