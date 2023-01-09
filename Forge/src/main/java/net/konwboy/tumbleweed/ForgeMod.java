package net.konwboy.tumbleweed;

import net.konwboy.tumbleweed.client.ModelTumbleweed;
import net.konwboy.tumbleweed.client.RenderTumbleweed;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ObjectHolder;
import net.minecraftforge.registries.RegisterEvent;

@Mod("tumbleweed")
@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
public class ForgeMod {

	@ObjectHolder(registryName = "minecraft:entity_type", value = "tumbleweed:tumbleweed")
	public static EntityType<EntityTumbleweed> TUMBLEWEED;

	public ForgeMod() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgeConfig.SPEC);
		ForgeNetwork.load();
	}

	@SubscribeEvent
	public static void layerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(RenderTumbleweed.MAIN_LAYER, ModelTumbleweed::createLayer);
	}

	@SubscribeEvent
	public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(TUMBLEWEED, RenderTumbleweed::new);
	}

	@SubscribeEvent
	public static void registerEntities(RegisterEvent event) {
		event.register(
			Registries.ENTITY_TYPE,
			Constants.TUMBLEWEED_ENTITY,
			() -> EntityType.Builder
					.of(EntityTumbleweed::new, MobCategory.MISC)
					.clientTrackingRange(10)
					.setUpdateInterval(30)
					.setShouldReceiveVelocityUpdates(true)
					.build("tumbleweed")
		);
	}
}