package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.ItemInsertCallback;
import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import java.util.WeakHashMap;

@Mixin(TransportItemsBetweenContainers.class)
public class TransportItemsBetweenContainersMixin {

    @Shadow
    private TransportItemsBetweenContainers.TransportItemTarget target;

    @Unique
    private static final WeakHashMap<PathfinderMob, BlockPos> ledger$entityTargetMap = new WeakHashMap<>();

    @Inject(method = "pickUpItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setChanged()V"))
    public void onTakeStack(PathfinderMob entity, Container inventory, CallbackInfo ci) {
        ItemStack stack = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        Level world = entity.level();
        ItemRemoveCallback.EVENT.invoker().remove(stack, target.pos(), (ServerLevel) world, Sources.COPPER_GOLEM, entity);
    }

    @Inject(method = "putDownItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setChanged()V"))
    private void onPlaceStack(PathfinderMob entity, Container inventory, CallbackInfo ci) {
        ledger$entityTargetMap.put(entity, target.pos());
    }

    @Inject(method = "stopTargetingCurrentTarget", at = @At("HEAD"))
    private void onInvalidateTargetStorage(PathfinderMob entity, CallbackInfo ci) {
        ledger$entityTargetMap.remove(entity);
    }

    @ModifyArgs(method = "addItemsToContainer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private static void onInsertStack(Args args, PathfinderMob entity, Container inventory) {
        ItemStack itemStack = args.get(1);
        Level world = entity.level();
        BlockPos targetPos = ledger$entityTargetMap.getOrDefault(entity, null);
        if (targetPos == null) return;

        ItemInsertCallback.EVENT.invoker().insert(itemStack, targetPos, (ServerLevel) world, Sources.COPPER_GOLEM, entity);
    }
}
