package com.github.quiltservertools.ledger.mixin.entities.silverfish;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
        targets = "net.minecraft.entity.mob.SilverfishEntity$CallForHelpGoal"
)
public class CallForHelpGoalMixin {

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;)Z"
            )
    )
    private boolean logSilverFishBreakInfestedBlock(
            World world,
            BlockPos pos,
            boolean drop,
            Entity entity,
            Operation<Boolean> original
    ) {
        String source = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
        BlockBreakCallback.EVENT.invoker()
                .breakBlock(
                        world,
                        pos,
                        world.getBlockState(pos),
                        null,
                        source
                );

        return original.call(world, pos, drop, entity);
    }

}
