package net.konwboy.tumbleweed;

import net.konwboy.tumbleweed.common.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = References.MOD_ID, name = References.MOD_NAME, version = "@VERSION@", useMetadata = true, guiFactory = References.GUI_FACTORY, acceptedMinecraftVersions = "[1.9.4,1.10.2]")
public class Tumbleweed {

	@Mod.Instance(References.MOD_ID)
	public static Tumbleweed instance;

	@SidedProxy(clientSide = References.CLIENT_PROXY, serverSide = References.COMMON_PROXY)
	public static CommonProxy proxy;

	public static SimpleNetworkWrapper network;
	public static float windX = 0.07f;
	public static float windZ = -0.07f;
	public static Logger logger = LogManager.getLogger(References.MOD_ID);

	@Mod.EventHandler
	public static void preInit(FMLPreInitializationEvent event) {
		Config.init(event.getSuggestedConfigurationFile());

		network = NetworkRegistry.INSTANCE.newSimpleChannel("Tumbleweed");

		network.registerMessage(MessageWind.class, MessageWind.class, 0, Side.CLIENT);
		network.registerMessage(MessageFade.class, MessageFade.class, 1, Side.CLIENT);

		proxy.initClient();
	}

	@Mod.EventHandler
	public static void init(FMLInitializationEvent event) {
		EntityRegistry.registerModEntity(EntityTumbleweed.class, "Tumbleweed", 0, Tumbleweed.instance, 80, 4, true);
		MinecraftForge.EVENT_BUS.register(new CommonEventHandler());
	}

	@Mod.EventHandler
	public static void postInit(FMLPostInitializationEvent event) {
	}
}