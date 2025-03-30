package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.ItemInsertCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.block.DecoratedPotBlock;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecoratedPotBlock.class)
public abstract class DecoratedPotBlockMixin {
    @Inject(method = "onUseWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;splitUnlessCreative(ILnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;"))
    public void logItemInsert(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir, @Local DecoratedPotBlockEntity decoratedPotBlockEntity) {
        ItemInsertCallback.EVENT.invoker().insert(stack.copyWithCount(1), pos, (ServerWorld) world, Sources.PLAYER, (ServerPlayerEntity) player);
    }

    @Inject(method = "onProjectileHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void logProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile, CallbackInfo ci) {
        if (world.getBlockEntity(hit.getBlockPos()) instanceof DecoratedPotBlockEntity decoratedPotBlockEntity) {
            if (projectile.getOwner() instanceof ServerPlayerEntity player) {
                BlockBreakCallback.EVENT.invoker().breakBlock(world, hit.getBlockPos(), state, decoratedPotBlockEntity, Sources.PLAYER, player);
            } else {
                BlockBreakCallback.EVENT.invoker().breakBlock(world, hit.getBlockPos(), state, decoratedPotBlockEntity, Sources.PROJECTILE);
            }
        }
    }
}
