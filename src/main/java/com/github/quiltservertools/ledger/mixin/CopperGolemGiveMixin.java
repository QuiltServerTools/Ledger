package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.ItemDropCallback;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.animal.coppergolem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BehaviorUtils.class)
public class CopperGolemGiveMixin {

    @Inject(method = "throwItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;spawnEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static void onItemGiven(LivingEntity entity, ItemStack stack, Vec3 targetLocation, Vec3 velocityFactor, float yOffset, CallbackInfo ci, @Local ItemEntity itemEntity) {
        if (entity instanceof CopperGolem) {
            ItemDropCallback.EVENT.invoker().drop(itemEntity, entity);
        }
    }
}
