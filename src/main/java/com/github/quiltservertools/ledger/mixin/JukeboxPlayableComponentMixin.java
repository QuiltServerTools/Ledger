package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.JukeboxPlayableComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.block.JukeboxBlock.HAS_RECORD;


@Mixin(JukeboxPlayableComponent.class)
public abstract class JukeboxPlayableComponentMixin {

    @Inject(method = "tryPlayStack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/entity/JukeboxBlockEntity;setStack(Lnet/minecraft/item/ItemStack;)V"))
    private static void ledgerPlayerInsertMusicDisc(World world, BlockPos pos, ItemStack itemStack, PlayerEntity player, CallbackInfoReturnable<ItemActionResult> cir) {
        BlockState blockState = world.getBlockState(pos);

        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                blockState.with(HAS_RECORD, false),
                blockState,
                null,
                world.getBlockEntity(pos),
                Sources.INTERACT,
                player);
    }
}
