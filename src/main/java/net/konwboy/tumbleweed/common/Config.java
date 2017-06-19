package net.konwboy.tumbleweed.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.konwboy.tumbleweed.Tumbleweed;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.List;
import java.util.Set;

public class Config {

	public static Configuration config;

	private static List<WeightedItem> weightedItems = Lists.newArrayList();
	private static int totalWeight;
	private static double spawnChance;
	private static Set<ResourceLocation> biomeWhitelist = Sets.newHashSet();
	private static Set<ResourceLocation> spawningBlocks = Sets.newHashSet();

	private static final double DEFAULT_CHANCE = 0.4;
	private static final String[] DEFAULT_DROPS = { "3 minecraft:bone", "3 minecraft:deadbush", "3 minecraft:string", "3 minecraft:feather", "3 minecraft:wheat", "3 minecraft:stick", "3 minecraft:reeds", "2 minecraft:melon_seeds", "2 minecraft:pumpkin_seeds", "2 minecraft:gold_nugget", "1 minecraft:name_tag", "1 minecraft:saddle", "1 minecraft:emerald", "1 minecraft:diamond", "1 minecraft:iron_ingot", "1 minecraft:gold_ingot" };
	private static final String[] DEFAULT_BLOCKS = { "minecraft:deadbush" };

	public static void init(File file) {
		if (config == null) {
			config = new Configuration(file);
			load();
		}
	}

	public static void load() {
		weightedItems.clear();
		biomeWhitelist.clear();
		spawningBlocks.clear();
		totalWeight = 0;

		Property itemConfig = config.get(Configuration.CATEGORY_GENERAL, "Drops", DEFAULT_DROPS);
		itemConfig.setComment("These items will drop from tumbleweed upon destroying.\nThe first number is entry weight and the string is item name.");
		if (itemConfig.isList()) {
			for (String s : itemConfig.getStringList()) {
				String[] itemData = s.split(" ");
				if (itemData.length != 2)
					continue;

				double weight = Double.parseDouble(itemData[0]);
				weightedItems.add(new WeightedItem(weight, itemData[1]));
				totalWeight += weight;
			}
		}

		Property chanceConfig = config.get(Configuration.CATEGORY_GENERAL, "Chance", DEFAULT_CHANCE);
		chanceConfig.setComment("The chance of a tumbleweed spawning in a chunk.");
		spawnChance = chanceConfig.getDouble();

		Property biomesConfig = config.get(Configuration.CATEGORY_GENERAL, "Biome Whitelist", new String[0]);
		biomesConfig.setComment("If not empty, tumbleweeds spawn ONLY in the specified biomes. Else they appear in all hot, dry biomes.\nExample entry: minecraft:desert");
		if (biomesConfig.isList())
			for (String entry : biomesConfig.getStringList()) {
				ResourceLocation id = new ResourceLocation(entry);
				if (Biome.REGISTRY.containsKey(id))
					biomeWhitelist.add(id);
				else
					Tumbleweed.logger.log(Level.WARN, "Biome {} doesn't exist.", id);
			}

		Property blockConfig = config.get(Configuration.CATEGORY_GENERAL, "Spawning Blocks", DEFAULT_BLOCKS);
		blockConfig.setComment("The blocks in which tumbleweeds can spawn in.");
		if (blockConfig.isList())
			for (String entry : blockConfig.getStringList()) {
				ResourceLocation id = new ResourceLocation(entry);
				if (Block.REGISTRY.containsKey(id))
					spawningBlocks.add(id);
				else
					Tumbleweed.logger.log(Level.WARN, "Block {} doesn't exist.", entry);
			}

		if (config.hasChanged())
			config.save();
	}

	public static ItemStack getRandomItem() {
		double randomWeight = totalWeight * Math.random();
		double countedWeight = 0.0;

		for (WeightedItem weightedItem : weightedItems) {
			countedWeight += weightedItem.getWeight();

			if (countedWeight >= randomWeight) {
				String[] parts = weightedItem.getId().split(":");
				Item item = Item.REGISTRY.getObject(new ResourceLocation(parts[0] + ":" + parts[1]));
				int meta = 0;
				if (parts.length >= 3)
					meta = Integer.parseInt(parts[2]);

				if (item != null)
					return new ItemStack(item, 1, meta);
			}
		}

		return null;
	}

	public static double getSpawnChance() {
		return spawnChance;
	}

	public static Set<ResourceLocation> getBiomeWhitelist() {
		return biomeWhitelist;
	}

	public static Set<ResourceLocation> getSpawningBlocks() {
		return spawningBlocks;
	}

	private static class WeightedItem {

		private final double weight;
		private final String id;

		private WeightedItem(double weight, String id) {
			this.weight = weight;
			this.id = id;
		}

		private double getWeight() {
			return weight;
		}

		private String getId() {
			return id;
		}
	}
}