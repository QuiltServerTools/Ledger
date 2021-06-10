package us.potatoboy.ledger.mixin.callbacks;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Iterator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import us.potatoboy.ledger.callbacks.BlockExplodeCallback;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow
    @Final
    private World world;

    @Shadow
    @Final
    private Entity entity;

    @Inject(
            method = "affectWorld",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void ledgerBlockExplodeCallback(
            boolean bl,
            CallbackInfo ci,
            boolean bl2,
            ObjectArrayList<Pair<ItemStack, BlockPos>> objectArrayList,
            Iterator<BlockPos> blocks,
            BlockPos blockPos,
            BlockState blockState,
            Block block) {

        if (blockState.isAir()) return;

        BlockExplodeCallback.Companion.getEVENT().invoker().explode(
                world,
                entity,
                blockPos,
                blockState,
                world.getBlockEntity(blockPos) != null ? world.getBlockEntity(blockPos) : null
        );
    }
}
