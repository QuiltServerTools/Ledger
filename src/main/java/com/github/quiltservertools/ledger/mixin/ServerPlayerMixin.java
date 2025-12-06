package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.EntityMountCallback;
import com.github.quiltservertools.ledger.utility.HandlerWithContext;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.OptionalInt;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(
            method = "openMenu",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;initMenu(Lnet/minecraft/world/inventory/AbstractContainerMenu;)V"
            )
    )
    public void addPositionContext(MenuProvider factory, CallbackInfoReturnable<OptionalInt> cir, @Local AbstractContainerMenu screenHandler) {
        if (factory instanceof BlockEntity blockEntity) {
            ((HandlerWithContext) screenHandler).setPos(blockEntity.getBlockPos());
        }
    }

    @Inject(method = "startRiding", at = @At(value = "RETURN"))
    public void onPlayerStartedRiding(Entity entity, boolean force, boolean emitEvent, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        if (!cir.getReturnValue()) {
            return;
        }

        EntityMountCallback.EVENT.invoker().mount(entity, player);
    }
}
