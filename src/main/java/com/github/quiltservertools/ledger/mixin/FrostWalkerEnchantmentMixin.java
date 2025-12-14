package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.ReplaceDisk;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ReplaceDisk.class)
public abstract class FrostWalkerEnchantmentMixin {
    @ModifyArgs(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private void logFrostWalkerPlacement(Args args, ServerLevel world, int level, EnchantedItemInUse context, Entity entity, Vec3 vec3d) {
        // Frosted ice block is hardcoded in target class
        BlockPos pos = args.get(0);
        BlockState state = args.get(1);
        pos = pos.immutable();
        // TODO 1.21 - Datapacks can use this. The source might need to be renamed
        BlockPlaceCallback.EVENT.invoker().place(world, pos, state, null, Sources.FROST_WALKER,
                entity instanceof Player ? (Player) entity : null);
    }
}
