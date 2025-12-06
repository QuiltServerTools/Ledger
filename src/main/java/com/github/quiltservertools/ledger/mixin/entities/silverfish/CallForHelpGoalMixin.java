package com.github.quiltservertools.ledger.mixin.entities.silverfish;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
        targets = "net.minecraft.world.entity.monster.Silverfish$SilverfishWakeUpFriendsGoal"
)
public class CallForHelpGoalMixin {

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private boolean logSilverFishBreakInfestedBlock(
            Level world,
            BlockPos pos,
            boolean drop,
            Entity entity,
            Operation<Boolean> original
    ) {
        String source = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
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
