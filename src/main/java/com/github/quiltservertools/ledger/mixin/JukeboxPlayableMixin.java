package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD;

@Mixin(JukeboxPlayable.class)
public abstract class JukeboxPlayableMixin {

    @Inject(method = "tryInsertIntoJukebox", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/JukeboxBlockEntity;setTheItem(Lnet/minecraft/world/item/ItemStack;)V"))
    private static void ledgerPlayerInsertMusicDisc(Level world, BlockPos pos, ItemStack itemStack, Player player, CallbackInfoReturnable<InteractionResult> cir) {
        BlockState blockState = world.getBlockState(pos);

        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                blockState.setValue(HAS_RECORD, false),
                blockState,
                null,
                world.getBlockEntity(pos),
                Sources.INTERACT,
                player);
    }
}
