package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.utility.PlayerCausable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ExplosiveProjectileEntity.class)
public abstract class ExplosiveProjectileEntityMixin implements PlayerCausable {
    @Nullable
    @Override
    public PlayerEntity getCausingPlayer() {
        if (((ExplosiveProjectileEntity) (Object) this).getOwner() instanceof MobEntity entity) {
            if (entity.getTarget() instanceof PlayerEntity player) {
                return player;
            }
        }
        return null;
    }
}
