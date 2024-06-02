package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(RavagerEntity.class)
public abstract class RavagerEntityMixin extends RaiderEntity {
    protected RavagerEntityMixin(EntityType<? extends RaiderEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyArgs(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;)Z"))
    public void logRavagerBreakingLeaves(Args args) {
        BlockPos pos = args.get(0);
        var world = this.getWorld();
        BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, world.getBlockState(pos), world.getBlockEntity(pos), Registries.ENTITY_TYPE.getId(((RavagerEntity) (Object) this).getType()).getPath());
    }
}
