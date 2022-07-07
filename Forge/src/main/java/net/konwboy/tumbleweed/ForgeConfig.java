package net.konwboy.tumbleweed;

import net.konwboy.tumbleweed.services.IConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public class ForgeConfig implements IConfig {

	public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_DROPS;
	public static final ForgeConfigSpec.ConfigValue<Double> SPAWN_CHANCE;
	public static final ForgeConfigSpec.ConfigValue<Integer> MAX_PER_PLAYER;
	public static final ForgeConfigSpec.ConfigValue<Boolean> DAMAGE_CROPS;
	public static final ForgeConfigSpec.ConfigValue<Boolean> DROP_ONLY_BY_PLAYER;

	public static final ForgeConfigSpec SPEC;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		ENABLE_DROPS = builder.comment(Constants.ENABLE_DROPS_DESC).define("enableDrops", true);
		SPAWN_CHANCE = builder.comment(Constants.SPAWN_CHANCE_DESC).define("spawnChance", 0.5);
		MAX_PER_PLAYER = builder.comment(Constants.MAX_PER_PLAYER_DESC).define("maxPerPlayer", 8);
		DAMAGE_CROPS = builder.comment(Constants.DAMAGE_CROPS_DESC).define("damageCrops", true);
		DROP_ONLY_BY_PLAYER = builder.comment(Constants.DROP_ONLY_BY_PLAYER_DESC).define("dropOnlyByPlayer", false);

		SPEC = builder.build();
	}

	@Override
	public boolean enableDrops() {
		return ENABLE_DROPS.get();
	}

	@Override
	public double spawnChance() {
		return SPAWN_CHANCE.get();
	}

	@Override
	public int maxPerPlayer() {
		return MAX_PER_PLAYER.get();
	}

	@Override
	public boolean damageCrops() {
		return DAMAGE_CROPS.get();
	}

	@Override
	public boolean dropOnlyByPlayer() {
		return DROP_ONLY_BY_PLAYER.get();
	}
}
