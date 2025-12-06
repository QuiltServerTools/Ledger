package com.github.quiltservertools.ledger.mixin.entities.silverfish;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
        targets = "net.minecraft.world.entity.monster.Silverfish$SilverfishMergeWithStoneGoal"
)
public abstract class WanderAndInfestGoalMixin extends RandomStrollGoal {

    public WanderAndInfestGoalMixin(PathfinderMob mob, double speed) {
        super(mob, speed);
    }

    @WrapOperation(
            method = "start",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/LevelAccessor;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            )
    )
    private boolean logSilverFishInfestBlock(
            LevelAccessor worldAccess,
            BlockPos pos,
            BlockState state,
            int flags,
            Operation<Boolean> original
    ) {
        BlockState oldState = worldAccess.getBlockState(pos);
        String source = BuiltInRegistries.ENTITY_TYPE.getKey(this.mob.getType()).getPath();

        BlockChangeCallback.EVENT.invoker()
                .changeBlock(
                        this.mob.level(),
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
