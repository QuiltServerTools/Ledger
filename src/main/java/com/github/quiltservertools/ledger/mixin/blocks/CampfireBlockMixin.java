package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin {

    @Shadow
    @Final
    public static BooleanProperty LIT;

    @Inject(method = "onUse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;incrementStat(Lnet/minecraft/util/Identifier;)V"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void logCampfireAddItem(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir, BlockEntity blockEntity) {
        CampfireBlockEntity newEntity = (CampfireBlockEntity) world.getBlockEntity(pos);
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, state, world.getBlockState(pos), blockEntity, newEntity, Sources.INSERT, player);
    }

    @Inject(method = "extinguish", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/WorldAccess;emitGameEvent(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/event/GameEvent;Lnet/minecraft/util/math/BlockPos;)V"))
    private static void logCampfireExtinguish(Entity entity, WorldAccess world, BlockPos pos, BlockState state, CallbackInfo ci) {
        CampfireBlockEntity blockEntity = (CampfireBlockEntity) world.getBlockEntity(pos);
        if (entity instanceof PlayerEntity) {
            BlockChangeCallback.EVENT.invoker().changeBlock((World) world, pos, state, state.with(LIT, Boolean.FALSE), blockEntity, null, Sources.EXTINGUISH, (PlayerEntity) entity);
        } else {
            BlockChangeCallback.EVENT.invoker().changeBlock((World) world, pos, state, state.with(LIT, Boolean.FALSE), blockEntity, null, Sources.EXTINGUISH);
        }
    }

    @Inject(method = "onProjectileHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void logCampfireProjectileIgnite(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile, CallbackInfo ci) {
        CampfireBlockEntity blockEntity = (CampfireBlockEntity) world.getBlockEntity(hit.getBlockPos());
        BlockChangeCallback.EVENT.invoker().changeBlock(world, hit.getBlockPos(), state, state.with(LIT, Boolean.TRUE), blockEntity, null, Sources.FIRE);
    }

}