package flippinghelper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages widget highlights for the GE interface.
 * Highlights relevant widgets based on the currently selected/hovered item.
 */
@Slf4j
@Singleton
public class HighlightManager {

    private static final Color HIGHLIGHT_COLOR = new Color(30, 144, 255); // Dodger blue

    private final Client client;
    private final GrandExchangeHelper geHelper;
    private final OverlayManager overlayManager;

    private final List<WidgetHighlightOverlay> activeOverlays = new ArrayList<>();
    private FlippingItem currentItem = null;
    private boolean highlightsEnabled = true;

    @Inject
    public HighlightManager(Client client, GrandExchangeHelper geHelper, OverlayManager overlayManager) {
        this.client = client;
        this.geHelper = geHelper;
        this.overlayManager = overlayManager;
    }

    /**
     * Set the item to highlight for.
     */
    public void setCurrentItem(FlippingItem item) {
        this.currentItem = item;
        redraw();
    }

    /**
     * Clear the current item selection.
     */
    public void clearCurrentItem() {
        this.currentItem = null;
        removeAllHighlights();
    }

    /**
     * Enable or disable highlights.
     */
    public void setHighlightsEnabled(boolean enabled) {
        this.highlightsEnabled = enabled;
        if (!enabled) {
            removeAllHighlights();
        } else {
            redraw();
        }
    }

    /**
     * Redraw all highlights based on current state.
     */
    public void redraw() {
        removeAllHighlights();

        if (!highlightsEnabled || currentItem == null) {
            return;
        }

        if (!geHelper.isOpen()) {
            return;
        }

        if (geHelper.isHomeScreenOpen()) {
            drawHomeScreenHighlights();
        } else if (geHelper.isSlotOpen()) {
            drawOfferScreenHighlights();
        }
    }

    /**
     * Draw highlights on the GE home screen.
     */
    private void drawHomeScreenHighlights() {
        String action = currentItem.getPredictedAction();

        if ("buy".equals(action)) {
            // Highlight an empty buy slot
            int emptySlot = geHelper.findEmptySlot();
            if (emptySlot != -1) {
                Widget buyButton = geHelper.getBuyButton(emptySlot);
                if (buyButton != null && !buyButton.isHidden()) {
                    addHighlight(buyButton, HIGHLIGHT_COLOR, new Rectangle(0, 0, 45, 44));
                    log.debug("Highlighting buy button for slot {}", emptySlot);
                }
            }
        } else if ("sell".equals(action)) {
            // Highlight the item in inventory
            Widget inventoryItem = findInventoryItemWidget(currentItem.getId());
            if (inventoryItem != null && !inventoryItem.isHidden()) {
                addHighlight(inventoryItem, HIGHLIGHT_COLOR, new Rectangle(0, 0, 34, 32));
                log.debug("Highlighting inventory item {}", currentItem.getName());
            }
        }
    }

    /**
     * Draw highlights on the GE offer screen.
     */
    private void drawOfferScreenHighlights() {
        // Highlight price and quantity buttons
        Widget setPriceButton = geHelper.getSetPriceButton();
        if (setPriceButton != null && !setPriceButton.isHidden()) {
            addHighlight(setPriceButton, HIGHLIGHT_COLOR, new Rectangle(1, 6, 33, 23));
        }

        Widget setQuantityButton = geHelper.getSetQuantityButton();
        if (setQuantityButton != null && !setQuantityButton.isHidden()) {
            addHighlight(setQuantityButton, HIGHLIGHT_COLOR, new Rectangle(1, 6, 33, 23));
        }

        // Optionally highlight confirm button
        Widget confirmButton = geHelper.getConfirmButton();
        if (confirmButton != null && !confirmButton.isHidden()) {
            addHighlight(confirmButton, HIGHLIGHT_COLOR, new Rectangle(1, 1, 150, 38));
        }
    }

    /**
     * Add a highlight overlay for a widget.
     */
    private void addHighlight(Widget widget, Color color, Rectangle relativeBounds) {
        SwingUtilities.invokeLater(() -> {
            WidgetHighlightOverlay overlay = new WidgetHighlightOverlay(widget, color, relativeBounds);
            activeOverlays.add(overlay);
            overlayManager.add(overlay);
        });
    }

    /**
     * Remove all active highlights.
     */
    public void removeAllHighlights() {
        SwingUtilities.invokeLater(() -> {
            for (WidgetHighlightOverlay overlay : activeOverlays) {
                overlayManager.remove(overlay);
            }
            activeOverlays.clear();
        });
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

    /**
     * Get the currently highlighted item.
     */
    public FlippingItem getCurrentItem() {
        return currentItem;
    }
}
