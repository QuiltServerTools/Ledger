package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.SetBlockCommand;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SetBlockCommand.class)
public class SetBlockCommandMixin {
    private final static ThreadLocal<BlockState> oldState = new ThreadLocal<>();

    @Inject(method = "execute", at = @At("HEAD"))
    private static void storePreviousState(ServerCommandSource source, BlockPos pos, BlockStateArgument block, SetBlockCommand.Mode mode, @Nullable Predicate<CachedBlockPosition> condition, CallbackInfoReturnable<Integer> cir) {
        oldState.set(source.getWorld().getBlockState(pos));
    }

    @Inject(method = "execute", at = @At("RETURN"))
    private static void logSetBlockChange(ServerCommandSource source, BlockPos pos, BlockStateArgument block, SetBlockCommand.Mode mode, @Nullable Predicate<CachedBlockPosition> condition, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        Entity entity = source.getEntity();
        PlayerEntity player = entity instanceof PlayerEntity ? (PlayerEntity) entity : null;

        BlockChangeCallback.EVENT.invoker().changeBlock(source.getWorld(), pos, oldState.get(), block.getBlockState(), null, null, Sources.COMMAND + "/setblock", player);
    }
}
