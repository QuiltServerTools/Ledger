package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.SetBlockCommand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SetBlockCommand.class)
public class SetBlockCommandMixin {
    private final static ThreadLocal<BlockState> oldState = new ThreadLocal<>();
    private final static ThreadLocal<BlockEntity> oldBlockEntity = new ThreadLocal<>();

    @Inject(method = "execute", at = @At("HEAD"))
    private static void storePreviousState(ServerCommandSource source, BlockPos pos, BlockStateArgument block, SetBlockCommand.Mode mode, @Nullable Predicate<CachedBlockPosition> condition, CallbackInfoReturnable<Integer> cir) {
        ServerWorld world = source.getWorld();

        oldState.set(world.getBlockState(pos));
        oldBlockEntity.set(world.getBlockEntity(pos));
    }

    @Inject(method = "execute", at = @At("RETURN"))
    private static void logSetBlockChange(ServerCommandSource source, BlockPos pos, BlockStateArgument block, SetBlockCommand.Mode mode, @Nullable Predicate<CachedBlockPosition> condition, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        Entity entity = source.getEntity();
        PlayerEntity player = entity instanceof PlayerEntity ? (PlayerEntity) entity : null;

        ServerWorld world = source.getWorld();
        if (!oldState.get().isAir()) {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, oldState.get(), oldBlockEntity.get(), Sources.COMMAND, player);
        }

        BlockPlaceCallback.EVENT.invoker().place(world, pos, block.getBlockState(), null, Sources.COMMAND, player);
    }
}
