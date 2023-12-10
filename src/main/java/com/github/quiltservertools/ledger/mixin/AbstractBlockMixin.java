package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.utility.PlayerCausable;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

@Mixin(AbstractBlock.class)
public abstract class AbstractBlockMixin {

    @Inject(
            method = "onExploded",
            at = @At(
                    value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"
            )
    )
    private void ledgerBlockExplodeCallback(BlockState blockState, World world, BlockPos blockPos, Explosion explosion, BiConsumer<ItemStack, BlockPos> biConsumer, CallbackInfo ci) {
        LivingEntity entity;
        if (explosion.getCausingEntity() instanceof PlayerCausable playerCausable && playerCausable.getCausingPlayer() != null) {
            entity = playerCausable.getCausingPlayer();
        } else {
            entity = explosion.getCausingEntity();
        }

        String source;
        if (explosion.getEntity() != null && !(explosion.getEntity() instanceof PlayerEntity)) {
            source = Registries.ENTITY_TYPE.getId(explosion.getEntity().getType()).getPath();
        } else {
            source = Sources.EXPLOSION;
        }

        BlockBreakCallback.EVENT.invoker().breakBlock(
                world,
                blockPos,
                blockState,
                world.getBlockEntity(blockPos) != null ? world.getBlockEntity(blockPos) : null,
                source,
                entity instanceof PlayerEntity player ? player : null
        );
    }

}
