package us.potatoboy.ledger.mixin.callbacks;

import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import us.potatoboy.ledger.callbacks.BlockBurnCallback;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    @Inject(
            method = "trySpreadingFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void ledgerBlockBurnBreakInvoker(World world, BlockPos pos, int spreadFactor, Random rand, int currentAge, CallbackInfo ci, int i, BlockState blockState) {
        if (blockState.getBlock() != Blocks.FIRE) {
            BlockBurnCallback.Companion.getEVENT().invoker().burn(world, pos, blockState, world.getBlockEntity(pos) != null ? world.getBlockEntity(pos) : null);
        }
    }

    @Inject(
            method = "trySpreadingFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void ledgerBlockBurnReplaceInvoker(World world, BlockPos pos, int spreadFactor, Random rand, int currentAge, CallbackInfo ci, int i, BlockState blockState) {
        if (blockState.getBlock() != Blocks.FIRE) {
            BlockBurnCallback.Companion.getEVENT().invoker().burn(world, pos, blockState, world.getBlockEntity(pos) != null ? world.getBlockEntity(pos) : null);
        }
    }
}
