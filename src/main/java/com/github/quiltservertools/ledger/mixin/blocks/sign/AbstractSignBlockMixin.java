package com.github.quiltservertools.ledger.mixin.blocks.sign;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.SignChangingItem;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractSignBlock.class)
public class AbstractSignBlockMixin {

    /**
     * Wraps the operation of item-based interactions with signs like waxing, dyeing, and using glow ink, with a
     * block-change log action.
     * <p>
     * Uses a weird and probably bad hack that copies the NBT data of the sign into a new block entity instance so that
     * the old data can be preserved for rollbacks.
     *
     * @param instance        The {@linkplain Item item} that is being used on the sign
     * @param world           The world of the interaction
     * @param signBlockEntity The sign block entity being interacted with
     * @param front           Whether the interaction is happening on the front of the sign
     * @param player          The player interacting with the sign
     * @param original        The original {@link SignChangingItem#useOnSign(World, SignBlockEntity, boolean, PlayerEntity)}
     *                        operation that this mixin wraps.
     * @return Returns the result of calling {@code original} with this method's parameters.
     */
    @WrapOperation(
            method = "onUseWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/SignChangingItem;useOnSign(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/SignBlockEntity;ZLnet/minecraft/entity/player/PlayerEntity;)Z"
            )
    )
    private boolean logSignItemInteraction(
            SignChangingItem instance,
            World world,
            SignBlockEntity signBlockEntity,
            boolean front,
            PlayerEntity player,
            Operation<Boolean> original
    ) {

        BlockState state = signBlockEntity.getCachedState();
        BlockPos pos = signBlockEntity.getPos();
        DynamicRegistryManager registryManager = world.getRegistryManager();

        // a bad hack to copy the old sign block entity for rollbacks
        @Nullable BlockEntity oldSignEntity = BlockEntity.createFromNbt(pos, state, signBlockEntity.createNbtWithId(registryManager), registryManager);

        boolean result = original.call(instance, world, signBlockEntity, front, player);
        if (result && oldSignEntity != null) {
            BlockChangeCallback.EVENT.invoker()
                    .changeBlock(
                            world,
                            pos,
                            state,
                            state, // the state doesn't update, the block entity does
                            oldSignEntity,
                            signBlockEntity,
                            player
                    );
        }

        return result;
    }

}
