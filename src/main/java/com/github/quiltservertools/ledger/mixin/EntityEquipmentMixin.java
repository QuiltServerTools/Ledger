package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.ItemDropCallback;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityEquipment.class)
public abstract class EntityEquipmentMixin {
    // Drops armor and offhand when a player dies (called from Inventory#dropAll). Only players are
    // logged, matching the regular drop hook in ServerPlayerMixin.
    @WrapOperation(method = "dropAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean ledgerLogDeathDrop(Level world, Entity entity, Operation<Boolean> original, @Local(argsOnly = true) LivingEntity owner) {
        boolean added = original.call(world, entity);
        if (added && owner instanceof Player player && entity instanceof ItemEntity itemEntity) {
            ItemDropCallback.EVENT.invoker().drop(itemEntity, player);
        }
        return added;
    }
}
