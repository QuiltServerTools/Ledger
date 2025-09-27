package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.EntityMountCallback;
import com.github.quiltservertools.ledger.utility.HandlerWithContext;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.OptionalInt;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(
            method = "openHandledScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;onScreenHandlerOpened(Lnet/minecraft/screen/ScreenHandler;)V"
            )
    )
    public void addPositionContext(NamedScreenHandlerFactory factory, CallbackInfoReturnable<OptionalInt> cir, @Local ScreenHandler screenHandler) {
        if (factory instanceof BlockEntity blockEntity) {
            ((HandlerWithContext) screenHandler).setPos(blockEntity.getPos());
        }
    }

    @Inject(method = "startRiding", at = @At(value = "RETURN"))
    public void onPlayerStartedRiding(Entity entity, boolean force, boolean emitEvent, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        if (!cir.getReturnValue()) {
            return;
        }

        EntityMountCallback.EVENT.invoker().mount(entity, player);
    }
}
