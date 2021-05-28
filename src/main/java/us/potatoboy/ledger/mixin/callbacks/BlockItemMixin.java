package us.potatoboy.ledger.mixin.callbacks;

import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import us.potatoboy.ledger.callbacks.PlayerBlockPlaceCallback;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {
	public BlockItemMixin(Settings settings) {
		super(settings);
	}

	@Inject(
			method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ActionResult;success(Z)Lnet/minecraft/util/ActionResult;"),
			cancellable = true
	)
	public void ledgerPlayerPlaceBlockCallback(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
		if (context.getPlayer() == null) return;
		// TODO deal with dispenser placements (create separate event?)

		BlockState blockState = context.getWorld().getBlockState(context.getBlockPos());

		PlayerBlockPlaceCallback.Companion.getEVENT().invoker().place(
				context.getWorld(),
				context.getPlayer(),
				context.getBlockPos(),
				blockState,
				context,
				context.getWorld().getBlockEntity(context.getBlockPos()) != null ? context.getWorld().getBlockEntity(context.getBlockPos()) : null
		);
	}
}