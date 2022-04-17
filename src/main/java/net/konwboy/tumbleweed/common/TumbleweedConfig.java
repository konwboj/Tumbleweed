package net.konwboy.tumbleweed.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.konwboy.tumbleweed.Tumbleweed;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
public class TumbleweedConfig {

	public static double spawnChance;
	public static int maxPerPlayer;
	public static boolean enableDrops;
	public static boolean damageCrops;
	public static boolean dropOnlyByPlayer;

	private static final ForgeConfigSpec.ConfigValue<Double> SPAWN_CHANCE;
	private static final ForgeConfigSpec.ConfigValue<Integer> MAX_PER_PLAYER;
	private static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_DROPS;
	private static final ForgeConfigSpec.ConfigValue<Boolean> DAMAGE_CROPS;
	private static final ForgeConfigSpec.ConfigValue<Boolean> DROP_ONLY_BY_PLAYER;
	private static final ForgeConfigSpec.ConfigValue<List<String>> DROP_DATA;
	private static final ForgeConfigSpec.ConfigValue<List<String>> BLOCK_DATA;
	private static final ForgeConfigSpec.ConfigValue<List<String>> BIOME_DATA;

	public static final ForgeConfigSpec SPEC;

	private static final String[] DEFAULT_DROPS = { "3 minecraft:bone", "3 minecraft:dead_bush", "3 minecraft:string", "3 minecraft:feather", "3 minecraft:wheat", "3 minecraft:stick", "3 minecraft:sugar_cane", "2 minecraft:melon_seeds", "2 minecraft:pumpkin_seeds", "2 minecraft:gold_nugget", "1 minecraft:name_tag", "1 minecraft:saddle", "1 minecraft:emerald", "1 minecraft:diamond", "1 minecraft:iron_ingot", "1 minecraft:gold_ingot" };
	private static final String[] DEFAULT_BLOCKS = { "minecraft:dead_bush" };

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		SPAWN_CHANCE = builder
				.comment("Chance a tumbleweed spawns in a chunk.")
				.define("spawnChance", 0.5);

		MAX_PER_PLAYER = builder
				.comment("Maximum number of tumbleweeds existing per player (technically 17x17 loaded chunks).")
				.define("maxPerPlayer", 8);

		ENABLE_DROPS = builder
				.define("enableDrops", true);

		DAMAGE_CROPS = builder
				.comment("Should tumbleweeds destroy crops they touch.")
				.define("damageCrops", true);

		DROP_ONLY_BY_PLAYER = builder
				.comment("Drop items only when destroyed by player (normally also drops on lava and cactus damage, for example).")
				.define("dropOnlyByPlayer", false);

		DROP_DATA = builder
				.comment(
						"These items will drop from a tumbleweed upon destroying.",
						"The item format allows specifying item tags and NBT data. It follows vanilla's command argument format.",
						"The amount parameter is optional.",
						"weight item amount",
						"Example entry: 2 minecraft:arrow{display:{Name:'{\"text\":\"Cool arrow\"}'}} 20")
				.define("drops", Lists.newArrayList(DEFAULT_DROPS));

		BLOCK_DATA = builder
				.comment(
						"Blocks in which tumbleweeds can spawn. Only works with non-solid blocks.",
						"The actual location also requires sky access and a solid block below it.",
						"<mod>:<block>")
				.define("spawningBlocks", Lists.newArrayList(DEFAULT_BLOCKS));

		BIOME_DATA = builder
				.comment(
						"If not empty, tumbleweeds spawn ONLY in the specified biomes. Else they appear in all hot, dry biomes.",
						"Example entry: minecraft:desert")
				.define("biomeWhitelist", Lists.newArrayList());

		SPEC = builder.build();
	}

	public static Set<ResourceLocation> biomeWhitelist = Sets.newHashSet();
	public static Set<ResourceLocation> spawningBlocks = Sets.newHashSet();

	public static boolean canLoad = false;

	public static void load() {
		if (!canLoad)
			return;

		biomeWhitelist.clear();
		spawningBlocks.clear();

		spawnChance = SPAWN_CHANCE.get();
		maxPerPlayer = MAX_PER_PLAYER.get();
		enableDrops = ENABLE_DROPS.get();
		damageCrops = DAMAGE_CROPS.get();
		dropOnlyByPlayer = DROP_ONLY_BY_PLAYER.get();

		DropList.load(DROP_DATA.get());

		for (String entry : BIOME_DATA.get()) {
			ResourceLocation id = new ResourceLocation(entry);
			if (ForgeRegistries.BIOMES.containsKey(id))
				biomeWhitelist.add(id);
			else
				Tumbleweed.logger.warn("Biome {} is invalid.", id);
		}

		for (String entry : BLOCK_DATA.get()) {
			ResourceLocation location = new ResourceLocation(entry);
			if (ForgeRegistries.BLOCKS.containsKey(location))
				spawningBlocks.add(location);
			else
				Tumbleweed.logger.warn("Block {} is invalid.", entry);
		}
	}

	@SubscribeEvent
	public static void configReloading(ModConfigEvent.Reloading event) {
		if (event.getConfig().getSpec() == SPEC) {
			TumbleweedConfig.load();
		}
	}

}
