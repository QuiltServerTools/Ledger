package com.github.quiltservertools.ledger.mixin.blocks;

import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

@Mixin(LeveledCauldronBlock.class)
public abstract class LeveledCauldronBlockMixin {

    @Inject(method = "decrementFluidLevel",at = @At(value = "TAIL"))
    private static void ledgerLogDecrementLevelCauldron(BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        if (state.get(LEVEL) == 1) {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    state,
                    Blocks.CAULDRON.getDefaultState(),
                    null,
                    null,
                    Sources.DRAIN);
        }else {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    state,
                    state.with(LEVEL, state.get(LEVEL) - 1),
                    null,
                    null,
                    Sources.DRAIN);
        }// i guess this needs the duck thing but i need to look into that, no idea what it is
    }

    @Inject(method = "fillFromDripstone",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private void ledgerLogIncrementLevelCauldron(BlockState state, World world, BlockPos pos, Fluid fluid, CallbackInfo ci) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                state.with(LEVEL, state.get(LEVEL) + 1),
                state,
                null,
                null,
                Sources.FILL_DRIP);
    }

    @Inject(method = "precipitationTick",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private void ledgerLogIncrementLevelCauldron(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation, CallbackInfo ci) {
        BlockChangeCallback.EVENT.invoker().changeBlock(
                world,
                pos,
                state.with(LEVEL, state.get(LEVEL) + 1),
                state,
                null,
                null,
                Sources.FILL_RAIN_SNOW);
    }
}
