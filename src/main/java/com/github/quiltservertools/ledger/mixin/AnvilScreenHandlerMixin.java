package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    @Inject(method = "method_24922",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    private static void ledgerLogAnvilBreak(PlayerEntity player, World world, BlockPos pos, CallbackInfo ci) {
        BlockBreakCallback.EVENT.invoker().breakBlock(
                world,
                pos,
                world.getBlockState(pos),
                null,
                Sources.DECAY,
                player);
    }

    @ModifyArgs(method = "method_24922",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private static void ledgerLogAnvilChange(Args args, PlayerEntity player, World world, BlockPos pos) {
        BlockState newBlockState = args.get(1);
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                world.getBlockState(pos),
                newBlockState,
                world.getBlockEntity(pos),
                null,
                Sources.DECAY,
                player);
    }
}
