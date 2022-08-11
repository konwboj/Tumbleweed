package net.konwboy.tumbleweed;

import net.konwboy.tumbleweed.common.Spawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ForgeEvents {

	@SubscribeEvent
	public static void onTick(TickEvent.LevelTickEvent event) {
		if (event.phase == TickEvent.Phase.END)
			Spawner.endWorldTick(ForgeMod.TUMBLEWEED, (ServerLevel) event.level);
	}

}