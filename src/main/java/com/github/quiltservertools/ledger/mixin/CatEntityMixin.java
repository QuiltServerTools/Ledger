package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CatEntity.class)
public abstract class CatEntityMixin {

    @Inject(method = "interactMob",
            at = @At(value = "INVOKE",target = "Lnet/minecraft/entity/passive/CatEntity;setCollarColor(Lnet/minecraft/util/DyeColor;)V"))
    private void ledgerDogCollarColour(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir){
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityModifyCallback.EVENT.invoker().modify(player.world, entity.getBlockPos(), entity, null,player.getStackInHand(hand), player, Sources.DYE);
    }

}


