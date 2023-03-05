package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JukeboxBlock.class)
public abstract class JukeBoxBlockMixin {

    @Shadow
    @Final
    public static BooleanProperty HAS_RECORD;

    @Inject(method = "onUse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/entity/JukeboxBlockEntity;dropRecord()V"))
    private void ledgerLogDiscRemoved(BlockState blockState, World world, BlockPos pos, PlayerEntity player, Hand hand,
                                      BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                blockState,
                blockState.with(HAS_RECORD, false),
                world.getBlockEntity(pos),
                null,
                Sources.INTERACT,
                player);
    }
}
