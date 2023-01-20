package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EnderDragonEntity.class)
public abstract class EnderDragonEntityMixin {
    @ModifyArgs(method = "destroyBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    private void logEnderDragonBreakingBlocks(Args args) {
        EnderDragonEntity entity = (EnderDragonEntity) (Object) this;
        World world = entity.getEntityWorld();
        BlockPos blockPos = args.get(0);
        BlockBreakCallback.EVENT.invoker().breakBlock(world, blockPos, world.getBlockState(blockPos), world.getBlockEntity(blockPos), Registries.ENTITY_TYPE.getId(entity.getType()).getPath());
    }
}
