package net.konwboy.tumbleweed;

import net.konwboy.tumbleweed.client.RenderTumbleweed;
import net.konwboy.tumbleweed.common.Config;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.konwboy.tumbleweed.common.TumbleweedSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
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

@Mod(modid = Tumbleweed.MOD_ID, name = Tumbleweed.MOD_NAME, version = "@VERSION@", useMetadata = true, guiFactory = Tumbleweed.GUI_FACTORY, acceptedMinecraftVersions = "@MC_VERSIONS@")
public class Tumbleweed {

	public static final String MOD_ID = "tumbleweed";
	public static final String MOD_NAME = "Tumbleweed";
	public static final String GUI_FACTORY = "net.konwboy.tumbleweed.client.GuiFactory";

	@Mod.Instance(MOD_ID)
	public static Tumbleweed instance;

	public static Logger logger = LogManager.getLogger(MOD_ID);

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Config.init(event.getSuggestedConfigurationFile());

		if (event.getSide() == Side.CLIENT)
			RenderingRegistry.registerEntityRenderingHandler(EntityTumbleweed.class, new RenderTumbleweed.Factory());
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		EntityRegistry.registerModEntity(new ResourceLocation(MOD_ID, "tumbleweed"), EntityTumbleweed.class, "Tumbleweed", 0, Tumbleweed.instance, 80, 10, true);
		MinecraftForge.EVENT_BUS.register(new TumbleweedSpawner());
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		Config.load();
	}

	@SubscribeEvent
	public void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
		if (event.getModID().equals(MOD_ID))
			Config.load();
	}
}