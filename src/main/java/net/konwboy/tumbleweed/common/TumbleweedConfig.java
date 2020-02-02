package net.konwboy.tumbleweed.common;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.konwboy.tumbleweed.Tumbleweed;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.config.Config;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@Config(modid = Tumbleweed.MOD_ID)
public class TumbleweedConfig {

	@Config.Name("Chance")
	@Config.Comment("Chance a tumbleweed spawns in a chunk.")
	public static double spawnChance = 0.5;

	@Config.Name("Enable Drops")
	public static boolean enableDrops = true;

	@Config.Name("Damage Crops")
	@Config.Comment("Should tumbleweeds damage crops.")
	public static boolean damageCrops = true;

	@Config.Name("Max Per Player")
	@Config.Comment("Maximum number of tumbleweeds existing per player (technically 17x17 loaded chunks).")
	public static int maxPerPlayer = 8;

	@Config.Name("Drops")
	@Config.Comment("These items will drop from a tumbleweed upon destroying.\n<weight> <mod>:<item>:[metadata] [amount]")
	public static String[] dropsData = Logic.DEFAULT_DROPS;

	@Config.Name("Spawning Blocks")
	@Config.Comment("Blocks from which tumbleweeds can spawn.\n<mod>:<block>:[metadata]")
	public static String[] spawningBlocksData = Logic.DEFAULT_BLOCKS;

	@Config.Name("Biome Whitelist")
	@Config.Comment("If not empty, tumbleweeds spawn ONLY in the specified biomes. Else they appear in all hot, dry biomes.\nExample entry: minecraft:desert")
	public static String[] biomeWhitelistData = new String[0];

	public static class Logic
	{
		private static final String[] DEFAULT_DROPS = { "3 minecraft:bone", "3 minecraft:deadbush", "3 minecraft:string", "3 minecraft:feather", "3 minecraft:wheat", "3 minecraft:stick", "3 minecraft:reeds", "2 minecraft:melon_seeds", "2 minecraft:pumpkin_seeds", "2 minecraft:gold_nugget", "1 minecraft:name_tag", "1 minecraft:saddle", "1 minecraft:emerald", "1 minecraft:diamond", "1 minecraft:iron_ingot", "1 minecraft:gold_ingot" };
		private static final String[] DEFAULT_BLOCKS = { "minecraft:deadbush" };

		private static List<WeightedItem> weightedItems = Lists.newArrayList();
		private static int totalWeight;

		public static Set<ResourceLocation> biomeWhitelist = Sets.newHashSet();
		public static Set<Metadata> spawningBlocks = Sets.newHashSet();

		public static void reload() {
			totalWeight = 0;
			weightedItems.clear();
			biomeWhitelist.clear();
			spawningBlocks.clear();

			for (String s : dropsData) {
				String[] itemData = s.split(" ");
				if (itemData.length < 2)
					continue;

				double weight = Double.parseDouble(itemData[0]);
				Metadata meta = getMetadata(itemData[1]);

				if (meta == null || !Item.REGISTRY.containsKey(meta.id)) {
					Tumbleweed.logger.warn("Item {} is invalid.", itemData[1]);
					continue;
				}

				int amount = 1;
				if (itemData.length >= 3) {
					try {
						amount = Integer.parseInt(itemData[2]);
					} catch (NumberFormatException ignore) {
					}
				}

				weightedItems.add(new WeightedItem(getMetadata(itemData[1]), amount, weight));
				totalWeight += weight;
			}

			for (String entry : biomeWhitelistData) {
				ResourceLocation id = new ResourceLocation(entry);
				if (Biome.REGISTRY.containsKey(id))
					biomeWhitelist.add(id);
				else
					Tumbleweed.logger.warn("Biome {} is invalid.", id);
			}

			for (String entry : spawningBlocksData) {
				Metadata meta = getMetadata(entry);
				if (meta == null || !Block.REGISTRY.containsKey(meta.id)) {
					Tumbleweed.logger.warn("Block {} is invalid.", entry);
					continue;
				}

				spawningBlocks.add(meta);
			}
		}

		public static ItemStack getRandomItem() {
			double randomWeight = totalWeight * Math.random();
			double countedWeight = 0.0;

			for (WeightedItem weightedItem : weightedItems) {
				countedWeight += weightedItem.weight;

				if (countedWeight < randomWeight) continue;

				Item item = Item.REGISTRY.getObject(weightedItem.meta.id);
				if (item != null)
					return new ItemStack(item, weightedItem.amount, weightedItem.meta.meta);
			}

			return null;
		}

		@Nullable
		private static Metadata getMetadata(String id) {
			String[] split = id.split(":");
			if (split.length < 2)
				return null;

			int meta = 0;
			if (split.length >= 3)
				try {
					meta = Integer.parseInt(split[2]);
				} catch (NumberFormatException ignore) {
				}

			return new Metadata(new ResourceLocation(split[0], split[1]), meta);
		}
	}

	public static class Metadata {
		public final ResourceLocation id;
		public final int meta;

		public Metadata(ResourceLocation id, int meta) {
			this.id = id;
			this.meta = meta;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Metadata metadata = (Metadata) o;
			return meta == metadata.meta && Objects.equal(id, metadata.id);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(id, meta);
		}
	}

	private static class WeightedItem {
		public final Metadata meta;
		public final int amount;
		public final double weight;

		public WeightedItem(Metadata meta, int amount, double weight) {
			this.meta = meta;
			this.amount = amount;
			this.weight = weight;
		}
	}

}
