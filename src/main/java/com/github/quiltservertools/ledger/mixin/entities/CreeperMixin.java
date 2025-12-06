package com.github.quiltservertools.ledger.mixin.entities;

import com.github.quiltservertools.ledger.utility.PlayerCausable;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Creeper.class)
public abstract class CreeperMixin implements PlayerCausable {
    @Unique
    private Player ignitePlayer;

    @Unique
    @Override
    public Player getCausingPlayer() {
        if (ignitePlayer != null) return ignitePlayer;
        if (((Creeper) (Object) this).getTarget() instanceof Player player) return player;
        return null;
    }
    
    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Creeper;ignite()V"))
    private void trackIgnitingPlayer(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ignitePlayer = player;
    }
}
