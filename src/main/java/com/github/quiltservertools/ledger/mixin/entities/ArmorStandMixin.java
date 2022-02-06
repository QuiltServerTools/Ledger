package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.EntityKillCallback;
import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStandMixin {

    @Unique
    private NbtCompound oldEntityTags;
    @Unique
    private ItemStack oldEntityStack;


    @Inject(method = "equip",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/entity/decoration/ArmorStandEntity;equipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;)V")
    ) private void legerLogOldEntity(PlayerEntity player, EquipmentSlot slot, ItemStack playerStack, Hand hand, CallbackInfoReturnable<Boolean> cir){
        //NbtCompound entityCustomNBT = new NbtCompound();
        //this.writeCustomDataToNbt(entityCustomNBT);
        LivingEntity entity = (LivingEntity) (Object) this;
        this.oldEntityTags = entity.writeNbt(new NbtCompound());
        this.oldEntityStack = entity.getEquippedStack(slot);

        // better way to do this?
        // can custom nbt data be used?
    }

    @Inject(method = "equip",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/entity/decoration/ArmorStandEntity;equipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;)V",shift = At.Shift.AFTER)
    ) private void ledgerArmorStandInteractInvoker(PlayerEntity player, EquipmentSlot slot, ItemStack playerStack, Hand hand, CallbackInfoReturnable<Boolean> cir){
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!playerStack.isEmpty()) {
            EntityModifyCallback.EVENT.invoker().modify(player.world, entity.getBlockPos(), oldEntityTags, entity, playerStack, player, Sources.EQUIP);
        }
        if (!oldEntityStack.isEmpty()){
            EntityModifyCallback.EVENT.invoker().modify(player.world, entity.getBlockPos(), oldEntityTags, entity, oldEntityStack, player, Sources.REMOVE);
        }
        //both fire when swapping items untested

    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/ArmorStandEntity;kill()V")
    ) private void ledgerArmorStandKillInvoker(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityKillCallback.EVENT.invoker().kill(entity.world, entity.getBlockPos(),entity,source);
    }

}


