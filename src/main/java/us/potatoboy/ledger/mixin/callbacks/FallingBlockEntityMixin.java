package us.potatoboy.ledger.mixin.callbacks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import us.potatoboy.ledger.callbacks.BlockBurnCallback;
import us.potatoboy.ledger.callbacks.BlockFallCallback;
import us.potatoboy.ledger.callbacks.BlockLandCallback;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {
	@Shadow
	private BlockState block;

	@Inject(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"
			),
			locals = LocalCapture.CAPTURE_FAILEXCEPTION
	)
	private void ledgerBlockFallInvoker(CallbackInfo ci, Block block, BlockPos blockPos) {
		FallingBlockEntity entity = (FallingBlockEntity) (Object) this;

		BlockFallCallback.Companion.getEVENT().invoker().fall(entity.world, blockPos, this.block);
	}

	@Inject(
			method = "tick",
			at = @At(
					value = "INVOKE_ASSIGN",
					target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"
			),
			locals = LocalCapture.CAPTURE_FAILEXCEPTION
	)
	private void ledgerBlockLandInvoker(CallbackInfo ci, Block block, BlockPos blockPos2, boolean bl, boolean bl2, BlockState blockState) {
		FallingBlockEntity entity = (FallingBlockEntity) (Object) this;

		BlockLandCallback.Companion.getEVENT().invoker().land(entity.world, blockPos2, this.block);
	}
}
