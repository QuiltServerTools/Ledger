package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.callbacks.ItemRenameCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Objects;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    private AnvilMenuMixin(MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition itemInputSlots) {
        super(menuType, containerId, inventory, access, itemInputSlots);
    }

    // Runs before the anvil consumes its inputs, so the first input slot still holds the item as it
    // was before the rename. Taking a result that was only repaired or enchanted keeps the old name
    // and is not logged.
    @Inject(method = "onTake", at = @At("HEAD"))
    private void ledgerLogItemRename(Player player, ItemStack result, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        ItemStack original = this.inputSlots.getItem(AnvilMenu.INPUT_SLOT);
        if (Objects.equals(original.get(DataComponents.CUSTOM_NAME), result.get(DataComponents.CUSTOM_NAME))) {
            return;
        }

        BlockPos pos = this.access.evaluate((world, blockPos) -> blockPos, player.blockPosition());
        ItemRenameCallback.EVENT.invoker().rename(player, pos, original.copy(), result.copy());
    }

    @Inject(method = "lambda$onTake$0",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private static void ledgerLogAnvilBreak(Player player, Level world, BlockPos pos, CallbackInfo ci) {
        BlockBreakCallback.EVENT.invoker().breakBlock(
                world,
                pos,
                world.getBlockState(pos),
                null,
                Sources.DECAY,
                player);
    }

    @ModifyArgs(method = "lambda$onTake$0",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static void ledgerLogAnvilChange(Args args, Player player, Level world, BlockPos pos) {
        BlockState newBlockState = args.get(1);
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                world.getBlockState(pos),
                newBlockState,
                world.getBlockEntity(pos),
                null,
                Sources.DECAY,
                player);
    }
}
