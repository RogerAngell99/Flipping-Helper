package flippinghelper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.VarClientInt;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles creating a clickable widget in the GE search interface for the currently selected item.
 * Similar to Flipping Copilot's "Copilot item:" feature.
 */
@Slf4j
@Singleton
public class GeSearchAutoFillHandler {

    private static final String HELPER_LABEL = "Helper item:";

    private final Client client;
    private final GrandExchangeHelper geHelper;

    private FlippingItem currentItem = null;
    private boolean searchInterfaceWasOpen = false;

    @Inject
    public GeSearchAutoFillHandler(Client client, GrandExchangeHelper geHelper) {
        this.client = client;
        this.geHelper = geHelper;
    }

    /**
     * Set the current item to auto-fill.
     */
    public void setCurrentItem(FlippingItem item) {
        this.currentItem = item;
    }

    /**
     * Clear the current item.
     */
    public void clearCurrentItem() {
        this.currentItem = null;
    }

    /**
     * Check and handle GE search interface state.
     * Should be called on game tick.
     */
    public void tick() {
        boolean searchInterfaceOpen = isSearchInterfaceOpen();

        // Detect when search interface just opened
        if (searchInterfaceOpen && !searchInterfaceWasOpen) {
            onSearchInterfaceOpened();
        }

        searchInterfaceWasOpen = searchInterfaceOpen;
    }

    /**
     * Called when the GE search interface opens.
     */
    private void onSearchInterfaceOpened() {
        if (currentItem == null) {
            log.debug("No current item to show in search");
            return;
        }

        // Check if we're in a buy/sell offer screen
        if (!geHelper.isOpen()) {
            log.debug("GE is not open");
            return;
        }

        log.debug("Search interface opened, showing clickable widget for: {}", currentItem.getName());

        // Create the clickable widget in the search results
        showSuggestedItemInSearch();
    }

    /**
     * Show the suggested item as a clickable widget in the GE search interface.
     */
    private void showSuggestedItemInSearch() {
        if (currentItem == null) {
            return;
        }

        try {
            int itemId = Integer.parseInt(currentItem.getId());
            String itemName = currentItem.getName();

            // Check if the "Show last searched item" option is enabled
            if (isPreviousSearchSet() && isShowLastSearchEnabled()) {
                // Update existing widgets
                setPreviousSearch(itemId, itemName);
            } else {
                // Create new widgets
                createPreviousSearchWidget(itemId, itemName);
                createPreviousSearchTextWidget();
                createPreviousSearchItemWidget(itemId);
                createPreviousSearchItemNameWidget(itemName);
            }

            log.info("Showed clickable widget for: {}", itemName);
        } catch (NumberFormatException e) {
            log.error("Invalid item ID: {}", currentItem.getId(), e);
        }
    }

    /**
     * Check if the previous search widgets are already set.
     */
    private boolean isPreviousSearchSet() {
        Widget searchResults = client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS);
        if (searchResults == null) {
            return false;
        }
        Widget previousSearch = searchResults.getChild(0);
        return previousSearch != null && previousSearch.getType() == WidgetType.RECTANGLE;
    }

    /**
     * Check if the "Show last searched item" option is enabled.
     * This is determined by whether the game already shows a previous search widget.
     */
    private boolean isShowLastSearchEnabled() {
        Widget searchResults = client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS);
        if (searchResults == null) {
            return false;
        }
        // If child(0) exists as a rectangle, the option is enabled
        Widget child0 = searchResults.getChild(0);
        return child0 != null;
    }

    /**
     * Update the existing previous search widgets.
     */
    private void setPreviousSearch(int itemId, String itemName) {
        Widget searchResults = client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS);
        if (searchResults == null) {
            return;
        }

        Widget previousSearch = searchResults.getChild(0);
        if (previousSearch != null) {
            previousSearch.setOnOpListener(754, itemId, 84);
            previousSearch.setOnKeyListener(754, itemId, -2147483640);
            previousSearch.setName("<col=ff9040>" + itemName + "</col>");
        }

        Widget previousSearchText = searchResults.getChild(1);
        if (previousSearchText != null) {
            previousSearchText.setText(HELPER_LABEL);
        }

        Widget itemNameWidget = searchResults.getChild(2);
        if (itemNameWidget != null) {
            itemNameWidget.setText(itemName);
        }

        Widget item = searchResults.getChild(3);
        if (item != null) {
            item.setItemId(itemId);
        }
    }

    /**
     * Create the main clickable widget for the previous search.
     */
    private void createPreviousSearchWidget(int itemId, String itemName) {
        Widget parentWidget = client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS);
        if (parentWidget == null) {
            log.warn("Search results widget not found");
            return;
        }

        Widget widget = parentWidget.createChild(WidgetType.RECTANGLE);
        widget.setTextColor(0xFFFFFF);
        widget.setOpacity(255);
        widget.setName("<col=ff9040>" + itemName + "</col>");
        widget.setHasListener(true);
        widget.setFilled(true);
        widget.setOriginalX(114);
        widget.setOriginalY(0);
        widget.setOriginalWidth(256);
        widget.setOriginalHeight(32);
        widget.setOnOpListener(754, itemId, 84);
        widget.setOnKeyListener(754, itemId, -2147483640);
        widget.setAction(0, "Select");

        // Set opacity to 200 when mouse is hovering (semi-transparent)
        widget.setOnMouseOverListener((JavaScriptCallback) ev -> {
            widget.setOpacity(200);
        });

        // Set opacity back to 255 when mouse is not hovering
        widget.setOnMouseLeaveListener((JavaScriptCallback) ev -> {
            widget.setOpacity(255);
        });

        widget.revalidate();
    }

    /**
     * Create the "Helper item:" text widget.
     */
    private void createPreviousSearchTextWidget() {
        Widget parentWidget = client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS);
        if (parentWidget == null) {
            return;
        }

        Widget widget = parentWidget.createChild(WidgetType.TEXT);
        widget.setText(HELPER_LABEL);
        widget.setFontId(495);
        widget.setOriginalX(114);
        widget.setOriginalY(0);
        widget.setOriginalWidth(95);
        widget.setOriginalHeight(32);
        widget.setYTextAlignment(1);
        widget.revalidate();
    }

    /**
     * Create the item icon widget.
     */
    private void createPreviousSearchItemWidget(int itemId) {
        Widget parentWidget = client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS);
        if (parentWidget == null) {
            return;
        }

        Widget widget = parentWidget.createChild(WidgetType.GRAPHIC);
        widget.setItemId(itemId);
        widget.setItemQuantity(1);
        widget.setItemQuantityMode(0);
        widget.setRotationX(550);
        widget.setModelZoom(1031);
        widget.setBorderType(1);
        widget.setOriginalX(214);
        widget.setOriginalY(0);
        widget.setOriginalWidth(36);
        widget.setOriginalHeight(32);
        widget.revalidate();
    }

    /**
     * Create the item name text widget.
     */
    private void createPreviousSearchItemNameWidget(String itemName) {
        Widget parentWidget = client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS);
        if (parentWidget == null) {
            return;
        }

        Widget widget = parentWidget.createChild(WidgetType.TEXT);
        widget.setText(itemName);
        widget.setFontId(495);
        widget.setOriginalX(254);
        widget.setOriginalY(0);
        widget.setOriginalWidth(116);
        widget.setOriginalHeight(32);
        widget.setYTextAlignment(1);
        widget.revalidate();
    }

    /**
     * Check if the GE search interface is currently open.
     */
    private boolean isSearchInterfaceOpen() {
        // Check if the search input type is active (14 = GE search)
        if (client.getVarcIntValue(VarClientInt.INPUT_TYPE) == 14) {
            Widget searchResults = client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS);
            return searchResults != null;
        }
        return false;
    }

    /**
     * Get the current item being suggested.
     */
    public FlippingItem getCurrentItem() {
        return currentItem;
    }
}
