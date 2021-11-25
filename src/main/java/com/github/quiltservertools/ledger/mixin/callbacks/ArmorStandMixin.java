package com.github.quiltservertools.ledger.mixin.callbacks;

import com.github.quiltservertools.ledger.actions.EntityModifyActionTypeKt;
import com.github.quiltservertools.ledger.callbacks.EntityKillCallback;
import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStandMixin {

    @Shadow public abstract ItemStack getEquippedStack(EquipmentSlot slot);


    @Inject(method = "equip",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/entity/decoration/ArmorStandEntity;equipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;)V")
    ) private void ledgerArmorStandInteractInvoker(PlayerEntity player, EquipmentSlot slot, ItemStack playerStack, Hand hand, CallbackInfoReturnable<Boolean> cir){
        ItemStack entityStack = this.getEquippedStack(slot);
        Entity entity = (Entity) (Object) this;
        if (entityStack.isEmpty() && playerStack.isEmpty() || entity == null) {;return;}
        // do nothing no items to swap or entity is non existant

        if (entityStack.isEmpty()) {
            EntityModifyCallback.EVENT.invoker().modify(EntityModifyActionTypeKt.EQUIP, player.world,
                    entity.getBlockPos(), entity, playerStack, player);
        }

        else if (playerStack.isEmpty()){
            EntityModifyCallback.EVENT.invoker().modify(EntityModifyActionTypeKt.REMOVE, player.world,
                    entity.getBlockPos(),entity, entityStack, player);
        }

    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/ArmorStandEntity;kill()V")
    ) private void ledgerArmorStandKillInvoker(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
        Entity entity = (Entity) (Object) this;
        EntityKillCallback.EVENT.invoker().kill(entity.world, entity.getBlockPos(),entity,source);
    }
}


