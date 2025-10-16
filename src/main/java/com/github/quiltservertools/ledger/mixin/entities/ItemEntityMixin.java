package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.ItemPickUpCallback;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    @Shadow
    public abstract ItemStack getStack();

    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "onPlayerCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;insertStack(Lnet/minecraft/item/ItemStack;)Z"))
    public void storeItemStack(PlayerEntity player, CallbackInfo ci, @Local ItemStack itemStack, @Share("originalItemStack") LocalRef<ItemStack> originalItemStackRef) {
        originalItemStackRef.set(itemStack.copy());
    }

    // insertStack modifies the ItemStack instance of the ItemEntity
    // The player may not be able to pick up all items from the stack
    @Inject(method = "onPlayerCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;insertStack(Lnet/minecraft/item/ItemStack;)Z", shift = At.Shift.AFTER))
    private void logPlayerItemPickUp(PlayerEntity player, CallbackInfo ci, @Local ItemStack modifiedItemStack, @Share("originalItemStack") LocalRef<ItemStack> originalItemStackRef) {
        var originalItemStack = originalItemStackRef.get();

        int modifiedCount = modifiedItemStack.getCount();
        int originalCount = originalItemStack.getCount();
        if (modifiedCount < originalCount) {
            ItemEntity itemEntityCopy = new ItemEntity(this.getEntityWorld(), this.getX(), this.getY(), this.getZ(), originalItemStackRef.get().copyWithCount(originalCount - modifiedCount));
            ItemPickUpCallback.EVENT.invoker().pickUp(itemEntityCopy, player);
        }
    }
}
