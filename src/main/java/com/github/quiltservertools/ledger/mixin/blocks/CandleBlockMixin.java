package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
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
    private static void ledgerLogCandleExtinguish(PlayerEntity player, BlockState state, WorldAccess worldAccess, BlockPos pos, CallbackInfo ci) {
        if (worldAccess instanceof World world) {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    state,
                    state.with(LIT, !state.get(LIT)),
                    null,
                    null,
                    Sources.EXTINGUISH, player);
        }
    }

    @Inject(method = "onProjectileHit", at = @At(value = "RETURN"))
    private void ledgerLogCandleLit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile, CallbackInfo ci) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                hit.getBlockPos(),
                state,
                state.with(LIT, !state.get(LIT)),
                null,
                null,
                Sources.FIRE);
    }
}
