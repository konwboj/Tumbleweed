package net.konwboy.tumbleweed;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.konwboy.tumbleweed.common.Spawner;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class FabricMod implements ModInitializer {

	public static final EntityType<EntityTumbleweed> TUMBLEWEED = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			Constants.TUMBLEWEED_ENTITY,
			FabricEntityTypeBuilder.create(MobCategory.MISC, EntityTumbleweed::new)
				.trackRangeBlocks(128)
				.trackedUpdateRate(30)
				.forceTrackedVelocityUpdates(true)
				.build()
	);

	@Override
	public void onInitialize() {
		FabricConfig.load();
		ServerTickEvents.END_WORLD_TICK.register(world -> Spawner.endWorldTick(TUMBLEWEED, world));
	}
}
