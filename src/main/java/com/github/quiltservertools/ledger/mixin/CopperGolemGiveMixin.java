package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.ItemDropCallback;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.task.TargetUtil;
import net.minecraft.entity.passive.CopperGolemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TargetUtil.class)
public class CopperGolemGiveMixin {

    @Inject(method = "give(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private static void onItemGiven(LivingEntity entity, ItemStack stack, Vec3d targetLocation, Vec3d velocityFactor, float yOffset, CallbackInfo ci, @Local ItemEntity itemEntity) {
        if (entity instanceof CopperGolemEntity) {
            ItemDropCallback.EVENT.invoker().drop(itemEntity, entity);
        }
    }
}
