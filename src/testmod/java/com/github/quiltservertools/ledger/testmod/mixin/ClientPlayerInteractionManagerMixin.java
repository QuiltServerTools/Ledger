package com.github.quiltservertools.ledger.testmod.mixin;

import com.github.quiltservertools.ledger.testmod.LedgerTest;
import com.github.quiltservertools.ledger.testmod.commands.InspectCommand;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(
        method = "interactBlock",
        at = @At(
            value = "INVOKE",
                target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;sendSequencedPacket(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/SequencedPacketCreator;)V"
        ),
        cancellable = true
    )
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (!InspectCommand.INSTANCE.getInspectOn())
            return;

        cir.setReturnValue(ActionResult.SUCCESS);
        LedgerTest.INSTANCE.inspectBlock(hitResult.getBlockPos());
    }
}
