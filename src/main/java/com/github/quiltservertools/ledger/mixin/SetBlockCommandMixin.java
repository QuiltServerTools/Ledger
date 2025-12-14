package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import java.util.function.Predicate;

@Mixin(SetBlockCommand.class)
public abstract class SetBlockCommandMixin {
    @ModifyArgs(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/commands/arguments/blocks/BlockInput;place(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)Z"
            )
    )
    private static void logSetBlockChange(Args args, CommandSourceStack source, BlockPos pos, BlockInput block, SetBlockCommand.Mode mode, @Nullable Predicate<BlockInWorld> condition, boolean strict) {
        ServerLevel world = args.get(0);

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
