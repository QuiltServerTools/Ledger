package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.LedgerKt;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CauldronBlock;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

import static net.minecraft.block.LeveledCauldronBlock.LEVEL;

@Mixin(CauldronBlock.class)
public abstract class CauldronBlockMixin {

    @Inject(method = "fillFromDripstone",at = @At(value = "TAIL"))
    private void ledgerLogIncrementLevelCauldron(BlockState state, World world, BlockPos pos, Fluid fluid, CallbackInfo ci) {
        LedgerKt.logInfo("fillFromDripstone");
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                state,
                world.getBlockState(pos),
                null,
                null,
                Sources.FILL_DRIP);
    }

    @Inject(method = "precipitationTick",at = @At(value = "TAIL"))
    private void ledgerLogIncrementLevelCauldron(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation, CallbackInfo ci) {
        LedgerKt.logInfo("precipitationTick");
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                state,
                world.getBlockState(pos),
                null,
                null,
                Sources.FILL_RAIN_SNOW);
    }
}
