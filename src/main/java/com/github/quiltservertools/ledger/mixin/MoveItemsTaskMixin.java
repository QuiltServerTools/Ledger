package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.ItemInsertCallback;
import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.brain.task.MoveItemsTask;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import java.util.WeakHashMap;

@Mixin(MoveItemsTask.class)
public class MoveItemsTaskMixin {

    @Shadow
    private MoveItemsTask.Storage targetStorage;

    @Unique
    private static final WeakHashMap<PathAwareEntity, BlockPos> ledger$entityTargetMap = new WeakHashMap<>();

    @Inject(method = "takeStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/Inventory;markDirty()V"))
    public void onTakeStack(PathAwareEntity entity, Inventory inventory, CallbackInfo ci) {
        ItemStack stack = entity.getEquippedStack(EquipmentSlot.MAINHAND);
        World world = entity.getEntityWorld();
        ItemRemoveCallback.EVENT.invoker().remove(stack, targetStorage.pos(), (ServerWorld) world, Sources.COPPER_GOLEM, entity);
    }

    @Inject(method = "placeStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/Inventory;markDirty()V"))
    private void onPlaceStack(PathAwareEntity entity, Inventory inventory, CallbackInfo ci) {
        ledger$entityTargetMap.put(entity, targetStorage.pos());
    }

    @Inject(method = "invalidateTargetStorage", at = @At("HEAD"))
    private void onInvalidateTargetStorage(PathAwareEntity entity, CallbackInfo ci) {
        ledger$entityTargetMap.remove(entity);
    }

    @ModifyArgs(method = "insertStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/Inventory;setStack(ILnet/minecraft/item/ItemStack;)V"))
    private static void onInsertStack(Args args, PathAwareEntity entity, Inventory inventory) {
        ItemStack itemStack = args.get(1);
        World world = entity.getEntityWorld();
        BlockPos targetPos = ledger$entityTargetMap.getOrDefault(entity, null);
        if (targetPos == null) return;

        ItemInsertCallback.EVENT.invoker().insert(itemStack, targetPos, (ServerWorld) world, Sources.COPPER_GOLEM, entity);
    }
}
