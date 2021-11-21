package com.github.quiltservertools.ledger.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EndCrystalEntity.class)
public abstract class EndCrystalEntityMixin {

    /*
    * Logs the causing entity when creating end crystal explosions
    * Mojang pass null for the causing entity parameter
     */

    @ModifyArgs(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/world/explosion/Explosion$DestructionType;)Lnet/minecraft/world/explosion/Explosion;"))
    public void correctEndCrystalEntitySource(Args args, DamageSource source, float amount) {
        args.set(0, source.getSource());
    }
}
