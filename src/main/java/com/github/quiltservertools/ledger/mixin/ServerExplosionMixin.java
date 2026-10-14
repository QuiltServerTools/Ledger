package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.ExplosionHitTracker;
import com.github.quiltservertools.ledger.utility.PlayerCausable;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    public abstract @Nullable LivingEntity getIndirectSourceEntity();

    @Shadow
    @Final
    @Nullable
    private Entity source;

    @Inject(
        method = "createFire",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private void ledgerExplosionFireCallback(List<BlockPos> positions, CallbackInfo ci, @Local BlockPos blockPos) {
        BlockState blockState = BaseFireBlock.getState(level, blockPos);

        BlockPlaceCallback.EVENT.invoker().place(
            level,
            blockPos,
            blockState,
            level.getBlockEntity(blockPos) != null ? level.getBlockEntity(blockPos) : null,
            ledgerGetSourceName(),
            ledgerGetCausingPlayer()
        );
    }

    // Explosions that trigger blocks instead of destroying them (wind charges) toggle doors,
    // trapdoors, fence gates, levers and buttons and blow out candles. Log every hit that leaves the
    // same block in a different state; blocks that were destroyed are logged as broken elsewhere.
    @WrapOperation(
        method = "interactWithBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;onExplosionHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;Ljava/util/function/BiConsumer;)V"
        )
    )
    private void ledgerLogExplosionHit(BlockState state, ServerLevel world, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer, Operation<Void> original) {
        boolean wasActive = ExplosionHitTracker.getActive();
        ExplosionHitTracker.setActive(true);
        try {
            original.call(state, world, pos, explosion, dropConsumer);
        } finally {
            ExplosionHitTracker.setActive(wasActive);
        }

        BlockState newState = world.getBlockState(pos);
        if (newState == state || !newState.is(state.getBlock())) {
            return;
        }

        BlockChangeCallback.EVENT.invoker().changeBlock(
            world,
            pos,
            state,
            newState,
            null,
            null,
            ledgerGetSourceName(),
            ledgerGetCausingPlayer()
        );
    }

    @Unique
    private @Nullable Player ledgerGetCausingPlayer() {
        LivingEntity entity;
        if (this.source instanceof PlayerCausable playerCausable && playerCausable.getCausingPlayer() != null) {
            entity = playerCausable.getCausingPlayer();
        } else {
            entity = getIndirectSourceEntity();
        }
        return entity instanceof Player player ? player : null;
    }

    @Unique
    private String ledgerGetSourceName() {
        if (this.source != null && !(this.source instanceof Player)) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(this.source.getType()).getPath();
        }
        return Sources.EXPLOSION;
    }
}
