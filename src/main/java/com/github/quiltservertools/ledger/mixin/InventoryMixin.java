package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.ItemDropCallback;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    // On death the inventory spawns its items directly instead of going through Player#drop, so
    // the drop hook in ServerPlayerMixin never sees them. Armor and offhand are handled in
    // EntityEquipmentMixin.
    @WrapOperation(method = "dropAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean ledgerLogDeathDrop(Level world, Entity entity, Operation<Boolean> original) {
        boolean added = original.call(world, entity);
        if (added && entity instanceof ItemEntity itemEntity) {
            ItemDropCallback.EVENT.invoker().drop(itemEntity, this.player);
        }
        return added;
    }
}
