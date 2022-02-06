package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {

    @Inject(method = "litServerTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/ItemScatterer;spawn(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", shift = At.Shift.AFTER))
    private static void logCampfireRemoveItem(World world, BlockPos pos, BlockState state, CampfireBlockEntity campfire, CallbackInfo ci) {
        BlockChangeCallback.EVENT.invoker().changeBlock(world, pos, state, world.getBlockState(pos), campfire, world.getBlockEntity(pos), Sources.REMOVE);
    }

}