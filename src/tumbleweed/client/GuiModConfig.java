package tumbleweed.client;

import cpw.mods.fml.client.config.GuiConfig;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import tumbleweed.common.Config;
import tumbleweed.common.References;

public class GuiModConfig extends GuiConfig
{
	public GuiModConfig(GuiScreen parentScreen)
	{
		super(parentScreen, new ConfigElement(Config.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(), References.MOD_ID, false, false, GuiConfig.getAbridgedConfigPath(Config.config.toString()));
	}
}