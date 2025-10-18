package flippinghelper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuEntry;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.VarClientStr;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles automated GE interactions for auto-filling offers.
 */
@Slf4j
@Singleton
public class GeInteractionHandler {

    private final Client client;
    private final ClientThread clientThread;
    private final GrandExchangeHelper geHelper;

    @Inject
    public GeInteractionHandler(Client client, ClientThread clientThread, GrandExchangeHelper geHelper) {
        this.client = client;
        this.clientThread = clientThread;
        this.geHelper = geHelper;
    }

    /**
     * Auto-fill a GE offer for the given item.
     * This initiates the process but relies on user interaction for item search.
     */
    public void autoFillOffer(FlippingItem item) {
        log.info("Auto-filling offer for: {} ({}) - {} {}x @ {} gp",
            item.getName(), item.getId(), item.getPredictedAction(),
            item.getQuantity(), item.getPredictedAction().equals("buy") ?
                item.getAdjustedLowPrice() : item.getAdjustedHighPrice());

        if (!geHelper.isOpen()) {
            log.warn("GE is not open, cannot auto-fill");
            return;
        }

        String action = item.getPredictedAction();

        if ("buy".equals(action)) {
            startBuyOffer(item);
        } else if ("sell".equals(action)) {
            startSellOffer(item);
        }
    }

    /**
     * Start a buy offer.
     */
    private void startBuyOffer(FlippingItem item) {
        clientThread.invoke(() -> {
            int emptySlot = geHelper.findEmptySlot();
            if (emptySlot == -1) {
                log.warn("No empty GE slot available");
                return;
            }

            Widget buyButton = geHelper.getBuyButton(emptySlot);
            if (buyButton == null || buyButton.isHidden()) {
                log.warn("Buy button not found for slot {}", emptySlot);
                return;
            }

            // Click the buy button
            clickWidget(buyButton);
            log.debug("Clicked buy button for slot {}", emptySlot);

            // Note: After clicking, the GE search interface will open
            // The user will need to search and select the item manually
            // Once selected, we can auto-fill price and quantity via keybind or further automation
        });
    }

    /**
     * Start a sell offer.
     */
    private void startSellOffer(FlippingItem item) {
        clientThread.invoke(() -> {
            // Find the item in inventory
            Widget inventoryItem = findInventoryItemWidget(item.getId());
            if (inventoryItem == null) {
                log.warn("Item {} not found in inventory", item.getName());
                return;
            }

            // Check if we have an empty slot
            int emptySlot = geHelper.findEmptySlot();
            if (emptySlot == -1) {
                log.warn("No empty GE slot available");
                return;
            }

            // Click the inventory item (this should open the sell interface)
            clickWidget(inventoryItem);
            log.debug("Clicked inventory item {} for selling", item.getName());
        });
    }

    /**
     * Set the price in the GE chatbox input.
     */
    public void setPrice(long price) {
        setChatboxValue((int) price);
    }

    /**
     * Set the quantity in the GE chatbox input.
     */
    public void setQuantity(int quantity) {
        setChatboxValue(quantity);
    }

    /**
     * Set a value in the GE chatbox input (works for both price and quantity).
     */
    public void setChatboxValue(int value) {
        Widget chatboxInput = client.getWidget(ComponentID.CHATBOX_FULL_INPUT);
        if (chatboxInput == null) {
            log.warn("Chatbox input not found");
            return;
        }

        chatboxInput.setText(value + "*");
        client.setVarcStrValue(VarClientStr.INPUT_TEXT, String.valueOf(value));
        log.debug("Set chatbox value to: {}", value);
    }

    /**
     * Check if the user is currently setting the quantity for an offer.
     */
    public boolean isSettingQuantity() {
        Widget chatboxTitle = client.getWidget(ComponentID.CHATBOX_TITLE);
        if (chatboxTitle == null) {
            return false;
        }
        String titleText = chatboxTitle.getText();
        return titleText != null && (titleText.equals("How many do you wish to buy?") || titleText.equals("How many do you wish to sell?"));
    }

    /**
     * Check if the user is currently setting the price for an offer.
     */
    public boolean isSettingPrice() {
        Widget chatboxTitle = client.getWidget(ComponentID.CHATBOX_TITLE);
        if (chatboxTitle == null) {
            return false;
        }
        String titleText = chatboxTitle.getText();

        // Also check that we're in the GE offer screen
        Widget offerContainer = client.getWidget(465, 26);
        if (offerContainer == null) {
            return false;
        }

        return titleText != null && titleText.equals("Set a price for each item:");
    }

    /**
     * Click a widget by invoking its menu action.
     * Note: This is a simplified version. Full implementation would require
     * proper menu action parameters based on the widget type.
     */
    private void clickWidget(Widget widget) {
        if (widget == null) {
            return;
        }

        MenuEntry menuEntry = client.createMenuEntry(-1)
            .setOption("")
            .setTarget("")
            .setIdentifier(widget.getItemId())
            .setType(MenuAction.CC_OP)
            .setParam0(widget.getIndex())
            .setParam1(widget.getId())
            .setForceLeftClick(true);

        client.setMenuEntries(new MenuEntry[]{menuEntry});
    }

    /**
     * Find inventory widget for a specific item ID.
     */
    private Widget findInventoryItemWidget(String itemIdStr) {
        try {
            int itemId = Integer.parseInt(itemIdStr);

            // GE inventory widget (when GE is open)
            Widget inventory = client.getWidget(467, 0);
            if (inventory == null) {
                // Regular inventory
                inventory = client.getWidget(149, 0);
            }

            if (inventory == null) {
                return null;
            }

            Widget[] children = inventory.getDynamicChildren();
            if (children == null) {
                return null;
            }

            for (Widget widget : children) {
                if (widget.getItemId() == itemId) {
                    return widget;
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid item ID: {}", itemIdStr);
        }

        return null;
    }
}
