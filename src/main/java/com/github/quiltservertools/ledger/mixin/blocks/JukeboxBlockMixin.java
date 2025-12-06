package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JukeboxBlock.class)
public abstract class JukeboxBlockMixin {

    @Shadow
    @Final
    public static BooleanProperty HAS_RECORD;

    @Inject(method = "useWithoutItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/JukeboxBlockEntity;popOutTheItem()V"))
    private void ledgerLogDiscRemoved(BlockState blockState, Level world, BlockPos pos, Player player,
                                      BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                blockState,
                blockState.setValue(HAS_RECORD, false),
                world.getBlockEntity(pos),
                null,
                Sources.INTERACT,
                player);
    }
}
