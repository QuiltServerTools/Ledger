package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.PlayerCausable;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.ExplosionImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExplosionImpl.class)
public abstract class ExplosionImplMixin {
    @Shadow
    @Final
    private ServerWorld world;

    @Shadow
    public abstract @Nullable LivingEntity getCausingEntity();

    @Shadow
    @Final
    @Nullable
    private Entity entity;

    @Inject(
        method = "createFire",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"
        )
    )
    private void ledgerExplosionFireCallback(List<BlockPos> positions, CallbackInfo ci, @Local BlockPos blockPos) {
        BlockState blockState = AbstractFireBlock.getState(world, blockPos);

        LivingEntity entity;
        if (this.entity instanceof PlayerCausable playerCausable && playerCausable.getCausingPlayer() != null) {
            entity = playerCausable.getCausingPlayer();
        } else {
            entity = getCausingEntity();
        }

        String source;
        if (this.entity != null && !(this.entity instanceof PlayerEntity)) {
            source = Registries.ENTITY_TYPE.getId(this.entity.getType()).getPath();
        } else {
            source = Sources.EXPLOSION;
        }

        BlockPlaceCallback.EVENT.invoker().place(
            world,
            blockPos,
            blockState,
            world.getBlockEntity(blockPos) != null ? world.getBlockEntity(blockPos) : null,
            source,
            entity instanceof PlayerEntity player ? player : null
        );
    }
}
