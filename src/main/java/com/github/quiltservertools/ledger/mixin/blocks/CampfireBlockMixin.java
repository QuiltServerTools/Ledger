package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin {

    @Shadow
    @Final
    public static BooleanProperty LIT;

    @Inject(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/resources/ResourceLocation;)V")
    )
    public void logCampfireAddItem(ItemStack itemStack, BlockState blockState, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir, @Local BlockEntity oldBlockEntity) {
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, blockState, world.getBlockState(pos), oldBlockEntity, world.getBlockEntity(pos), Sources.INSERT, player);
    }

    @Inject(method = "dowse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;gameEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)V"))
    private static void logCampfireExtinguish(Entity entity, LevelAccessor worldAccess, BlockPos pos, BlockState blockState, CallbackInfo ci) {
        if (worldAccess instanceof Level world) {
            if (entity instanceof Player player) {
                BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, blockState, blockState.setValue(LIT, Boolean.FALSE), world.getBlockEntity(pos), null, Sources.EXTINGUISH, player);
            } else {
                BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, blockState, blockState.setValue(LIT, Boolean.FALSE), world.getBlockEntity(pos), null, Sources.EXTINGUISH);
            }
        }
    }

    @Inject(method = "onProjectileHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void logCampfireProjectileIgnite(Level world, BlockState blockState, BlockHitResult hit, Projectile projectile, CallbackInfo ci) {
        BlockChangeCallback.EVENT.invoker().changeBlock(world, hit.getBlockPos(), blockState, blockState.setValue(LIT, Boolean.TRUE), world.getBlockEntity(hit.getBlockPos()), null, Sources.FIRE);
    }

}