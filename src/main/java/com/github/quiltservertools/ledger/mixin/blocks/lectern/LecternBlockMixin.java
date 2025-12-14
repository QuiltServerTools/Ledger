package com.github.quiltservertools.ledger.mixin.blocks.lectern;

import com.github.quiltservertools.ledger.callbacks.ItemInsertCallback;
import com.github.quiltservertools.ledger.utility.PlayerLecternHook;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternBlock.class)
public class LecternBlockMixin {
    @Inject(method = "placeBook", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private static void logPutBook(LivingEntity user, Level world, BlockPos pos, BlockState state, ItemStack stack, CallbackInfo ci) {
        LecternBlockEntity blockEntity = (LecternBlockEntity) world.getBlockEntity(pos);
        if (blockEntity == null) return;
        ItemInsertCallback.EVENT.invoker().insert(blockEntity.getBook(), pos, (ServerLevel) world, Sources.PLAYER, (ServerPlayer) user);
    }

    @Inject(method = "openScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;"))
    public void storeLectern(Level world, BlockPos pos, Player player, CallbackInfo ci) {
        PlayerLecternHook.getActiveHandlers().put(player, world.getBlockEntity(pos));
    }
}
