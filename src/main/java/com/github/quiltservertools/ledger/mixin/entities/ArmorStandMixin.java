package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.EntityKillCallback;
import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.utility.NbtUtils;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin {
    @Unique
    private CompoundTag oldEntityTags;
    @Unique
    private ItemStack oldEntityStack;
    @Unique
    private boolean ledgerBreakLogged;

    @Inject(method = "swapItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"))
    private void legerLogOldEntity(Player player, EquipmentSlot slot, ItemStack playerStack, InteractionHand hand, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        this.oldEntityTags = NbtUtils.INSTANCE.createNbt(entity);
        this.oldEntityStack = entity.getItemBySlot(slot);
    }

    @Inject(method = "swapItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V", shift = At.Shift.AFTER))
    private void ledgerArmorStandInteract(Player player, EquipmentSlot slot, ItemStack playerStack, InteractionHand hand, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!oldEntityStack.isEmpty()) {
            EntityModifyCallback.EVENT.invoker().modify(player.level(), entity.blockPosition(), oldEntityTags, entity, oldEntityStack, player, Sources.REMOVE);
        }
        if (!playerStack.isEmpty()) {
            EntityModifyCallback.EVENT.invoker().modify(player.level(), entity.blockPosition(), oldEntityTags, entity, playerStack, player, Sources.EQUIP);
        }
    }

    // brokenByAnything drops the armor stand's equipment and empties every slot before kill is
    // called, so logging at the kill call would store an armor stand without its items and the
    // rollback would bring back an empty one. Log here, while the equipment is still attached.
    @Inject(method = "brokenByAnything", at = @At("HEAD"))
    private void ledgerArmorStandBroken(ServerLevel world, DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityKillCallback.EVENT.invoker().kill(world, entity.blockPosition(), entity, damageSource);
        this.ledgerBreakLogged = true;
    }

    @Inject(method = "causeDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;kill(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void ledgerArmorStandKill(ServerLevel world, DamageSource damageSource, float amount, CallbackInfo ci) {
        if (ledgerConsumeBreakLogged()) {
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityKillCallback.EVENT.invoker().kill(entity.level(), entity.blockPosition(), entity, damageSource);
    }

    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;kill(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void ledgerArmorStandKill(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (ledgerConsumeBreakLogged()) {
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityKillCallback.EVENT.invoker().kill(world, entity.blockPosition(), entity, source);
    }

    /**
     * Every brokenByAnything call is immediately followed by a kill call, which must not log the
     * armor stand a second time. Paths that kill without breaking (creative, damage that bypasses
     * invulnerability) never set the flag and are still logged by the kill hooks above.
     */
    @Unique
    private boolean ledgerConsumeBreakLogged() {
        if (!this.ledgerBreakLogged) {
            return false;
        }
        this.ledgerBreakLogged = false;
        return true;
    }
}
