package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ComparatorBlock.class)
public abstract class ComparatorBlockMixin {
    @Shadow
    @Final
    public static EnumProperty<ComparatorMode> MODE;

    @Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void logComparatorInteraction(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, state.cycle(MODE), state, world.getBlockEntity(pos), world.getBlockEntity(pos), Sources.INTERACT, player);
    }
}
