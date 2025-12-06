package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Inject(method = "method_24922",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private static void ledgerLogAnvilBreak(Player player, Level world, BlockPos pos, CallbackInfo ci) {
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
                    target = "Lnet/minecraft/world/level/Level;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static void ledgerLogAnvilChange(Args args, Player player, Level world, BlockPos pos) {
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
