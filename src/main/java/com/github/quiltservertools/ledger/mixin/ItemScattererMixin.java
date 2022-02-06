package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.actionutils.LocationalInventory;
import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ItemScatterer.class)
public abstract class ItemScattererMixin {

    @ModifyArgs(
            method = "spawn(Lnet/minecraft/world/World;DDDLnet/minecraft/inventory/Inventory;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/Inventory;getStack(I)Lnet/minecraft/item/ItemStack;"))
    private static void ledgerTrackContainerBreakRemove(Args args, World world, double x, double y, double z, Inventory inventory) {
        ItemStack stack = inventory.getStack(args.get(0));

        if (!stack.isEmpty() && inventory instanceof LocationalInventory locationalInventory) {
            ItemRemoveCallback.EVENT.invoker().remove(stack, locationalInventory.getLocation(), (ServerWorld) world, Sources.BROKE, null);
        }
    }
}