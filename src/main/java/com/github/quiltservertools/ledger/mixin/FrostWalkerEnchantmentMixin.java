package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.effect.entity.ReplaceDiskEnchantmentEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ReplaceDiskEnchantmentEffect.class)
public abstract class FrostWalkerEnchantmentMixin {
    @ModifyArgs(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private void logFrostWalkerPlacement(Args args, ServerWorld world, int level, EnchantmentEffectContext context, Entity entity, Vec3d vec3d) {
        // Frosted ice block is hardcoded in target class
        BlockPos pos = args.get(0);
        BlockState state = args.get(1);
        pos = pos.toImmutable();
        // TODO 1.21 - Datapacks can use this. The source might need to be renamed
        BlockPlaceCallback.EVENT.invoker().place(world, pos, state, null, Sources.FROST_WALKER,
                entity instanceof PlayerEntity ? (PlayerEntity) entity : null);
    }
}
