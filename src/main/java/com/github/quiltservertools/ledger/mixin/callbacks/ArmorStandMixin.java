package com.github.quiltservertools.ledger.mixin.callbacks;

import com.github.quiltservertools.ledger.callbacks.EntityEquipCallback;
import com.github.quiltservertools.ledger.callbacks.EntityRemoveCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
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
    ) private void ledgerEquipEntityInvoker(PlayerEntity player, EquipmentSlot slot, ItemStack playerStack, Hand hand, CallbackInfoReturnable<Boolean> cir){
        ItemStack entityStack = this.getEquippedStack(slot);
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entityStack.isEmpty() && playerStack.isEmpty() || entity == null) {;return;}
        // do nothing no items to swap or entity is non existant

        if (entityStack.isEmpty() && !playerStack.isEmpty()) {
            EntityEquipCallback.EVENT.invoker().equip(playerStack, player.world,
                    entity.getBlockPos(), entity, player);
        }

        else if (!entityStack.isEmpty() && playerStack.isEmpty()){
            EntityRemoveCallback.EVENT.invoker().remove(entityStack, player.world,
                    entity.getBlockPos(),entity, player);
        }
        else{
            EntityRemoveCallback.EVENT.invoker().remove(entityStack, player.world,
                    entity.getBlockPos(),entity, player);

            EntityEquipCallback.EVENT.invoker().equip(playerStack, player.world,
                    entity.getBlockPos(),entity, player);

        }

    }
}


