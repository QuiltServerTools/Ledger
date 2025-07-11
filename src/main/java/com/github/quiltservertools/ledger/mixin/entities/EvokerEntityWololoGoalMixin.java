package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.utility.NbtUtils;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EvokerEntity.WololoGoal.class)
public abstract class EvokerEntityWololoGoalMixin {
    @Unique
    private NbtCompound oldEntityTags;

    @Inject(method = "castSpell", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SheepEntity;setColor(Lnet/minecraft/util/DyeColor;)V"))
    public void legerLogOldEntity(CallbackInfo ci, @Local SheepEntity sheepEntity) {
        if (sheepEntity.getColor() != DyeColor.RED) {
            this.oldEntityTags = NbtUtils.INSTANCE.createNbt(sheepEntity);
        }
    }

    @Inject(method = "castSpell", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SheepEntity;setColor(Lnet/minecraft/util/DyeColor;)V", shift = At.Shift.AFTER))
    public void ledgerEvokerDyeSheep(CallbackInfo ci, @Local SheepEntity sheepEntity) {
        if (oldEntityTags != null) {
            EntityModifyCallback.EVENT.invoker().modify(sheepEntity.getWorld(), sheepEntity.getBlockPos(), oldEntityTags, sheepEntity, Items.RED_DYE.getDefaultStack(), null, Sources.DYE);
        }
    }
}
