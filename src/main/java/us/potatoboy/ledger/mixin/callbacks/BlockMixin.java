package us.potatoboy.ledger.mixin.callbacks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.potatoboy.ledger.callbacks.PlayerBlockPlaceCallback;

@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(
            method = "onPlaced", at = @At("TAIL"),
            cancellable = true
    )
    public void ledgerPlayerPlaceBlockCallback(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack, CallbackInfo ci) {
        PlayerBlockPlaceCallback.Companion.getEVENT().invoker().place(world, placer, pos, state, world.getBlockEntity(pos));
    }
}