package net.konwboy.tumbleweed.client;

import net.konwboy.tumbleweed.Tumbleweed;
import net.konwboy.tumbleweed.common.Config;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;

public class GuiModConfig extends GuiConfig {

	public GuiModConfig(GuiScreen parentScreen) {
		super(parentScreen, new ConfigElement(Config.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(), Tumbleweed.MOD_ID, false, false, GuiConfig.getAbridgedConfigPath(Config.config.toString()));
	}
}