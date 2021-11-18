package com.github.quiltservertools.ledger.mixin.callbacks;

import com.github.quiltservertools.ledger.callbacks.EntityEquipCallback;
import com.github.quiltservertools.ledger.callbacks.EntityRemoveCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameMixin {

    @Shadow protected abstract ItemStack getHeldItemStack();

    @Shadow protected abstract ItemStack getAsItemStack();

    @Inject(method = "dropHeldStack",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/entity/decoration/ItemFrameEntity;setHeldItemStack(Lnet/minecraft/item/ItemStack;)V")
    )private void ledgerPlayerEntityInteractInvoker(Entity player, boolean alwaysDrop, CallbackInfo ci){
        ItemStack itemStack = this.getHeldItemStack();
        if (itemStack.isEmpty()) {return;} // could log as break

        Entity entity = (Entity) (Object) this;
        EntityRemoveCallback.EVENT.invoker().remove(itemStack, player.world,
                entity.getBlockPos(),entity, (PlayerEntity) player);
    }

    @Inject(method = "interact",
            at = @At(value = "INVOKE",
                    target ="Lnet/minecraft/entity/decoration/ItemFrameEntity;setHeldItemStack(Lnet/minecraft/item/ItemStack;)V")
    )private void ledgerPlayerEntityInteractInvoker(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir){
        ItemStack itemStack = player.getStackInHand(hand);
        Entity entity = (Entity) (Object) this;
        EntityEquipCallback.EVENT.invoker().equip(itemStack, player.world,
                entity.getBlockPos(),entity, player);
    }
}
