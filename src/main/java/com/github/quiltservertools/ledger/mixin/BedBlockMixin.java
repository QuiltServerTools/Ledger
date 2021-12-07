package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.utility.EndCrystalDuck;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
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

    @ModifyArgs(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/world/explosion/Explosion$DestructionType;)Lnet/minecraft/world/explosion/Explosion;"))
    public void correctEndCrystalEntitySource(Args args, DamageSource source, float amount) {
        if (source.getSource() instanceof PlayerEntity) {
            this.causingPlayer = (PlayerEntity) source.getSource();
        } else if (source.getSource() instanceof ProjectileEntity) {
            this.causingPlayer = (PlayerEntity) ((ProjectileEntity) source.getSource()).getOwner();
        }
        args.set(0, (EndCrystalEntity) (Object) this);
    }
}
