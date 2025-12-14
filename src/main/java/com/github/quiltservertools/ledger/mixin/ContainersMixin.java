package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.actionutils.LocationalInventory;
import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Containers.class)
public abstract class ContainersMixin {

    @ModifyArgs(
            method = "dropContents(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/Container;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;getItem(I)Lnet/minecraft/world/item/ItemStack;"))
    private static void ledgerTrackContainerBreakRemove(Args args, Level world, double x, double y, double z, Container inventory) {
        ItemStack stack = inventory.getItem(args.get(0));

        if (!stack.isEmpty() && inventory instanceof LocationalInventory locationalInventory) {
            ItemRemoveCallback.EVENT.invoker().remove(stack, locationalInventory.getLocation(), (ServerLevel) world, Sources.BROKE, null);
        }
    }
}