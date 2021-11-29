package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.actionutils.LocationalInventory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LockableContainerBlockEntity.class)
public abstract class LockableContainerBlockEntityMixin extends BlockEntity implements LocationalInventory {
    public LockableContainerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @NotNull
    @Override
    public BlockPos getLocation() {
        return this.pos;
    }
}
