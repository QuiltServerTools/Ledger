package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.commands.FillCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(FillCommand.class)
public abstract class FillCommandMixin {
    @Inject(
            method = "fillBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/commands/arguments/blocks/BlockInput;place(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)Z"
            )
    )
    private static void logFillChanges(
            CommandSourceStack source,
            BoundingBox range,
            BlockInput block,
            @Coerce Object mode,
            Predicate<BlockInWorld> filter,
            boolean strict,
            CallbackInfoReturnable<Integer> cir,
            @Local BlockPos pos) {
        ServerLevel world = source.getLevel();
        BlockState oldState = world.getBlockState(pos);
        BlockEntity oldBlockEntity = world.getBlockEntity(pos);
        BlockState newState = block.getState();
        Entity entity = source.getEntity();
        Player player = entity instanceof Player ? (Player) entity : null;

        if (!oldState.isAir()) {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, pos.immutable(), oldState, oldBlockEntity, Sources.COMMAND, player);
        }

        BlockPlaceCallback.EVENT.invoker().place(world, pos.immutable(), newState, null, Sources.COMMAND, player);
    }
}
