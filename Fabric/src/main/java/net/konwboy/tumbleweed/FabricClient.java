package net.konwboy.tumbleweed;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.konwboy.tumbleweed.client.ModelTumbleweed;
import net.konwboy.tumbleweed.client.RenderTumbleweed;

@Environment(EnvType.CLIENT)
public class FabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		FabricNetwork.loadClient();
		EntityRendererRegistry.register(FabricMod.TUMBLEWEED, RenderTumbleweed::new);
		EntityModelLayerRegistry.registerModelLayer(RenderTumbleweed.MAIN_LAYER, ModelTumbleweed::createLayer);
	}
}
