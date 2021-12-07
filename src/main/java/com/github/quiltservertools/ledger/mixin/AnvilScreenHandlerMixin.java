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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {


    @Inject(method = "method_24922",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z",
                    shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
            private static void ledgerLogAnvilDestroy(PlayerEntity player, World world, BlockPos pos, CallbackInfo ci, BlockState blockState) {
        BlockBreakCallback.EVENT.invoker().breakBlock(
                world,
                pos,
                blockState,
                null,
                Sources.DECAY,
                player);
    }


    @Inject(method = "method_24922",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
                    shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private static void ledgerLogAnvilDamageOnUse(PlayerEntity player, World world, BlockPos pos, CallbackInfo ci, BlockState blockState) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                blockState,
                world.getBlockState(pos),
                null,
                null,
                Sources.DECAY,
                player);
    }
}
