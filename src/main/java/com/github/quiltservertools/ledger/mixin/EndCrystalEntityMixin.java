package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.utility.EndCrystalDuck;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EndCrystalEntity.class)
public abstract class EndCrystalEntityMixin implements EndCrystalDuck {

    @Unique
    private PlayerEntity causingPlayer;

    @Unique
    @Override
    public PlayerEntity getCausingPlayer() {
        return causingPlayer;
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/world/explosion/ExplosionBehavior;DDDFZLnet/minecraft/world/World$class_7867;)Lnet/minecraft/world/explosion/Explosion;"))
    public void correctEndCrystalEntitySource(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getSource() instanceof PlayerEntity player) {
            this.causingPlayer = player;
        } else if (source.getSource() instanceof ProjectileEntity projectile && projectile.getOwner() instanceof PlayerEntity player) {
            this.causingPlayer = player;
        }
    }
}
