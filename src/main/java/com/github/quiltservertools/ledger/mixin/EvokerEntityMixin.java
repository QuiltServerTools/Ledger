package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EvokerEntity.WololoGoal.class)
public abstract class EvokerEntityMixin {
    @Shadow @Final
    EvokerEntity field_7268;

    @Inject(method = "castSpell", at = @At(value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/entity/mob/EvokerEntity;getWololoTarget()Lnet/minecraft/entity/passive/SheepEntity;"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    @SuppressWarnings("Method")
    public void ledgerEvokerDyeSheep(CallbackInfo ci, SheepEntity sheepEntity) {
        if(sheepEntity.getColor() == DyeColor.RED) {return;}
        // multiple evokers target the same sheep
        LivingEntity entity = this.field_7268;
        EntityModifyCallback.EVENT.invoker().modify(this.field_7268.world, sheepEntity.getBlockPos(), sheepEntity, Items.RED_DYE.getDefaultStack(), entity, Sources.DYE);
    }
}
