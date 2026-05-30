package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.commands.subcommands.PageCommand;
import com.github.quiltservertools.ledger.commands.subcommands.TeleportCommand;
import com.github.quiltservertools.ledger.utility.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {
    @Inject(
            method = "handleCustomClickAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;handleCustomClickAction(Lnet/minecraft/resources/Identifier;Ljava/util/Optional;)V"
            ),
            cancellable = true
    )
    private void handleLedgerPageChangeClick(ServerboundCustomClickActionPacket packet, CallbackInfo ci) {
        if (!((Object) this instanceof ServerGamePacketListenerImpl game)) return;

        if (packet.id().equals(MessageUtils.INSTANCE.getPageChangeAction())) {
            ServerPlayer player = game.player;
            packet.payload().flatMap(Tag::asCompound)
                    .flatMap(root -> root.getInt("page"))
                    .ifPresent(page ->
                            PageCommand.INSTANCE.page(player.createCommandSourceStack(), page)
                    );
            ci.cancel();
        } else if (packet.id().equals(MessageUtils.INSTANCE.getTeleportAction())) {
            ServerPlayer player = game.player;
            packet.payload().flatMap(Tag::asCompound).ifPresent(tag -> {
                var pos = new BlockPos(
                        tag.getIntOr("x", 0), 
                        tag.getIntOr("y", 0), 
                        tag.getIntOr("z", 0)
                );
                var world = Identifier.parse(tag.getStringOr("world", ""));
                ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, world);
                ServerLevel serverLevel = game.player.level().getServer().getLevel(resourceKey);
                TeleportCommand.INSTANCE.teleport(player, serverLevel, pos);
            });
            ci.cancel();
        }
    }
}
