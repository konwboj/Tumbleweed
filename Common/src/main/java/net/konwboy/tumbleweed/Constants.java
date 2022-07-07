package net.konwboy.tumbleweed;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {

	public static final Logger LOG = LoggerFactory.getLogger("Tumbleweed");
	public static final ResourceLocation TUMBLEWEED_ENTITY = new ResourceLocation("tumbleweed", "tumbleweed");
	public static final ResourceLocation TUMBLEWEED_CHANNEL = new ResourceLocation("tumbleweed", "net");

	// Config key descriptions

	public static final String[] ENABLE_DROPS_DESC = {
			"Should tumbleweeds drop items upon destroying.",
			"The drop list is configurable using loot tables (using vanilla data packs or a mod like CraftTweaker)."
	};

	public static final String[] SPAWN_CHANCE_DESC = {
			"Chance a tumbleweed spawns in a chunk.",
			"Spawner blocks and allowed biomes are customizable using data packs through block and biome tags.",
			"Tumbleweeds can spawn in non-solid blocks with a solid block below and sky access above."
	};

	public static final String[] MAX_PER_PLAYER_DESC = {
			"Maximum number of tumbleweeds existing per player (technically per 17x17 loaded chunks)."
	};

	public static final String[] DAMAGE_CROPS_DESC = {
			"Should tumbleweeds destroy crops they land upon."
	};

	public static final String[] DROP_ONLY_BY_PLAYER_DESC = {
			"Drop items only when destroyed by player (normally also drops on lava and cactus damage, for example)."
	};

}
