package com.github.quiltservertools.ledger.mixin.preview;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityTrackerEntry.class)
public interface EntityTrackerEntryAccessor {

    @Accessor
    Entity getEntity();

}
