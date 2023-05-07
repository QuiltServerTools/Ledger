package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EnderDragonEntity.class)
public abstract class EnderDragonEntityMixin {
    @Inject(method = "destroyBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void logEnderDragonBreakingBlocks(Box box, CallbackInfoReturnable<Boolean> cir, int i, int j, int k, int l, int m, int n, boolean bl, boolean bl2, int o, int p, int q, BlockPos blockPos) {
        EnderDragonEntity entity = (EnderDragonEntity) (Object) this;
        World world = entity.getEntityWorld();
        BlockBreakCallback.EVENT.invoker().breakBlock(world, blockPos, world.getBlockState(blockPos), world.getBlockEntity(blockPos), Registries.ENTITY_TYPE.getId(entity.getType()).getPath());
    }
}
