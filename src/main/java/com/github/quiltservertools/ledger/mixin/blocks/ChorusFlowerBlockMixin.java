package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChorusFlowerBlock.class)
public abstract class ChorusFlowerBlockMixin {
    @Inject(method = "onProjectileHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    public void logChorusFlowerBreak(Level world, BlockState state, BlockHitResult hit, Projectile projectile, CallbackInfo ci) {
        Entity entity = projectile.getOwner();
        if (entity instanceof Player player) {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, hit.getBlockPos(), state, null, Sources.PROJECTILE, player);
        } else {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, hit.getBlockPos(), state, null, Sources.PROJECTILE);
        }
    }
}
