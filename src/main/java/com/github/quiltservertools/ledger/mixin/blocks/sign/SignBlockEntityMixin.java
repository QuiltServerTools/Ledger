package com.github.quiltservertools.ledger.mixin.blocks.sign;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;
import java.util.function.UnaryOperator;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin {

    /**
     * Wraps the operation of sign text editing with signs with a block-change log action.
     * <p>
     * Uses a weird and probably bad hack that copies the NBT data of the sign into a new block entity instance so that
     * the old data can be preserved for rollbacks.
     *
     * @param instance    The sign block entity being edited
     * @param textChanger A parameter for the original operation
     * @param front       Whether the interaction is happening on the front of the sign
     * @param original    The original {@link SignBlockEntity#changeText(UnaryOperator, boolean)} operation that this
     *                    mixin wraps.
     * @return Returns the result of calling {@code original} with this method's parameters.
     */
    @WrapOperation(
            method = "tryChangeText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/SignBlockEntity;changeText(Ljava/util/function/UnaryOperator;Z)Z"
            )
    )
    private boolean logSignTextChange(
            SignBlockEntity instance,
            UnaryOperator<SignText> textChanger,
            boolean front,
            Operation<Boolean> original
    ) {

        World world = instance.getWorld();
        DynamicRegistryManager registryManager = world.getRegistryManager();
        BlockPos pos = instance.getPos();
        BlockState state = instance.getCachedState();

        // a bad hack to copy the old sign block entity for rollbacks
        @Nullable BlockEntity oldSignEntity = BlockEntity.createFromNbt(pos, state, instance.createNbtWithId(registryManager), registryManager);

        boolean result = original.call(instance, textChanger, front);
        if (result && oldSignEntity != null) {

            assert world != null : "World cannot be null, this is already in the target method";

            UUID editorID = instance.getEditor();
            PlayerEntity player = world.getPlayerByUuid(editorID);
            assert player != null : "The editor must exist, this is already checked in target method";

            BlockChangeCallback.EVENT.invoker()
                    .changeBlock(
                            world,
                            pos,
                            state,
                            state, // the state doesn't update, the block entity does
                            oldSignEntity,
                            instance,
                            player
                    );
        }

        return result;
    }

}
