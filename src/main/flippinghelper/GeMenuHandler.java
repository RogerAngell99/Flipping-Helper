package flippinghelper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles menu interactions for the Grand Exchange.
 * Adds custom menu entries to auto-position items when clicking on highlighted widgets.
 */
@Slf4j
@Singleton
public class GeMenuHandler {

    private final Client client;
    private final HighlightManager highlightManager;
    private final GrandExchangeHelper geHelper;

    @Inject
    public GeMenuHandler(Client client, HighlightManager highlightManager, GrandExchangeHelper geHelper) {
        this.client = client;
        this.highlightManager = highlightManager;
        this.geHelper = geHelper;
    }

    /**
     * Called when a menu entry is added.
     * Can be used to modify or add custom menu entries.
     */
    public void onMenuEntryAdded(MenuEntryAdded event) {
        FlippingItem currentItem = highlightManager.getCurrentItem();
        if (currentItem == null) {
            return;
        }

        if (!geHelper.isOpen()) {
            return;
        }

        // Add custom menu entry for buy/sell actions
        String action = currentItem.getPredictedAction();
        if ("buy".equals(action)) {
            if (event.getOption().equals("Buy")) {
                addCustomMenuEntry("Quick-buy " + currentItem.getName(), event);
            }
        } else if ("sell".equals(action)) {
            if (event.getOption().equals("Sell")) {
                addCustomMenuEntry("Quick-sell " + currentItem.getName(), event);
            }
        }
    }

    /**
     * Called when a menu option is clicked.
     * Handles custom menu actions.
     */
    public void onMenuOptionClicked(MenuOptionClicked event) {
        FlippingItem currentItem = highlightManager.getCurrentItem();
        if (currentItem == null) {
            return;
        }

        String option = event.getMenuOption();
        if (option.startsWith("Quick-buy") || option.startsWith("Quick-sell")) {
            handleQuickAction(currentItem, option);
        }
    }

    /**
     * Add a custom menu entry.
     */
    private void addCustomMenuEntry(String option, MenuEntryAdded event) {
        client.getMenu()
            .createMenuEntry(-1)
            .setOption(option)
            .setTarget(event.getTarget())
            .setType(event.getType())
            .setIdentifier(event.getIdentifier())
            .setParam0(event.getActionParam0())
            .setParam1(event.getActionParam1());
    }

    /**
     * Handle quick buy/sell action.
     * This would typically involve:
     * 1. Clicking the buy/sell button
     * 2. Searching for the item
     * 3. Setting the price and quantity
     * 4. Confirming the offer
     *
     * Note: Actual implementation would require invoking menu actions programmatically,
     * which is complex and may require additional RuneLite API knowledge.
     */
    private void handleQuickAction(FlippingItem item, String action) {
        log.info("Quick action requested: {} for item {} ({})", action, item.getName(), item.getId());

        String actionType = item.getPredictedAction();
        int price = "buy".equals(actionType) ? item.getAdjustedLowPrice() : item.getAdjustedHighPrice();
        int quantity = item.getQuantity();

        log.info("Would {} {} x {} for {} gp each", actionType, quantity, item.getName(), price);

        // TODO: Implement actual GE interaction
        // This would require:
        // 1. Using invokeMenuAction to click widgets
        // 2. Sending chat input for item search
        // 3. Setting varps for price and quantity
        // 4. Clicking confirm button
        //
        // Example (pseudo-code):
        // if ("buy".equals(actionType)) {
        //     int emptySlot = geHelper.findEmptySlot();
        //     Widget buyButton = geHelper.getBuyButton(emptySlot);
        //     // Invoke menu action on buyButton
        //     // Search for item
        //     // Set price and quantity
        //     // Click confirm
        // }
    }
}
