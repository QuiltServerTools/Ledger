package us.potatoboy.ledger.mixin;

import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import us.potatoboy.ledger.actionutils.DoubleInventoryHelper;

@Mixin(DoubleInventory.class)
public abstract class DoubleInventoryMixin implements DoubleInventoryHelper {
    @Shadow
    @Final
    private Inventory first;

    @Shadow
    @Final
    private Inventory second;

    @NotNull
    @Override
    public Inventory getInventory(int slot) {
        return slot >= this.first.size() ? this.second : this.first;
    }
}
