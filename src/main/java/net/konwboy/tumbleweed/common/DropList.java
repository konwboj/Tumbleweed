package net.konwboy.tumbleweed.common;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.konwboy.tumbleweed.Tumbleweed;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class DropList {

	private static List<WeightedItem> weightedItems = Lists.newArrayList();
	private static int totalWeight;

	public static void load(List<String> drops){
		totalWeight = 0;
		weightedItems.clear();

		for (String s : drops) {
			String[] itemData = s.split(" ");
			if (itemData.length < 2)
				continue;

			double weight = Double.parseDouble(itemData[0]);
			String item = itemData[1];

			try {
				new ItemParser(new StringReader(item), true).parse();
			} catch (CommandSyntaxException e) {
				Tumbleweed.logger.warn("Error parsing drop list entry {}: {}", item, e.getMessage());
				continue;
			}

			int amount = 1;

			if (itemData.length >= 3) {
				try {
					amount = Integer.parseInt(itemData[2]);
				} catch (NumberFormatException ignore) {
				}
			}

			weightedItems.add(new WeightedItem(item, amount, weight));
			totalWeight += weight;
		}
	}

	public static ItemStack getRandomItem(Level level) {
		double randomWeight = totalWeight * Math.random();
		double countedWeight = 0.0;

		for (WeightedItem weightedItem : weightedItems) {
			countedWeight += weightedItem.weight;

			if (countedWeight < randomWeight) continue;

			try {
				return weightedItem.getStack(level);
			} catch (CommandSyntaxException e) {
				Tumbleweed.logger.warn("Error parsing drop list entry {}: {}", weightedItem.item, e.getMessage());
			}

			return null;
		}

		return null;
	}

	private record WeightedItem(String item, int amount, double weight) {

		public ItemStack getStack(Level level) throws CommandSyntaxException {
			ItemParser parser = new ItemParser(new StringReader(item), true).parse();
			var item = parser.getItem();

			//noinspection ConstantConditions
			if (item == null) { // The parsed string is an item tag
				var tagOpt = Registry.ITEM.getTag(parser.getTag());

				if (tagOpt.isEmpty()) {
					Tumbleweed.logger.warn("Couldn't find item tag {}", parser.getTag());
					return null;
				}

				var tag = tagOpt.get();

				item = tag.getRandomElement(level.random).map(Holder::value).orElse(null);
				if (item == null)
					return null;
			}

			ItemStack stack = new ItemStack(item, amount);
			if (parser.getNbt() != null)
				stack.setTag(parser.getNbt());
			return stack;
		}
	}

}
