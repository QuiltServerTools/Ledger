package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeverBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LeverBlock.class)
public abstract class LeverBlockMixin {

    @Unique
    private PlayerEntity activePlayer;

    @Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/LeverBlock;togglePower(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)V"))
    public void storePlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        activePlayer = player;
    }

    @ModifyArgs(method = "togglePower", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void logLeverUse(Args args, BlockState state, World world, BlockPos pos, PlayerEntity playerEntity) {
        if (activePlayer == null) return;

        BlockState newState = args.get(1);
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, state, newState, null, null, activePlayer);
    }
}
