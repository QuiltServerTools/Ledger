package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.PlayerCausable;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerExplosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

        LivingEntity entity;
        if (this.source instanceof PlayerCausable playerCausable && playerCausable.getCausingPlayer() != null) {
            entity = playerCausable.getCausingPlayer();
        } else {
            entity = getIndirectSourceEntity();
        }

        String source;
        if (this.source != null && !(this.source instanceof Player)) {
            source = BuiltInRegistries.ENTITY_TYPE.getKey(this.source.getType()).getPath();
        } else {
            source = Sources.EXPLOSION;
        }

        BlockPlaceCallback.EVENT.invoker().place(
            level,
            blockPos,
            blockState,
            level.getBlockEntity(blockPos) != null ? level.getBlockEntity(blockPos) : null,
            source,
            entity instanceof Player player ? player : null
        );
    }
}
