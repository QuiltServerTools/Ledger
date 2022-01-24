package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.FillCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

@Mixin(FillCommand.class)
public abstract class FillCommandMixin {
    @Inject(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/command/argument/BlockStateArgument;setBlockState(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;I)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void logFillChanges(ServerCommandSource source, BlockBox range, BlockStateArgument block, @Coerce Object mode, Predicate filter, CallbackInfoReturnable<Integer> cir, List list, ServerWorld serverWorld, int j, Iterator var9, BlockPos pos) {
        ServerWorld world = source.getWorld();
        BlockState oldState = world.getBlockState(pos);
        BlockEntity oldBlockEntity = world.getBlockEntity(pos);
        BlockState newState = block.getBlockState();
        Entity entity = source.getEntity();
        PlayerEntity player = entity instanceof PlayerEntity ? (PlayerEntity) entity : null;

        if (!oldState.isAir()) {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, pos.toImmutable(), oldState, oldBlockEntity, Sources.COMMAND, player);
        }

        BlockPlaceCallback.EVENT.invoker().place(world, pos.toImmutable(), newState, null, Sources.COMMAND, player);
    }
}
