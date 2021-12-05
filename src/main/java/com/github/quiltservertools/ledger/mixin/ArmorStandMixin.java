package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.Ledger;
import com.github.quiltservertools.ledger.LedgerKt;
import com.github.quiltservertools.ledger.callbacks.EntityKillCallback;
import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.database.DatabaseManager;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.EulerAngle;
import org.jetbrains.exposed.sql.Op;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStandMixin {

    @Shadow public abstract ItemStack getEquippedStack(EquipmentSlot slot);

    private boolean hasChanged;

    @Inject(method = "equip",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/entity/decoration/ArmorStandEntity;equipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;)V")
    ) private void ledgerArmorStandInteractInvoker(PlayerEntity player, EquipmentSlot slot, ItemStack playerStack, Hand hand, CallbackInfoReturnable<Boolean> cir){
        ItemStack entityStack = this.getEquippedStack(slot);
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entityStack.isEmpty() && playerStack.isEmpty() || entity == null) { return; }
        // do nothing no items to swap or entity is non-existent

        if (entityStack.isEmpty()) {
            EntityModifyCallback.EVENT.invoker().modify(player.world, entity.getBlockPos(), entity, playerStack, player, Sources.EQUIP);
        }
        else if (playerStack.isEmpty()){
            EntityModifyCallback.EVENT.invoker().modify(player.world, entity.getBlockPos(), entity, entityStack, player, Sources.REMOVE);
        }

    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/ArmorStandEntity;kill()V")
    ) private void ledgerArmorStandKillInvoker(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityKillCallback.EVENT.invoker().kill(entity.world, entity.getBlockPos(),entity,source);
    }

// has issues, rollback/restore use same methods. also do not actually roll them back for some reason.

    @Inject(method = "tick", at = @At(value = "TAIL")
    ) private void ledgerArmorStandRotationInvoker(CallbackInfo ci){
        LivingEntity entity = (LivingEntity) (Object) this;
        if (hasChanged) {
            EntityModifyCallback.EVENT.invoker().modify(entity.world, entity.getBlockPos(), entity, null, null, Sources.ROTATE);
            hasChanged = false;
            LedgerKt.logInfo("LOGGED");
        }
    }
    @Inject(method = "setHeadRotation", at = @At(value = "TAIL")
    ) private void ledgerArmorStandRotationHasChanged(CallbackInfo ci){
        hasChanged = true;
        LedgerKt.logInfo("true");
    }
    @Inject(method = "setBodyRotation", at = @At(value = "TAIL")
    ) private void ledgerArmorStandRotationHasChanged2(CallbackInfo ci){
        hasChanged = true;
        LedgerKt.logInfo("true");
    }
    @Inject(method = "setLeftArmRotation", at = @At(value = "TAIL")
    ) private void ledgerArmorStandRotationHasChanged3(CallbackInfo ci){
        hasChanged = true;
        LedgerKt.logInfo("true");
    }
    @Inject(method = "setLeftLegRotation", at = @At(value = "TAIL")
    ) private void ledgerArmorStandRotationHasChanged4(CallbackInfo ci){
        hasChanged = true;
        LedgerKt.logInfo("true");
    }
    @Inject(method = "setRightArmRotation", at = @At(value = "TAIL")
    ) private void ledgerArmorStandRotationHasChanged5(CallbackInfo ci){
        hasChanged = true;
        LedgerKt.logInfo("true");
    }
    @Inject(method = "setRightLegRotation", at = @At(value = "TAIL")
    ) private void ledgerArmorStandRotationHasChanged6(CallbackInfo ci){
        hasChanged = true;
        LedgerKt.logInfo("true");
    }

}


