package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.utility.NbtUtils;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CopperGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CopperGolemEntity.class)
public class CopperGolemEntityMixin {
    @Inject(method = "interactMob", at = @At(value = "HEAD"))
    public void setOldEntityTags(CallbackInfoReturnable<ActionResult> cir, @Share("oldEntityTags") LocalRef<NbtCompound> oldEntityTagsRef) {
        LivingEntity entity = (LivingEntity) (Object) this;
        oldEntityTagsRef.set(NbtUtils.INSTANCE.createNbt(entity));
    }

    @Inject(method = "interactMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/CopperGolemEntity;sheared(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/sound/SoundCategory;Lnet/minecraft/item/ItemStack;)V", shift = At.Shift.AFTER))
    public void afterShear(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir, @Share("oldEntityTags") LocalRef<NbtCompound> oldEntityTagsRef) {
        World world = player.getEntityWorld();
        CopperGolemEntity copperGolemEntity = (CopperGolemEntity) (Object) this;
        BlockPos pos = copperGolemEntity.getBlockPos();
        EntityModifyCallback.EVENT.invoker().modify(world, pos, oldEntityTagsRef.get(), copperGolemEntity, null, player, Sources.SHEAR);
    }

    @Inject(method = "interactMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;syncWorldEvent(Lnet/minecraft/entity/Entity;ILnet/minecraft/util/math/BlockPos;I)V", ordinal = 0))
    public void onWax(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir, @Share("oldEntityTags") LocalRef<NbtCompound> oldEntityTagsRef) {
        World world = player.getEntityWorld();
        CopperGolemEntity copperGolemEntity = (CopperGolemEntity) (Object) this;
        BlockPos pos = copperGolemEntity.getBlockPos();
        EntityModifyCallback.EVENT.invoker().modify(world, pos, oldEntityTagsRef.get(), copperGolemEntity, null, player, Sources.WAX);
    }

    // This catches 2 cases - removing oxidation with an axe, and using an axe on an unaffected copper golem resetting their next oxidation age.
    @Inject(method = "interactMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundFromEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"))
    public void onAxe(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir, @Share("oldEntityTags") LocalRef<NbtCompound> oldEntityTagsRef) {
        World world = player.getEntityWorld();
        CopperGolemEntity copperGolemEntity = (CopperGolemEntity) (Object) this;
        BlockPos pos = copperGolemEntity.getBlockPos();
        EntityModifyCallback.EVENT.invoker().modify(world, pos, oldEntityTagsRef.get(), copperGolemEntity, null, player, Sources.INTERACT);
    }

    @Inject(method = "turnIntoStatue", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/CopperGolemEntity;discard()V"))
    public void onTurningIntoStatue(ServerWorld world, CallbackInfo ci) {
        CopperGolemEntity copperGolemEntity = (CopperGolemEntity) (Object) this;
        BlockPos pos = copperGolemEntity.getBlockPos();
        EntityModifyCallback.EVENT.invoker().modify(world, pos, NbtUtils.INSTANCE.createNbt(copperGolemEntity), copperGolemEntity, null, null, Sources.STATUE);
    }
}
