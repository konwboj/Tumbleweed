package net.konwboy.tumbleweed.common;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
public class TumbleweedConfig {

	public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_DROPS;
	public static final ForgeConfigSpec.ConfigValue<Double> SPAWN_CHANCE;
	public static final ForgeConfigSpec.ConfigValue<Integer> MAX_PER_PLAYER;
	public static final ForgeConfigSpec.ConfigValue<Boolean> DAMAGE_CROPS;
	public static final ForgeConfigSpec.ConfigValue<Boolean> DROP_ONLY_BY_PLAYER;

	public static final ForgeConfigSpec SPEC;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		ENABLE_DROPS = builder
				.comment("Should tumbleweeds drop items upon destroying.",
						"The drop list is configurable using loot tables (using vanilla data packs or a mod like CraftTweaker).")
				.define("enableDrops", true);

		SPAWN_CHANCE = builder
				.comment("Chance a tumbleweed spawns in a chunk.",
						"Spawner blocks and allowed biomes are customizable using data packs through block and biome tags.",
						"Tumbleweeds can spawn in non-solid blocks with a solid block below and sky access above.")
				.define("spawnChance", 0.5);

		MAX_PER_PLAYER = builder
				.comment("Maximum number of tumbleweeds existing per player (technically per 17x17 loaded chunks).")
				.define("maxPerPlayer", 8);

		DAMAGE_CROPS = builder
				.comment("Should tumbleweeds destroy crops they land upon.")
				.define("damageCrops", true);

		DROP_ONLY_BY_PLAYER = builder
				.comment("Drop items only when destroyed by player (normally also drops on lava and cactus damage, for example).")
				.define("dropOnlyByPlayer", false);

		SPEC = builder.build();
	}

}
