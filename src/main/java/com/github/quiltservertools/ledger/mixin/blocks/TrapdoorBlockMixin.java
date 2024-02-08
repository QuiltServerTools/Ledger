package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrapdoorBlock.class)
public abstract class TrapdoorBlockMixin {
    @Shadow
    @Final
    public static BooleanProperty OPEN;

    @Inject(method = "flip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void logTrapdoorInteraction(BlockState state, World world, BlockPos pos, PlayerEntity player, CallbackInfo ci) {
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, state.cycle(OPEN), state, null, null, Sources.INTERACT, player);
    }

    @Inject(method = "neighborUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void logTrapdoorRedstoneInteraction(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify, CallbackInfo ci) {
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, state, state.cycle(OPEN), null, null, Sources.REDSTONE);
    }
}
