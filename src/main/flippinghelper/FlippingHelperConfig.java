package flippinghelper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("flippinghelper")
public interface FlippingHelperConfig extends Config
{
	@ConfigSection(
		name = "Filters",
		description = "Filter flipping opportunities",
		position = 0
	)
	String filterSection = "filters";

	@ConfigItem(
		keyName = "minBuyPrice",
		name = "Minimum Buy Price",
		description = "Minimum adjusted low price (0 = no minimum)",
		section = filterSection,
		position = 1
	)
	@Range(min = 0)
	default int minBuyPrice()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "maxBuyPrice",
		name = "Maximum Buy Price",
		description = "Maximum adjusted low price (0 = no maximum)",
		section = filterSection,
		position = 2
	)
	@Range(min = 0)
	default int maxBuyPrice()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "minProfit",
		name = "Minimum Profit",
		description = "Minimum profit per item (0 = no minimum)",
		section = filterSection,
		position = 3
	)
	@Range(min = 0)
	default int minProfit()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "minDailyVolume",
		name = "Minimum Daily Volume",
		description = "Minimum daily trading volume (0 = no minimum)",
		section = filterSection,
		position = 4
	)
	@Range(min = 0)
	default int minDailyVolume()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "minScore",
		name = "Minimum Score",
		description = "Minimum flipping score (0 = no minimum)",
		section = filterSection,
		position = 5
	)
	@Range(min = 0)
	default double minScore()
	{
		return 0.0;
	}

	@ConfigItem(
		keyName = "dumpFilter",
		name = "Dump Filter",
		description = "Filter items based on dump signal detection",
		section = filterSection,
		position = 6
	)
	default DumpFilter dumpFilter()
	{
		return DumpFilter.ALL;
	}

	@ConfigItem(
		keyName = "minQuantity",
		name = "Minimum Quantity",
		description = "Minimum quantity to trade (0 = no minimum)",
		section = filterSection,
		position = 7
	)
	@Range(min = 0)
	default int minQuantity()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "maxTotalInvestment",
		name = "Maximum Total Investment",
		description = "Maximum total GP to invest (quantity × buy price, 0 = no maximum)",
		section = filterSection,
		position = 8
	)
	@Range(min = 0)
	default int maxTotalInvestment()
	{
		return 0;
	}

	enum DumpFilter
	{
		ALL("Show All"),
		DUMP_ONLY("Only Dump Items"),
		NO_DUMP("Exclude Dump Items");

		private final String name;

		DumpFilter(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	@ConfigItem(
		keyName = "greeting",
		name = "Welcome Message",
		description = "The message to show to the user when they login",
		position = 10
	)
	default String greeting()
	{
		return "Hello";
	}
}
