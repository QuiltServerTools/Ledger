package com.github.quiltservertools.ledger.mixin.callbacks;

import com.github.quiltservertools.ledger.callbacks.EntityEquipCallback;
import com.github.quiltservertools.ledger.callbacks.EntityKillCallback;
import com.github.quiltservertools.ledger.callbacks.EntityRemoveCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameMixin {

    @Shadow protected abstract ItemStack getHeldItemStack();

    @Inject(method = "dropHeldStack",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/entity/decoration/ItemFrameEntity;setHeldItemStack(Lnet/minecraft/item/ItemStack;)V")
    )private void ledgerItemFrameRemoveInvoker(@Nullable Entity player, boolean alwaysDrop, CallbackInfo ci){
        ItemStack itemStack = this.getHeldItemStack();
        if (itemStack.isEmpty() || player == null) {return;} // removed nothing or destroyed by block

        Entity entity = (Entity) (Object) this;
        EntityRemoveCallback.EVENT.invoker().remove(itemStack, entity.world,
                entity.getBlockPos(),entity, ((PlayerEntity) player));
    }

    @Inject(method = "interact",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/entity/decoration/ItemFrameEntity;setHeldItemStack(Lnet/minecraft/item/ItemStack;)V")
    )private void ledgerItemFrameEquipInvoker(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir){
        ItemStack itemStack = player.getStackInHand(hand);
        Entity entity = (Entity) (Object) this;
        EntityEquipCallback.EVENT.invoker().equip(itemStack, player.world,
                entity.getBlockPos(),entity, player);
    }

    @Inject(method = "damage",
            at = @At(value = "RETURN")
    )private void ledgerItemFrameKillInvoker(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
        if (cir.getReturnValue() == Boolean.TRUE){
            Entity entity = (Entity) (Object) this;
            EntityKillCallback.EVENT.invoker().kill(entity.world, entity.getBlockPos(), entity, source);
        }
    }

    @Inject(method = "canStayAttached",
            at = @At(value = "RETURN")
    )private void ledgerItemFrameKillInvoker(CallbackInfoReturnable<Boolean> cir){
        if (cir.getReturnValue() == Boolean.FALSE){
            Entity entity = (Entity) (Object) this;
            EntityKillCallback.EVENT.invoker().kill(entity.world, entity.getBlockPos(), entity, DamageSource.magic(entity,entity));
        }
    }
}
