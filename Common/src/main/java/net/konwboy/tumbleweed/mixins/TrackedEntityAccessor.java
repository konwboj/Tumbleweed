package net.konwboy.tumbleweed.mixins;

import net.minecraft.server.level.ServerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net/minecraft/server/level/ChunkMap$TrackedEntity")
public interface TrackedEntityAccessor {

	@Accessor
	ServerEntity getServerEntity();
}
