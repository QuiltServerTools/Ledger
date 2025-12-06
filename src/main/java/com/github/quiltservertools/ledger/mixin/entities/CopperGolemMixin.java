package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.listeners.EntityCallbackListenerKt;
import com.github.quiltservertools.ledger.utility.NbtUtils;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.coppergolem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CopperGolem.class)
public class CopperGolemMixin {
    @Inject(method = "mobInteract", at = @At(value = "HEAD"))
    public void setOldEntityTags(CallbackInfoReturnable<InteractionResult> cir, @Share("oldEntityTags") LocalRef<CompoundTag> oldEntityTagsRef) {
        LivingEntity entity = (LivingEntity) (Object) this;
        oldEntityTagsRef.set(NbtUtils.INSTANCE.createNbt(entity));
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/coppergolem/CopperGolem;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V", shift = At.Shift.AFTER))
    public void afterShear(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, @Share("oldEntityTags") LocalRef<CompoundTag> oldEntityTagsRef) {
        Level world = player.level();
        CopperGolem copperGolemEntity = (CopperGolem) (Object) this;
        BlockPos pos = copperGolemEntity.blockPosition();
        EntityModifyCallback.EVENT.invoker().modify(world, pos, oldEntityTagsRef.get(), copperGolemEntity, null, player, Sources.SHEAR);
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(Lnet/minecraft/world/entity/Entity;ILnet/minecraft/core/BlockPos;I)V", ordinal = 0))
    public void onWax(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, @Share("oldEntityTags") LocalRef<CompoundTag> oldEntityTagsRef) {
        Level world = player.level();
        CopperGolem copperGolemEntity = (CopperGolem) (Object) this;
        BlockPos pos = copperGolemEntity.blockPosition();
        EntityModifyCallback.EVENT.invoker().modify(world, pos, oldEntityTagsRef.get(), copperGolemEntity, null, player, Sources.WAX);
    }

    // This catches 2 cases - removing oxidation with an axe, and using an axe on an unaffected copper golem resetting their next oxidation age.
    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    public void onAxe(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, @Share("oldEntityTags") LocalRef<CompoundTag> oldEntityTagsRef) {
        Level world = player.level();
        CopperGolem copperGolemEntity = (CopperGolem) (Object) this;
        BlockPos pos = copperGolemEntity.blockPosition();
        EntityModifyCallback.EVENT.invoker().modify(world, pos, oldEntityTagsRef.get(), copperGolemEntity, null, player, Sources.INTERACT);
    }

    @Inject(method = "turnToStatue", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/coppergolem/CopperGolem;discard()V"))
    public void onTurningIntoStatue(ServerLevel world, CallbackInfo ci) {
        CopperGolem copperGolemEntity = (CopperGolem) (Object) this;
        BlockPos pos = copperGolemEntity.blockPosition();
        EntityCallbackListenerKt.onKill(world, pos, copperGolemEntity, Sources.STATUE);

        BlockState blockState = world.getBlockState(pos);
        BlockEntity be = world.getBlockEntity(pos);
        BlockPlaceCallback.EVENT.invoker().place(world, pos, blockState, be, Sources.STATUE);
    }
}
