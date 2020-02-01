package net.konwboy.tumbleweed;

import net.konwboy.tumbleweed.client.RenderTumbleweed;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.konwboy.tumbleweed.common.TumbleweedConfig;
import net.konwboy.tumbleweed.common.TumbleweedSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tumbleweed.MOD_ID, name = Tumbleweed.MOD_NAME, version = "@VERSION@", useMetadata = true, acceptedMinecraftVersions = "@MC_VERSIONS@")
public class Tumbleweed {

	public static final String MOD_ID = "tumbleweed";
	public static final String MOD_NAME = "Tumbleweed";

	public static Logger logger = LogManager.getLogger(MOD_ID);

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		if (event.getSide() == Side.CLIENT)
			RenderingRegistry.registerEntityRenderingHandler(EntityTumbleweed.class, new RenderTumbleweed.Factory());
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		EntityRegistry.registerModEntity(new ResourceLocation(MOD_ID, "tumbleweed"), EntityTumbleweed.class, "Tumbleweed", 0, this, 128, 30, true);
		MinecraftForge.EVENT_BUS.register(new TumbleweedSpawner());
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		// Needs to run after other mods register their items/blocks
		TumbleweedConfig.Logic.reload();
	}

	@SubscribeEvent
	public void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
		if (event.getModID().equals(MOD_ID)) {
			ConfigManager.sync(MOD_ID, Config.Type.INSTANCE);
			TumbleweedConfig.Logic.reload();
		}
	}
}