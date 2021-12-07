package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.item.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.block.JukeboxBlock.HAS_RECORD;


@Mixin(MusicDiscItem.class)
public abstract class MusicDiscItemMixin{

    @Inject(method = "useOnBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/JukeboxBlock;setRecord(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/item/ItemStack;)V",
            shift = At.Shift.AFTER),
            cancellable = true)
    public void ledgerPlayerPlaceBlockCallback(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState blockState = world.getBlockState(pos);

        BlockChangeCallback.EVENT.invoker().changeBlock(
                context.getWorld(),
                pos,
                blockState.with(HAS_RECORD, false),
                blockState,
                null,
                world.getBlockEntity(pos),
                Sources.INSERT,
                context.getPlayer());
    }
}