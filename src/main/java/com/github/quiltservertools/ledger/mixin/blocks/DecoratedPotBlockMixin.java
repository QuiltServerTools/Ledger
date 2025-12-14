package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.ItemInsertCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecoratedPotBlock.class)
public abstract class DecoratedPotBlockMixin {
    @Inject(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;consumeAndReturn(ILnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    public void logItemInsert(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir, @Local DecoratedPotBlockEntity decoratedPotBlockEntity) {
        ItemInsertCallback.EVENT.invoker().insert(stack.copyWithCount(1), pos, (ServerLevel) world, Sources.PLAYER, (ServerPlayer) player);
    }

    @Inject(method = "onProjectileHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public void logProjectileHit(Level world, BlockState state, BlockHitResult hit, Projectile projectile, CallbackInfo ci) {
        if (world.getBlockEntity(hit.getBlockPos()) instanceof DecoratedPotBlockEntity decoratedPotBlockEntity) {
            if (projectile.getOwner() instanceof ServerPlayer player) {
                BlockBreakCallback.EVENT.invoker().breakBlock(world, hit.getBlockPos(), state, decoratedPotBlockEntity, Sources.PLAYER, player);
            } else {
                BlockBreakCallback.EVENT.invoker().breakBlock(world, hit.getBlockPos(), state, decoratedPotBlockEntity, Sources.PROJECTILE);
            }
        }
    }
}
