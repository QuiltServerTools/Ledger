package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import net.minecraft.entity.LightningEntity;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LightningEntity.class)
public abstract class LightningEntityMixin {
    @ModifyArgs(method = "spawnFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private void logFirePlacedByLightningBolt(Args args) {
        LightningEntity entity = (LightningEntity) (Object) this;
        BlockPlaceCallback.EVENT.invoker().place(entity.getEntityWorld(), args.get(0), args.get(1), null, Registry.ENTITY_TYPE.getId(entity.getType()).getPath());
    }
}
