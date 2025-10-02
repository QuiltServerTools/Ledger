package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class LilyPadBreakageMixin {
    @Inject(method = "breakBlock", at = @At(value = "RETURN", ordinal = 1))
    private void ledgerModifyLilyPadBreakArgs(BlockPos pos, boolean drop, Entity entity, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir, @Local BlockState state) {
        if (!state.getBlock().equals(Blocks.LILY_PAD)) return;
        World world = (World) (Object) this;
        if (entity.getFirstPassenger() instanceof PlayerEntity player) {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, state, null, Sources.VEHICLE, player);
        } else {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, state, null, Sources.VEHICLE);
        }
    }
}