package flippinghelper;

import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.ClientToolbar;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FlippingHelperFilterTest {

    @Mock
    private FlippingHelperConfig config;

    @Mock
    private ClientToolbar clientToolbar;

    @Mock
    private ItemManager itemManager;

    @Mock
    private Client client;

    @Mock
    private MouseManager mouseManager;

    @Mock
    private ClientThread clientThread;

    @Mock
    private HighlightManager highlightManager;

    @Mock
    private GeMenuHandler menuHandler;

    @Mock
    private GeSearchAutoFillHandler searchAutoFillHandler;

    @Mock
    private GeInteractionHandler interactionHandler;

    private FlippingHelperPlugin plugin;

    @Before
    public void setUp() {
        plugin = new FlippingHelperPlugin();
        // Use reflection to inject mocked dependencies
        injectField(plugin, "config", config);
        injectField(plugin, "clientToolbar", clientToolbar);
        injectField(plugin, "itemManager", itemManager);
        injectField(plugin, "client", client);
        injectField(plugin, "mouseManager", mouseManager);
        injectField(plugin, "clientThread", clientThread);
        injectField(plugin, "highlightManager", highlightManager);
        injectField(plugin, "menuHandler", menuHandler);
        injectField(plugin, "searchAutoFillHandler", searchAutoFillHandler);
        injectField(plugin, "interactionHandler", interactionHandler);

        // Setup default config values (no filtering)
        when(config.minBuyPrice()).thenReturn(0);
        when(config.maxBuyPrice()).thenReturn(0);
        when(config.minProfit()).thenReturn(0);
        when(config.minDailyVolume()).thenReturn(0);
        when(config.minScore()).thenReturn(0.0);
        when(config.dumpFilter()).thenReturn(FlippingHelperConfig.DumpFilter.ALL);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject field: " + fieldName, e);
        }
    }

    private FlippingItem createTestItem(String id, String name, long buyPrice, long profit,
                                       long volume, double score, Double dumpScore) {
        FlippingItem item = new FlippingItem();
        item.setId(id);
        item.setName(name);
        item.setAdjustedLowPrice(buyPrice);
        item.setAdjustedHighPrice(buyPrice + profit);
        item.setProfit(profit);
        item.setDailyVolume(volume);
        item.setScore(score);
        item.setDumpSignalScore(dumpScore);
        item.setQuantity(1);
        item.setMembers(true);
        return item;
    }

    @Test
    public void testNoFilters_AllItemsPass() {
        FlippingItem item1 = createTestItem("1", "Item 1", 1000, 100, 5000, 2.5, null);
        FlippingItem item2 = createTestItem("2", "Item 2", 500, 50, 2000, 1.5, 0.0);
        FlippingItem item3 = createTestItem("3", "Item 3", 10000, 1000, 10000, 3.0, 1.0);

        List<FlippingItem> items = Arrays.asList(item1, item2, item3);
        List<FlippingItem> filtered = plugin.filterItems(items);

        assertEquals("All items should pass with no filters", 3, filtered.size());
        assertTrue(filtered.contains(item1));
        assertTrue(filtered.contains(item2));
        assertTrue(filtered.contains(item3));
    }

    @Test
    public void testMinBuyPriceFilter() {
        when(config.minBuyPrice()).thenReturn(1000);

        FlippingItem item1 = createTestItem("1", "Cheap Item", 500, 100, 5000, 2.5, null);
        FlippingItem item2 = createTestItem("2", "Exact Price", 1000, 100, 5000, 2.5, null);
        FlippingItem item3 = createTestItem("3", "Expensive Item", 2000, 100, 5000, 2.5, null);

        assertTrue("Item with exact min price should pass", plugin.passesFilters(item2));
        assertTrue("Item above min price should pass", plugin.passesFilters(item3));
        assertFalse("Item below min price should fail", plugin.passesFilters(item1));

        List<FlippingItem> filtered = plugin.filterItems(Arrays.asList(item1, item2, item3));
        assertEquals("Should filter out items below min price", 2, filtered.size());
        assertFalse(filtered.contains(item1));
    }

    @Test
    public void testMaxBuyPriceFilter() {
        when(config.maxBuyPrice()).thenReturn(1000);

        FlippingItem item1 = createTestItem("1", "Cheap Item", 500, 100, 5000, 2.5, null);
        FlippingItem item2 = createTestItem("2", "Exact Price", 1000, 100, 5000, 2.5, null);
        FlippingItem item3 = createTestItem("3", "Expensive Item", 2000, 100, 5000, 2.5, null);

        assertTrue("Item below max price should pass", plugin.passesFilters(item1));
        assertTrue("Item with exact max price should pass", plugin.passesFilters(item2));
        assertFalse("Item above max price should fail", plugin.passesFilters(item3));

        List<FlippingItem> filtered = plugin.filterItems(Arrays.asList(item1, item2, item3));
        assertEquals("Should filter out items above max price", 2, filtered.size());
        assertFalse(filtered.contains(item3));
    }

    @Test
    public void testMinMaxBuyPriceFilter_Combined() {
        when(config.minBuyPrice()).thenReturn(1000);
        when(config.maxBuyPrice()).thenReturn(5000);

        FlippingItem item1 = createTestItem("1", "Too Cheap", 500, 100, 5000, 2.5, null);
        FlippingItem item2 = createTestItem("2", "Min Price", 1000, 100, 5000, 2.5, null);
        FlippingItem item3 = createTestItem("3", "Mid Range", 3000, 100, 5000, 2.5, null);
        FlippingItem item4 = createTestItem("4", "Max Price", 5000, 100, 5000, 2.5, null);
        FlippingItem item5 = createTestItem("5", "Too Expensive", 6000, 100, 5000, 2.5, null);

        assertFalse("Item below min should fail", plugin.passesFilters(item1));
        assertTrue("Item at min should pass", plugin.passesFilters(item2));
        assertTrue("Item in range should pass", plugin.passesFilters(item3));
        assertTrue("Item at max should pass", plugin.passesFilters(item4));
        assertFalse("Item above max should fail", plugin.passesFilters(item5));

        List<FlippingItem> filtered = plugin.filterItems(Arrays.asList(item1, item2, item3, item4, item5));
        assertEquals("Should only keep items in price range", 3, filtered.size());
    }

    @Test
    public void testMinProfitFilter() {
        when(config.minProfit()).thenReturn(100);

        FlippingItem item1 = createTestItem("1", "Low Profit", 1000, 50, 5000, 2.5, null);
        FlippingItem item2 = createTestItem("2", "Exact Profit", 1000, 100, 5000, 2.5, null);
        FlippingItem item3 = createTestItem("3", "High Profit", 1000, 500, 5000, 2.5, null);

        assertFalse("Item below min profit should fail", plugin.passesFilters(item1));
        assertTrue("Item with exact min profit should pass", plugin.passesFilters(item2));
        assertTrue("Item above min profit should pass", plugin.passesFilters(item3));

        List<FlippingItem> filtered = plugin.filterItems(Arrays.asList(item1, item2, item3));
        assertEquals("Should filter out low profit items", 2, filtered.size());
        assertFalse(filtered.contains(item1));
    }

    @Test
    public void testMinDailyVolumeFilter() {
        when(config.minDailyVolume()).thenReturn(5000);

        FlippingItem item1 = createTestItem("1", "Low Volume", 1000, 100, 1000, 2.5, null);
        FlippingItem item2 = createTestItem("2", "Exact Volume", 1000, 100, 5000, 2.5, null);
        FlippingItem item3 = createTestItem("3", "High Volume", 1000, 100, 10000, 2.5, null);

        assertFalse("Item below min volume should fail", plugin.passesFilters(item1));
        assertTrue("Item with exact min volume should pass", plugin.passesFilters(item2));
        assertTrue("Item above min volume should pass", plugin.passesFilters(item3));

        List<FlippingItem> filtered = plugin.filterItems(Arrays.asList(item1, item2, item3));
        assertEquals("Should filter out low volume items", 2, filtered.size());
        assertFalse(filtered.contains(item1));
    }

    @Test
    public void testMinScoreFilter() {
        when(config.minScore()).thenReturn(2.0);

        FlippingItem item1 = createTestItem("1", "Low Score", 1000, 100, 5000, 1.5, null);
        FlippingItem item2 = createTestItem("2", "Exact Score", 1000, 100, 5000, 2.0, null);
        FlippingItem item3 = createTestItem("3", "High Score", 1000, 100, 5000, 3.5, null);

        assertFalse("Item below min score should fail", plugin.passesFilters(item1));
        assertTrue("Item with exact min score should pass", plugin.passesFilters(item2));
        assertTrue("Item above min score should pass", plugin.passesFilters(item3));

        List<FlippingItem> filtered = plugin.filterItems(Arrays.asList(item1, item2, item3));
        assertEquals("Should filter out low score items", 2, filtered.size());
        assertFalse(filtered.contains(item1));
    }

    @Test
    public void testDumpFilter_ShowAll() {
        when(config.dumpFilter()).thenReturn(FlippingHelperConfig.DumpFilter.ALL);

        FlippingItem item1 = createTestItem("1", "No Dump Signal", 1000, 100, 5000, 2.5, null);
        FlippingItem item2 = createTestItem("2", "Zero Dump Score", 1000, 100, 5000, 2.5, 0.0);
        FlippingItem item3 = createTestItem("3", "Has Dump Signal", 1000, 100, 5000, 2.5, 1.0);

        assertTrue("Non-dump item should pass with ALL filter", plugin.passesFilters(item1));
        assertTrue("Zero dump item should pass with ALL filter", plugin.passesFilters(item2));
        assertTrue("Dump item should pass with ALL filter", plugin.passesFilters(item3));

        List<FlippingItem> filtered = plugin.filterItems(Arrays.asList(item1, item2, item3));
        assertEquals("All items should pass with ALL filter", 3, filtered.size());
    }

    @Test
    public void testDumpFilter_DumpOnly() {
        when(config.dumpFilter()).thenReturn(FlippingHelperConfig.DumpFilter.DUMP_ONLY);

        FlippingItem item1 = createTestItem("1", "No Dump Signal", 1000, 100, 5000, 2.5, null);
        FlippingItem item2 = createTestItem("2", "Zero Dump Score", 1000, 100, 5000, 2.5, 0.0);
        FlippingItem item3 = createTestItem("3", "Dump Score 1", 1000, 100, 5000, 2.5, 1.0);
        FlippingItem item4 = createTestItem("4", "Dump Score 5", 1000, 100, 5000, 2.5, 5.0);

        assertFalse("Non-dump item should fail with DUMP_ONLY filter", plugin.passesFilters(item1));
        assertFalse("Zero dump item should fail with DUMP_ONLY filter", plugin.passesFilters(item2));
        assertTrue("Dump item (score 1) should pass with DUMP_ONLY filter", plugin.passesFilters(item3));
        assertTrue("Dump item (score 5) should pass with DUMP_ONLY filter", plugin.passesFilters(item4));

        List<FlippingItem> filtered = plugin.filterItems(Arrays.asList(item1, item2, item3, item4));
        assertEquals("Only dump items should pass", 2, filtered.size());
        assertTrue(filtered.contains(item3));
        assertTrue(filtered.contains(item4));
    }

    @Test
    public void testDumpFilter_NoDump() {
        when(config.dumpFilter()).thenReturn(FlippingHelperConfig.DumpFilter.NO_DUMP);

        FlippingItem item1 = createTestItem("1", "No Dump Signal", 1000, 100, 5000, 2.5, null);
        FlippingItem item2 = createTestItem("2", "Zero Dump Score", 1000, 100, 5000, 2.5, 0.0);
        FlippingItem item3 = createTestItem("3", "Dump Score 1", 1000, 100, 5000, 2.5, 1.0);
        FlippingItem item4 = createTestItem("4", "Dump Score 5", 1000, 100, 5000, 2.5, 5.0);

        assertTrue("Non-dump item should pass with NO_DUMP filter", plugin.passesFilters(item1));
        assertTrue("Zero dump item should pass with NO_DUMP filter", plugin.passesFilters(item2));
        assertFalse("Dump item (score 1) should fail with NO_DUMP filter", plugin.passesFilters(item3));
        assertFalse("Dump item (score 5) should fail with NO_DUMP filter", plugin.passesFilters(item4));

        List<FlippingItem> filtered = plugin.filterItems(Arrays.asList(item1, item2, item3, item4));
        assertEquals("Only non-dump items should pass", 2, filtered.size());
        assertTrue(filtered.contains(item1));
        assertTrue(filtered.contains(item2));
    }

    @Test
    public void testMultipleFilters_Combined() {
        // Test multiple filters working together
        when(config.minBuyPrice()).thenReturn(1000);
        when(config.maxBuyPrice()).thenReturn(5000);
        when(config.minProfit()).thenReturn(100);
        when(config.minDailyVolume()).thenReturn(5000);
        when(config.minScore()).thenReturn(2.0);
        when(config.dumpFilter()).thenReturn(FlippingHelperConfig.DumpFilter.NO_DUMP);

        FlippingItem goodItem = createTestItem("1", "Perfect Item",
            3000, 200, 10000, 2.5, null);

        FlippingItem tooCheap = createTestItem("2", "Too Cheap",
            500, 200, 10000, 2.5, null);

        FlippingItem tooExpensive = createTestItem("3", "Too Expensive",
            6000, 200, 10000, 2.5, null);

        FlippingItem lowProfit = createTestItem("4", "Low Profit",
            3000, 50, 10000, 2.5, null);

        FlippingItem lowVolume = createTestItem("5", "Low Volume",
            3000, 200, 1000, 2.5, null);

        FlippingItem lowScore = createTestItem("6", "Low Score",
            3000, 200, 10000, 1.5, null);

        FlippingItem dumpItem = createTestItem("7", "Dump Item",
            3000, 200, 10000, 2.5, 1.0);

        assertTrue("Good item should pass all filters", plugin.passesFilters(goodItem));
        assertFalse("Too cheap should fail", plugin.passesFilters(tooCheap));
        assertFalse("Too expensive should fail", plugin.passesFilters(tooExpensive));
        assertFalse("Low profit should fail", plugin.passesFilters(lowProfit));
        assertFalse("Low volume should fail", plugin.passesFilters(lowVolume));
        assertFalse("Low score should fail", plugin.passesFilters(lowScore));
        assertFalse("Dump item should fail", plugin.passesFilters(dumpItem));

        List<FlippingItem> allItems = Arrays.asList(
            goodItem, tooCheap, tooExpensive, lowProfit, lowVolume, lowScore, dumpItem
        );
        List<FlippingItem> filtered = plugin.filterItems(allItems);

        assertEquals("Only one item should pass all filters", 1, filtered.size());
        assertTrue(filtered.contains(goodItem));
    }

    @Test
    public void testEmptyList() {
        List<FlippingItem> empty = Arrays.asList();
        List<FlippingItem> filtered = plugin.filterItems(empty);

        assertEquals("Empty list should return empty list", 0, filtered.size());
    }

    @Test
    public void testNullDumpScore_TreatedAsNonDump() {
        when(config.dumpFilter()).thenReturn(FlippingHelperConfig.DumpFilter.NO_DUMP);

        FlippingItem itemWithNull = createTestItem("1", "Null Dump", 1000, 100, 5000, 2.5, null);

        assertTrue("Item with null dump score should be treated as non-dump",
            plugin.passesFilters(itemWithNull));
    }

    @Test
    public void testItemWithZeroValues_WithNoFilters_ShouldPass() {
        // This simulates items that might have missing/zero values from API
        FlippingItem itemWithZeros = createTestItem("1", "Zero Item", 0, 0, 0, 0.0, null);

        // All filters set to 0 (default - no filtering)
        assertTrue("Item with all zero values should pass when no filters are set",
            plugin.passesFilters(itemWithZeros));
    }

    @Test
    public void testRealWorldScenario_DefaultFilters() {
        // Test with default filter settings (all 0, dump = ALL)
        // This is what the user is experiencing

        FlippingItem goodItem = createTestItem("1", "Good Item", 1000, 100, 5000, 2.5, null);
        FlippingItem zeroItem = createTestItem("2", "Zero Values", 0, 0, 0, 0.0, null);
        FlippingItem partialItem = createTestItem("3", "Partial Values", 500, 0, 1000, 1.0, null);

        // Default config: all zeros, dump filter = ALL
        assertTrue("Good item should pass with default filters", plugin.passesFilters(goodItem));
        assertTrue("Item with zero values should pass with default filters", plugin.passesFilters(zeroItem));
        assertTrue("Item with partial values should pass with default filters", plugin.passesFilters(partialItem));

        List<FlippingItem> items = Arrays.asList(goodItem, zeroItem, partialItem);
        List<FlippingItem> filtered = plugin.filterItems(items);

        assertEquals("All items should pass with default filters", 3, filtered.size());
    }
}
