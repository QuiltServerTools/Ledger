package com.github.quiltservertools.ledger.mixin.blocks.sign;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.NbtUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SignBlock.class)
public class SignBlockMixin {

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
     * @param original        The original {@link SignApplicator#tryApplyToSign(Level, SignBlockEntity, boolean, Player)}
     *                        operation that this mixin wraps.
     * @return Returns the result of calling {@code original} with this method's parameters.
     */
    @WrapOperation(
            method = "useItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/SignApplicator;tryApplyToSign(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZLnet/minecraft/world/entity/player/Player;)Z"
            )
    )
    private boolean logSignItemInteraction(
        SignApplicator instance,
        Level world,
        SignBlockEntity signBlockEntity,
        boolean front,
        Player player,
        Operation<Boolean> original
    ) {

        BlockState state = signBlockEntity.getBlockState();
        BlockPos pos = signBlockEntity.getBlockPos();
        RegistryAccess registryManager = world.registryAccess();

        // a bad hack to copy the old sign block entity for rollbacks
        @Nullable BlockEntity oldSignEntity = BlockEntity.loadStatic(pos, state, NbtUtils.INSTANCE.createNbt(signBlockEntity, registryManager), registryManager);

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
