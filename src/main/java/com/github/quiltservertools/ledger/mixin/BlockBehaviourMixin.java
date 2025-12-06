package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.PlayerCausable;
import com.github.quiltservertools.ledger.utility.Sources;
import java.util.function.BiConsumer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {

    @Inject(
            method = "onExplosionHit",
            at = @At(
                    value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            )
    )
    private void ledgerBlockExplodeCallback(BlockState state, ServerLevel world, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger, CallbackInfo ci) {
        LivingEntity entity;
        if (explosion.getIndirectSourceEntity() instanceof PlayerCausable playerCausable && playerCausable.getCausingPlayer() != null) {
            entity = playerCausable.getCausingPlayer();
        } else {
            entity = explosion.getIndirectSourceEntity();
        }

        String source;
        if (explosion.getDirectSourceEntity() != null && !(explosion.getDirectSourceEntity() instanceof Player)) {
            source = BuiltInRegistries.ENTITY_TYPE.getKey(explosion.getDirectSourceEntity().getType()).getPath();
        } else {
            source = Sources.EXPLOSION;
        }

        BlockBreakCallback.EVENT.invoker().breakBlock(
                world,
                pos,
                state,
                world.getBlockEntity(pos) != null ? world.getBlockEntity(pos) : null,
                source,
                entity instanceof Player player ? player : null
        );
    }

}
