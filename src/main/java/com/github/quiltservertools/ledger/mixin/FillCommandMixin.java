package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.FillCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FillCommand.class)
public class FillCommandMixin {
    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/argument/BlockStateArgument;setBlockState(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;I)Z"))
    private static boolean setBlockState(BlockStateArgument blockStateArgument, ServerWorld world, BlockPos pos, int flags, ServerCommandSource source) {
        BlockState oldState = world.getBlockState(pos);

        boolean success = blockStateArgument.setBlockState(world, pos, flags);
        if (success) {
            BlockState newState = blockStateArgument.getBlockState();
            Entity entity = source.getEntity();
            PlayerEntity player = entity instanceof PlayerEntity ? (PlayerEntity) entity : null;

            BlockChangeCallback.EVENT.invoker().changeBlock(source.getWorld(), pos.toImmutable(), oldState, newState, null, null, Sources.COMMAND + "/fill", player);
        }

        return success;
    }
}
