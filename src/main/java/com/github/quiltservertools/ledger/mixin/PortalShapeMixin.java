package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PortalShape.class)
public abstract class PortalShapeMixin {

    @Inject(method = "method_64315", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static void logPortalPlacement(LevelAccessor worldAccess, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (worldAccess instanceof ServerLevel world) {
            BlockPlaceCallback.EVENT.invoker().place(world, pos.immutable(), state, null, Sources.PORTAL);
        }
    }
}
