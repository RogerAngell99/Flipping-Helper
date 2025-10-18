package flippinghelper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.VarClientStr;
import net.runelite.api.Varbits;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;

import static net.runelite.api.VarPlayer.CURRENT_GE_ITEM;

/**
 * Single persistent overlay for all GE highlighting.
 * Checks state on every render to determine what to draw.
 * Follows a progressive highlight order based on the user's current step.
 */
@Slf4j
public class GeHighlightOverlay extends Overlay {
    private static final Color HIGHLIGHT_COLOR = new Color(30, 144, 255, 100); // Dodger blue, semi-transparent

    private final Client client;
    private final HighlightManager highlightManager;
    private final GrandExchangeHelper geHelper;

    public GeHighlightOverlay(Client client, HighlightManager highlightManager, GrandExchangeHelper geHelper) {
        this.client = client;
        this.highlightManager = highlightManager;
        this.geHelper = geHelper;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            // Check if we should be rendering anything
            if (graphics == null || !highlightManager.isHighlightsEnabled()) {
                return null;
            }

            FlippingItem currentItem = highlightManager.getCurrentItem();
            if (currentItem == null) {
                return null;
            }

            if (!geHelper.isOpen()) {
                return null;
            }

            // Draw highlights based on current GE state
            if (geHelper.isHomeScreenOpen()) {
                renderHomeScreenHighlights(graphics, currentItem);
            } else if (geHelper.isSlotOpen()) {
                renderOfferScreenHighlights(graphics, currentItem);
            }

        } catch (Throwable t) {
            // Silently handle any errors - widgets may have become invalid
            return null;
        }

        return null;
    }

    /**
     * Render highlights on the GE home screen.
     * Follows Flipping Copilot's logic: highlight ONE thing at a time based on what the user needs to do.
     */
    private void renderHomeScreenHighlights(Graphics2D graphics, FlippingItem item) {
        String action = item.getPredictedAction();

        // Only highlight the NEXT action the user should take
        if ("buy".equals(action)) {
            // Highlight an empty buy slot
            int emptySlot = geHelper.findEmptySlot();
            if (emptySlot != -1) {
                Widget buyButton = geHelper.getBuyButton(emptySlot);
                renderWidgetHighlight(graphics, buyButton, new Rectangle(0, 0, 45, 44));
            }
        } else if ("sell".equals(action)) {
            // Highlight the item in inventory
            Widget inventoryItem = findInventoryItemWidget(item.getId());
            renderWidgetHighlight(graphics, inventoryItem, new Rectangle(0, 0, 34, 32));
        }
    }

    /**
     * Render highlights on the GE offer screen.
     * Progressive highlighting: only highlight the NEXT step the user needs to complete.
     */
    private void renderOfferScreenHighlights(Graphics2D graphics, FlippingItem item) {
        String offerType = getOfferType();
        int currentItemId = client.getVarpValue(CURRENT_GE_ITEM);

        // Check if the correct offer type is selected
        if (!item.getPredictedAction().equals(offerType)) {
            return;
        }

        try {
            int itemId = Integer.parseInt(item.getId());

            // If item is selected and matches our suggestion
            if (currentItemId == itemId) {
                // Check if offer details are correct
                if (isOfferDetailsCorrect(item)) {
                    // Everything is set correctly - highlight confirm button
                    highlightConfirm(graphics);
                } else {
                    // Details need adjustment - highlight price and/or quantity
                    if (geHelper.getOfferPrice() != getPriceForItem(item)) {
                        highlightPrice(graphics);
                    }
                    if (geHelper.getOfferQuantity() != item.getQuantity()) {
                        highlightQuantity(graphics);
                    }
                }
            } else if (currentItemId == -1) {
                // No item selected yet - highlight the item in search results
                highlightItemInSearch(graphics, item);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid item ID: {}", item.getId());
        }
    }

    /**
     * Highlight the suggested item in the search results.
     */
    private void highlightItemInSearch(Graphics2D graphics, FlippingItem item) {
        // Don't highlight if user is typing
        if (!client.getVarcStrValue(VarClientStr.INPUT_TEXT).isEmpty()) {
            return;
        }

        Widget searchResults = client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS);
        if (searchResults == null) {
            return;
        }

        // Look for the item in dynamic children (search results)
        Widget[] children = searchResults.getDynamicChildren();
        if (children != null) {
            for (Widget widget : children) {
                if (widget.getName().equals("<col=ff9040>" + item.getName() + "</col>")) {
                    renderWidgetHighlight(graphics, widget, new Rectangle(0, 0, widget.getWidth(), widget.getHeight()));
                    return;
                }
            }
        }

        // Also check the static item widget (child 0) - the Helper item we created
        Widget helperWidget = searchResults.getChild(0);
        if (helperWidget != null) {
            try {
                int targetItemId = Integer.parseInt(item.getId());
                // Check if this is our helper widget by checking child(3) for the item icon
                Widget itemIconWidget = searchResults.getChild(3);
                if (itemIconWidget != null && itemIconWidget.getItemId() == targetItemId) {
                    renderWidgetHighlight(graphics, helperWidget, new Rectangle(0, 0, helperWidget.getWidth(), helperWidget.getHeight()));
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
    }

    /**
     * Check if offer details are correct for the suggested item.
     */
    private boolean isOfferDetailsCorrect(FlippingItem item) {
        return geHelper.getOfferPrice() == getPriceForItem(item)
                && geHelper.getOfferQuantity() == item.getQuantity();
    }

    /**
     * Get the appropriate price for the item based on action.
     */
    private int getPriceForItem(FlippingItem item) {
        if ("buy".equals(item.getPredictedAction())) {
            return (int) item.getAdjustedLowPrice();
        } else {
            return (int) item.getAdjustedHighPrice();
        }
    }

    /**
     * Get the current offer type (buy/sell).
     */
    private String getOfferType() {
        return client.getVarbitValue(Varbits.GE_OFFER_CREATION_TYPE) == 1 ? "sell" : "buy";
    }

    /**
     * Highlight the price button.
     */
    private void highlightPrice(Graphics2D graphics) {
        Widget setPriceButton = geHelper.getSetPriceButton();
        renderWidgetHighlight(graphics, setPriceButton, new Rectangle(1, 6, 33, 23));
    }

    /**
     * Highlight the quantity button.
     */
    private void highlightQuantity(Graphics2D graphics) {
        Widget setQuantityButton = geHelper.getSetQuantityButton();
        renderWidgetHighlight(graphics, setQuantityButton, new Rectangle(1, 6, 33, 23));
    }

    /**
     * Highlight the confirm button.
     */
    private void highlightConfirm(Graphics2D graphics) {
        Widget confirmButton = geHelper.getConfirmButton();
        renderWidgetHighlight(graphics, confirmButton, new Rectangle(1, 1, 150, 38));
    }

    /**
     * Render a highlight rectangle on a widget.
     */
    private void renderWidgetHighlight(Graphics2D graphics, Widget widget, Rectangle relativeBounds) {
        if (widget == null || widget.isHidden()) {
            return;
        }

        Rectangle widgetBounds = widget.getBounds();
        if (widgetBounds == null) {
            return;
        }

        // Apply relative bounds adjustments
        Rectangle highlightBounds = new Rectangle(
            widgetBounds.x + relativeBounds.x,
            widgetBounds.y + relativeBounds.y,
            relativeBounds.width,
            relativeBounds.height
        );

        // Draw semi-transparent highlight
        graphics.setColor(HIGHLIGHT_COLOR);
        graphics.fillRect(highlightBounds.x, highlightBounds.y, highlightBounds.width, highlightBounds.height);
    }

    /**
     * Render item info overlay.
     * TODO: Add text rendering with item name, prices, profit, etc.
     */
    private void renderItemInfo(Graphics2D graphics, Widget widget, FlippingItem item) {
        // Disabled for now - just show the blue highlight
        // TODO: Implement text overlay with item information
    }

    /**
     * Find the inventory widget for a specific item ID.
     */
    private Widget findInventoryItemWidget(String itemIdStr) {
        try {
            int itemId = Integer.parseInt(itemIdStr);

            // GE inventory widget (when GE is open)
            Widget inventory = client.getWidget(467, 0);
            if (inventory == null) {
                // Regular inventory widget
                inventory = client.getWidget(149, 0);
            }

            if (inventory == null) {
                return null;
            }

            Widget[] children = inventory.getDynamicChildren();
            if (children == null) {
                return null;
            }

            // Find the item in inventory
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
