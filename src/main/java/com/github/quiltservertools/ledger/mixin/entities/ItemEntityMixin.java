package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.ItemPickUpCallback;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    @Shadow
    public abstract ItemStack getItem();

    public ItemEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    public void storeItemStack(Player player, CallbackInfo ci, @Local ItemStack itemStack, @Share("originalItemStack") LocalRef<ItemStack> originalItemStackRef) {
        originalItemStackRef.set(itemStack.copy());
    }

    // insertStack modifies the ItemStack instance of the ItemEntity
    // The player may not be able to pick up all items from the stack
    @Inject(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z", shift = At.Shift.AFTER))
    private void logPlayerItemPickUp(Player player, CallbackInfo ci, @Local ItemStack modifiedItemStack, @Share("originalItemStack") LocalRef<ItemStack> originalItemStackRef) {
        var originalItemStack = originalItemStackRef.get();

        int modifiedCount = modifiedItemStack.getCount();
        int originalCount = originalItemStack.getCount();
        if (modifiedCount < originalCount) {
            ItemEntity itemEntityCopy = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), originalItemStackRef.get().copyWithCount(originalCount - modifiedCount));
            ItemPickUpCallback.EVENT.invoker().pickUp(itemEntityCopy, player);
        }
    }
}
