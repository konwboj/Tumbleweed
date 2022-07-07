package net.konwboy.tumbleweed;

import net.fabricmc.loader.api.FabricLoader;
import net.konwboy.tumbleweed.services.IConfig;
import org.apache.commons.lang3.tuple.Triple;

import java.io.StringReader;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

public class FabricConfig implements IConfig {

	private static boolean enableDrops = true;
	private static double spawnChance = 0.5;
	private static int maxPerPlayer = 8;
	private static boolean damageCrops = true;
	private static boolean dropOnlyByPlayer = false;

	public static void load() {
		try {
			var configFile = FabricLoader.getInstance().getConfigDir().resolve("tumbleweed.properties").toFile();
			configFile.getParentFile().mkdirs();

			if (configFile.exists()){
				var props = new Properties();
				props.load(new StringReader(Files.readString(configFile.toPath())));

				enableDrops = Boolean.parseBoolean((String)props.getOrDefault("enableDrops", enableDrops));
				spawnChance = Double.parseDouble((String)props.getOrDefault("spawnChance", spawnChance));
				maxPerPlayer = Integer.parseInt((String)props.getOrDefault("maxPerPlayer", maxPerPlayer));
				damageCrops = Boolean.parseBoolean((String)props.getOrDefault("damageCrops", damageCrops));
				dropOnlyByPlayer = Boolean.parseBoolean((String)props.getOrDefault("dropOnlyByPlayer", dropOnlyByPlayer));
			} else {
				StringBuilder contents = new StringBuilder();
				var toWrite = List.of(
						Triple.of(Constants.ENABLE_DROPS_DESC, "enableDrops", enableDrops),
						Triple.of(Constants.SPAWN_CHANCE_DESC, "spawnChance", spawnChance),
						Triple.of(Constants.MAX_PER_PLAYER_DESC, "maxPerPlayer", maxPerPlayer),
						Triple.of(Constants.DAMAGE_CROPS_DESC, "damageCrops", damageCrops),
						Triple.of(Constants.DROP_ONLY_BY_PLAYER_DESC, "dropOnlyByPlayer", dropOnlyByPlayer)
				);

				for (var t : toWrite) {
					for (var d : t.getLeft())
						contents.append("#").append(d).append('\n');
					contents.append(t.getMiddle()).append("=").append(t.getRight()).append('\n');
					contents.append('\n');
				}

				Files.writeString(configFile.toPath(), contents.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean enableDrops() {
		return enableDrops;
	}

	@Override
	public double spawnChance() {
		return spawnChance;
	}

	@Override
	public int maxPerPlayer() {
		return maxPerPlayer;
	}

	@Override
	public boolean damageCrops() {
		return damageCrops;
	}

	@Override
	public boolean dropOnlyByPlayer() {
		return dropOnlyByPlayer;
	}
}
