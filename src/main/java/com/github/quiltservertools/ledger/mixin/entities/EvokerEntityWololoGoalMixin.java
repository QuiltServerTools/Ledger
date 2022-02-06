package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EvokerEntity.WololoGoal.class)
public abstract class EvokerEntityWololoGoalMixin {
    @Shadow @Final
    EvokerEntity field_7268;
    @Unique
    private NbtCompound oldEntityTags;

    @Inject(method = "castSpell", 
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SheepEntity;setColor(Lnet/minecraft/util/DyeColor;)V"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    // any way to improve?
    public void legerLogOldEntity(CallbackInfo ci, SheepEntity sheepEntity) {
        if(sheepEntity.getColor() == DyeColor.RED) {return;}
        // multiple evokers target the same sheep
        this.oldEntityTags = sheepEntity.writeNbt(new NbtCompound());
    }


    @Inject(method = "castSpell",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/entity/passive/SheepEntity;setColor(Lnet/minecraft/util/DyeColor;)V"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void ledgerEvokerDyeSheep(CallbackInfo ci, SheepEntity sheepEntity) {
        if(sheepEntity.getColor() == DyeColor.RED) {return;}
        EvokerEntity evokerEntity = this.field_7268;
        EntityModifyCallback.EVENT.invoker().modify(evokerEntity.world, sheepEntity.getBlockPos(), oldEntityTags, sheepEntity, Items.RED_DYE.getDefaultStack(), evokerEntity, Sources.DYE);
    }
}
