package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.EntityKillCallback;
import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.utility.NbtUtils;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {
    @Shadow
    public abstract ItemStack getItem();

    @Unique
    private CompoundTag oldEntityTags;

    @Inject(method = "interact", at = @At(value = "HEAD"))
    private void ledgerLogOldEntity(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Entity entity = (Entity) (Object) this;
        oldEntityTags = NbtUtils.INSTANCE.createNbt(entity);
    }

    @Inject(method = "dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;setItem(Lnet/minecraft/world/item/ItemStack;)V"))
    private void ledgerLogOldEntity2(ServerLevel world, Entity entityActor, boolean dropSelf, CallbackInfo ci) {
        if (entityActor == null) {
            return;
        }
        Entity entity = (Entity) (Object) this;
        oldEntityTags = NbtUtils.INSTANCE.createNbt(entity);
    }

    @Inject(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;setItem(Lnet/minecraft/world/item/ItemStack;)V", shift = At.Shift.AFTER))
    private void ledgerItemFrameEquip(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack playerStack = player.getItemInHand(hand);
        Entity entity = (Entity) (Object) this;
        EntityModifyCallback.EVENT.invoker().modify(player.level(), entity.blockPosition(), oldEntityTags, entity, playerStack, player, Sources.EQUIP);
    }

    @Inject(method = "dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;setItem(Lnet/minecraft/world/item/ItemStack;)V"))
    private void ledgerItemFrameRemove(ServerLevel world, Entity entityActor, boolean dropSelf, CallbackInfo ci) {
        ItemStack entityStack = this.getItem();
        if (entityStack.isEmpty() || entityActor == null) {
            return;
        }
        Entity entity = (Entity) (Object) this;
        EntityModifyCallback.EVENT.invoker().modify(entityActor.level(), entity.blockPosition(), oldEntityTags, entity, entityStack, entityActor, Sources.REMOVE);
    }

    @Inject(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;setRotation(I)V", shift = At.Shift.AFTER))
    private void ledgerItemFrameRotate(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Entity entity = (Entity) (Object) this;
        EntityModifyCallback.EVENT.invoker().modify(player.level(), entity.blockPosition(), oldEntityTags, entity, null, player, Sources.ROTATE);
    }

    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/HangingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private void ledgerItemFrameKill(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        EntityKillCallback.EVENT.invoker().kill(entity.level(), entity.blockPosition(), entity, source);
    }

    @Inject(method = "survives", at = @At(value = "RETURN"))
    private void ledgerItemFrameKill(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() == Boolean.FALSE) {
            Entity entity = (Entity) (Object) this;
            EntityKillCallback.EVENT.invoker().kill(entity.level(), entity.blockPosition(), entity, entity.damageSources().magic());
        }
    }
}
