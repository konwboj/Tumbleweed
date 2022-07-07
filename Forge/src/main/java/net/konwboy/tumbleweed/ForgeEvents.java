package net.konwboy.tumbleweed;

import net.konwboy.tumbleweed.common.SkeletonAi;
import net.konwboy.tumbleweed.common.Spawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ForgeEvents {

	@SubscribeEvent
	public static void onTick(TickEvent.WorldTickEvent event) {
		if (event.phase == TickEvent.Phase.END)
			Spawner.endWorldTick(ForgeMod.TUMBLEWEED, (ServerLevel) event.world);
	}

	@SubscribeEvent
	public static void onEntityJoin(EntityJoinWorldEvent event) {
		if (event.getEntity() instanceof AbstractSkeleton)
			SkeletonAi.modifySkeleton((AbstractSkeleton) event.getEntity());
	}

}