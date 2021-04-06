package us.potatoboy.ledger.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import us.potatoboy.ledger.actionutils.LocationalInventory;

@Mixin(LockableContainerBlockEntity.class)
public abstract class LockableContainerBlockEntityMixin extends BlockEntity implements LocationalInventory {
	public LockableContainerBlockEntityMixin(BlockEntityType<?> type) {
		super(type);
	}

	@NotNull
	@Override
	public BlockPos getLocation() {
		return this.pos;
	}
}
