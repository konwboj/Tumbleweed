package net.konwboy.tumbleweed;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.konwboy.tumbleweed.common.SkeletonAi;
import net.konwboy.tumbleweed.common.Spawner;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.AbstractSkeleton;

public class FabricMod implements ModInitializer {

	public static final EntityType<EntityTumbleweed> TUMBLEWEED = Registry.register(
			Registry.ENTITY_TYPE,
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

		ServerEntityEvents.ENTITY_LOAD.register((e, w) -> {
			if (e instanceof AbstractSkeleton)
				SkeletonAi.modifySkeleton((AbstractSkeleton) e);
		});
	}
}
