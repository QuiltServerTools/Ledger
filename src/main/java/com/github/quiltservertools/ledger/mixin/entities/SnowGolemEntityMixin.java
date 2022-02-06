package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(SnowGolemEntity.class)
public abstract class SnowGolemEntityMixin {
    @ModifyArgs(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    public void logSnowGolemSnow(Args args) {
        BlockPos pos = args.get(0);
        BlockState state = args.get(1);
        BlockPlaceCallback.EVENT.invoker().place(((LivingEntity) (Object) this).world, pos, state, null, Sources.SNOW_GOLEM);
    }

    @Inject(method = "interactMob",
            at = @At(value = "INVOKE",target = "Lnet/minecraft/entity/passive/SnowGolemEntity;sheared(Lnet/minecraft/sound/SoundCategory;)V"))
    private void ledgerDogCollarColour(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir){
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityModifyCallback.EVENT.invoker().modify(player.world, entity.getBlockPos(), new NbtCompound(), entity, null, player, Sources.SHEAR);
    }
}
