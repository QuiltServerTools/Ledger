package com.github.quiltservertools.ledger.mixin.entities.silverfish;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
        targets = "net.minecraft.entity.mob.SilverfishEntity$WanderAndInfestGoal"
)
public abstract class WanderAndInfestGoalMixin extends WanderAroundGoal {

    public WanderAndInfestGoalMixin(PathAwareEntity mob, double speed) {
        super(mob, speed);
    }

    @WrapOperation(
            method = "start",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldAccess;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"
            )
    )
    private boolean logSilverFishInfestBlock(
            WorldAccess worldAccess,
            BlockPos pos,
            BlockState state,
            int flags,
            Operation<Boolean> original
    ) {
        BlockState oldState = worldAccess.getBlockState(pos);
        String source = Registries.ENTITY_TYPE.getId(this.mob.getType()).getPath();

        BlockChangeCallback.EVENT.invoker()
                .changeBlock(
                        this.mob.getWorld(),
                        pos,
                        oldState,
                        state,
                        null,
                        null,
                        source
                );

        return original.call(worldAccess, pos, state, flags);
    }
}
