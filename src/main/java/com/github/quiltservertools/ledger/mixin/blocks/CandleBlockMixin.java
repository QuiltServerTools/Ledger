package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractCandleBlock.class)
public abstract class CandleBlockMixin {

    @Shadow
    @Final
    public static BooleanProperty LIT;

    @Inject(method = "extinguish", at = @At(value = "RETURN"))
    private static void ledgerLogCandleExtinguish(Player player, BlockState state, LevelAccessor worldAccess, BlockPos pos, CallbackInfo ci) {
        if (worldAccess instanceof Level world) {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    state,
                    state.setValue(LIT, !state.getValue(LIT)),
                    null,
                    null,
                    Sources.EXTINGUISH, player);
        }
    }

    @Inject(method = "onProjectileHit", at = @At(value = "RETURN"))
    private void ledgerLogCandleLit(Level world, BlockState state, BlockHitResult hit, Projectile projectile, CallbackInfo ci) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                hit.getBlockPos(),
                state,
                state.setValue(LIT, !state.getValue(LIT)),
                null,
                null,
                Sources.FIRE);
    }
}
