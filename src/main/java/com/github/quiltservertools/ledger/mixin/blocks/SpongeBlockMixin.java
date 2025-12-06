package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SpongeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpongeBlock.class)
public abstract class SpongeBlockMixin {

    @Unique
    private BlockState oldBlockState;

    @Inject(method = "method_49829", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static void logWaterDrainNonSource(BlockPos actorBlockPos, Level world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // pos is the blockpos for affected water
        BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, world.getBlockState(pos), null, Sources.SPONGE);
    }

    @Inject(method = "method_49829", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private static void logWaterDrainSource(BlockPos actorBlockPos, Level world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, world.getBlockState(pos), null, Sources.SPONGE);
    }

    @Inject(method = "tryAbsorbWater", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public void ledgerStoreState(Level world, BlockPos pos, CallbackInfo ci) {
        oldBlockState = world.getBlockState(pos);
        // first invocation will be sponge, all others after will be wet sponge
        // because sponges will execute this method & absorbWater for every face in contact with water.
    }

    @Inject(method = "tryAbsorbWater", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public void ledgerLogSpongeToWetSponge(Level world, BlockPos pos, CallbackInfo ci) {
        BlockState newBlockState = world.getBlockState(pos);
        if (oldBlockState == newBlockState) {return;} // if the sponge is already wet don't log
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, oldBlockState, newBlockState, null, null, Sources.WET);
        // logs if sponge comes into contact with water.
    }
}
