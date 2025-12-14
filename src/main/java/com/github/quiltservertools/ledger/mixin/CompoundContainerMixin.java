package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.actionutils.DoubleInventoryHelper;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CompoundContainer.class)
public abstract class CompoundContainerMixin implements DoubleInventoryHelper {
    @Shadow
    @Final
    private Container container1;

    @Shadow
    @Final
    private Container container2;

    @NotNull
    @Override
    public Container getInventory(int slot) {
        return slot >= this.container1.getContainerSize() ? this.container2 : this.container1;
    }
}
