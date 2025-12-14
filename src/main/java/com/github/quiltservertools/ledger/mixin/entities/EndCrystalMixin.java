package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.utility.PlayerCausable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndCrystal.class)
public abstract class EndCrystalMixin implements PlayerCausable {

    @Unique
    private Player causingPlayer;

    @Unique
    @Override
    public Player getCausingPlayer() {
        return causingPlayer;
    }

    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
    public void correctEndCrystalEntitySource(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getDirectEntity() instanceof Player player) {
            this.causingPlayer = player;
        } else if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof Player player) {
            this.causingPlayer = player;
        }
    }
}
