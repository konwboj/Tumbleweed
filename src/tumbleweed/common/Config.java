package tumbleweed.common;

import com.google.common.collect.Lists;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.logging.log4j.Level;
import tumbleweed.Tumbleweed;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

public class Config
{
	public static Configuration config;
	private static List<WeightedItem> weightedItems = Lists.newArrayList();
	private static int totalWeight;

	private static final String[] defaults = { "3 minecraft:bone", "3 minecraft:deadbush", "3 minecraft:string", "3 minecraft:feather", "3 minecraft:wheat", "3 minecraft:stick", "3 minecraft:reeds", "2 minecraft:melon_seeds", "2 minecraft:pumpkin_seeds", "2 minecraft:gold_nugget", "1 minecraft:name_tag", "1 minecraft:saddle", "1 minecraft:emerald", "1 minecraft:diamond", "1 minecraft:iron_ingot", "1 minecraft:gold_ingot" };
	private static final Pattern digitsOnly = Pattern.compile("[0-9]+");

	public static void init(File file)
	{
		if (config == null)
		{
			config = new Configuration(file);
			load();
		}
	}

	public static void load()
	{
		weightedItems.clear();

		Property itemConfig = config.get(Configuration.CATEGORY_GENERAL, "Drops", defaults);
		itemConfig.comment = "These items will drop from tumbleweed upon destroying.\nFirst number is entry weight and the string after space is item name.";
		if (itemConfig.isList())
		{
			String[] items = itemConfig.getStringList();

			for (String s : items)
			{
				if (digitsOnly.matcher(s).matches())
				{
					int id = Integer.parseInt(s);
					Tumbleweed.logger.log(Level.WARN, String.format("MagicClover: Item ids are not supported (%s).", id));
				} else
				{
					String[] itemData = s.split(" ");
					double weight = Double.parseDouble(itemData[0]);
					String id = itemData[1];

					weightedItems.add(new WeightedItem(weight, id));
					totalWeight += weight;
				}
			}
		}

		if (config.hasChanged())
			config.save();
	}

	public static ItemStack getRandomItem()
	{
		double randomWeight = totalWeight * Math.random();
		double countedWeight = 0.0;

		for (WeightedItem weightedItem : weightedItems)
		{
			countedWeight += weightedItem.getWeight();
			if (countedWeight >= randomWeight)
			{
				String[] parts = weightedItem.getId().split(":");
				Item item = (Item) GameData.getItemRegistry().getObject(parts[0] + ":" + parts[1]);
				int meta = 0;
				if (parts.length >= 3)
					meta = Integer.parseInt(parts[2]);

				return new ItemStack(item, 1, meta);
			}
		}

		return null;
	}

	private static class WeightedItem
	{
		private final double weight;
		private final String id;

		private WeightedItem(double weight, String id)
		{
			this.weight = weight;
			this.id = id;
		}

		private double getWeight()
		{
			return weight;
		}

		private String getId()
		{
			return id;
		}
	}
}