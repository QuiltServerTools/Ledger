package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.EntityKillCallback;
import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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
public abstract class ItemFrameEntityMixin {

    @Shadow
    public abstract ItemStack getHeldItemStack();

    @Inject(method = "interact",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;setHeldItemStack(Lnet/minecraft/item/ItemStack;)V")
    )
    private void ledgerItemFrameEquipInvoker(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack playerStack = player.getStackInHand(hand);
        Entity entity = (Entity) (Object) this;
        EntityModifyCallback.EVENT.invoker().modify(player.world, entity.getBlockPos(), new NbtCompound(), entity, playerStack, player, Sources.EQUIP);
    }

    @Inject(method = "dropHeldStack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;setHeldItemStack(Lnet/minecraft/item/ItemStack;)V")
    )
    private void ledgerItemFrameRemoveInvoker(@Nullable Entity entityActor, boolean alwaysDrop, CallbackInfo ci) {
        ItemStack entityStack = this.getHeldItemStack();
        if (entityStack.isEmpty() || entityActor == null) { return; } // removed nothing or destroyed by block
        Entity entity = (Entity) (Object) this;
        EntityModifyCallback.EVENT.invoker().modify(entityActor.world, entity.getBlockPos(), new NbtCompound(), entity,entityStack, entityActor, Sources.REMOVE);
    }

    @Inject(method = "interact",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;setRotation(I)V")
    )
    private void ledgerItemFrameRotateInvoker(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        Entity entity = (Entity) (Object) this;
        EntityModifyCallback.EVENT.invoker().modify(player.world, entity.getBlockPos(), new NbtCompound(), entity,null, player, Sources.ROTATE);
    }

    @Inject(method = "damage",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/decoration/AbstractDecorationEntity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z")
    )
    private void ledgerItemFrameKillInvoker(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() == Boolean.TRUE) {
            Entity entity = (Entity) (Object) this;
            EntityKillCallback.EVENT.invoker().kill(entity.world, entity.getBlockPos(), entity, source);
        }
    }

    @Inject(method = "canStayAttached",
            at = @At(value = "RETURN")
    )
    private void ledgerItemFrameKillInvoker(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() == Boolean.FALSE) {
            Entity entity = (Entity) (Object) this;
            EntityKillCallback.EVENT.invoker().kill(entity.world, entity.getBlockPos(), entity, DamageSource.magic(entity,entity));
        }
    }
}
