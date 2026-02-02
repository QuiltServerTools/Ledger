package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.commands.subcommands.PageCommand;
import com.github.quiltservertools.ledger.utility.MessageUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {
    @Inject(method = "handleCustomClickAction", at = @At("HEAD"), cancellable = true)
    private void handleLedgerPageChangeClick(ServerboundCustomClickActionPacket packet, CallbackInfo ci) {
        if (!packet.id().equals(MessageUtils.INSTANCE.getPageChangeAction())) return;
        if (!((Object) this instanceof ServerGamePacketListenerImpl game)) return;
        ServerPlayer player = game.player;
        packet.payload().flatMap(Tag::asCompound)
                .flatMap(root -> root.getInt("page"))
                .ifPresent(page ->
                        PageCommand.INSTANCE.page(player.createCommandSourceStack(), page)
                );
        ci.cancel();
    }
}
