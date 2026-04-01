package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LilyPadBlock.class)
public abstract class LilyPadBlockMixin {
    @Inject(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    private void ledgerLogLilyPadBreak(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean bl, CallbackInfo ci) {
        if (entity.getFirstPassenger() instanceof Player player) {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, new BlockPos(pos), state, null, Sources.VEHICLE, player);
        } else {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, new BlockPos(pos), state, null, Sources.VEHICLE);
        }
    }
}