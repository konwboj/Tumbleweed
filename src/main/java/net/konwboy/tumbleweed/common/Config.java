package net.konwboy.tumbleweed.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Data;
import net.konwboy.tumbleweed.Tumbleweed;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.logging.log4j.Level;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Set;

public class Config {

	public static Configuration config;

	private static List<WeightedItem> weightedItems = Lists.newArrayList();
	private static int totalWeight;
	private static double spawnChance;
	private static Set<ResourceLocation> biomeWhitelist = Sets.newHashSet();
	private static Set<Metadata> spawningBlocks = Sets.newHashSet();

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
				Metadata meta = getMetadata(itemData[1]);

				if (meta == null || !Item.REGISTRY.containsKey(meta.id)) {
					Tumbleweed.logger.log(Level.WARN, "Item {} is invalid.", itemData[1]);
					continue;
				}

				weightedItems.add(new WeightedItem(getMetadata(itemData[1]), weight));
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
				Metadata meta = getMetadata(entry);
				if (meta == null || !Block.REGISTRY.containsKey(meta.id)) {
					Tumbleweed.logger.log(Level.WARN, "Block {} is invalid.", entry);
					continue;
				}
				spawningBlocks.add(meta);
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
				Item item = Item.REGISTRY.getObject(weightedItem.meta.id);
				if (item != null)
					return new ItemStack(item, 1, weightedItem.meta.meta);
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

	public static Set<Metadata> getSpawningBlocks() {
		return spawningBlocks;
	}

	@Nullable
	private static Metadata getMetadata(String id) {
		String[] split = id.split(":");
		if (split.length < 2)
			return null;

		int meta = 0;
		if (split.length >= 3)
			meta = Integer.parseInt(split[2]);

		return new Metadata(new ResourceLocation(split[0], split[1]), meta);
	}

	@Data
	public static class Metadata {

		private final ResourceLocation id;
		private final int meta;
	}

	@Data
	private static class WeightedItem {

		private final Metadata meta;
		private final double weight;
	}

}