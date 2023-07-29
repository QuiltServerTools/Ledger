package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.ItemFrameInsertCallback;
import com.github.quiltservertools.ledger.callbacks.ItemFrameRemoveCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameEntityMixin {
    @Inject(
            method = "dropHeldStack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;setHeldItemStack(Lnet/minecraft/item/ItemStack;)V",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void ledgerItemFrameItemRemoveCallback(Entity entity, boolean alwaysDrop, CallbackInfo ci, ItemStack itemStack) {

        ItemFrameEntity itemFrame = (ItemFrameEntity) (Object) this;
        String source = Registries.ENTITY_TYPE.getId(itemFrame.getType()).getPath();

        if (itemStack != ItemStack.EMPTY)
            ItemFrameRemoveCallback.EVENT.invoker().remove(
                    itemStack,
                    itemFrame.getBlockPos(),
                    (ServerWorld) itemFrame.getWorld(),
                    source,
                    entity instanceof ServerPlayerEntity player ? player : null,
                    itemFrame
            );
    }

    @Inject(
            method = "interact",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;setHeldItemStack(Lnet/minecraft/item/ItemStack;)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void ledgerItemFrameItemInsertCallback(
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir,
            ItemStack itemStack
    ) {
        ItemFrameEntity itemFrame = (ItemFrameEntity) (Object) this;

        String source = Registries.ENTITY_TYPE.getId(itemFrame.getType()).getPath();

        if (itemStack != ItemStack.EMPTY)
            ItemFrameInsertCallback.EVENT.invoker().insert(
                    itemStack.copyWithCount(1),
                    itemFrame.getBlockPos(),
                    (ServerWorld) itemFrame.getWorld(),
                    source,
                    player instanceof ServerPlayerEntity sp ? sp : null,
                    itemFrame
            );
    }

}
