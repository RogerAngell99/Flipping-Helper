package flippinghelper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("flippinghelper")
public interface FlippingHelperConfig extends Config
{
	@ConfigItem(
		keyName = "greeting",
		name = "Welcome Message",
		description = "The message to show to the user when they login"
	)
	default String greeting()
	{
		return "Hello";
	}
}
