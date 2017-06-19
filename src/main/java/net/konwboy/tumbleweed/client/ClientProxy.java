package net.konwboy.tumbleweed.client;

import net.konwboy.tumbleweed.common.CommonProxy;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy {

	@Override
	public void initClient() {
		RenderingRegistry.registerEntityRenderingHandler(EntityTumbleweed.class, new RenderTumbleweed.Factory());
	}
}