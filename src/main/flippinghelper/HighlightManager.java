package flippinghelper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages widget highlights for the GE interface.
 * Uses a single persistent overlay that checks state on each render.
 */
@Slf4j
@Singleton
public class HighlightManager {

    private final Client client;
    private final GrandExchangeHelper geHelper;
    private final OverlayManager overlayManager;
    private final GeInteractionHandler interactionHandler;

    private GeHighlightOverlay highlightOverlay;
    private FlippingItem currentItem = null;
    private boolean highlightsEnabled = true;

    @Inject
    public HighlightManager(Client client, GrandExchangeHelper geHelper, OverlayManager overlayManager,
                           GeInteractionHandler interactionHandler) {
        this.client = client;
        this.geHelper = geHelper;
        this.overlayManager = overlayManager;
        this.interactionHandler = interactionHandler;
    }

    /**
     * Initialize the overlay system.
     * Must be called after injection is complete.
     */
    public void initialize() {
        if (highlightOverlay == null) {
            highlightOverlay = new GeHighlightOverlay(client, this, geHelper);
            overlayManager.add(highlightOverlay);
            log.debug("GeHighlightOverlay initialized and added");
        }
    }

    /**
     * Shutdown the overlay system.
     */
    public void shutdown() {
        if (highlightOverlay != null) {
            overlayManager.remove(highlightOverlay);
            highlightOverlay = null;
            log.debug("GeHighlightOverlay removed");
        }
    }

    /**
     * Set the item to highlight for.
     * The overlay will pick this up on its next render.
     */
    public void setCurrentItem(FlippingItem item) {
        this.currentItem = item;
        log.debug("Current item set to: {}", item != null ? item.getName() : "null");
    }

    /**
     * Clear the current item selection.
     */
    public void clearCurrentItem() {
        this.currentItem = null;
        log.debug("Current item cleared");
    }

    /**
     * Enable or disable highlights.
     */
    public void setHighlightsEnabled(boolean enabled) {
        this.highlightsEnabled = enabled;
        log.debug("Highlights enabled: {}", enabled);
    }

    /**
     * Check if highlights are enabled.
     */
    public boolean isHighlightsEnabled() {
        return highlightsEnabled;
    }

    /**
     * Get the currently highlighted item.
     */
    public FlippingItem getCurrentItem() {
        return currentItem;
    }

    /**
     * Redraw all highlights based on current state.
     * With persistent overlay, this is a no-op - overlay checks state itself.
     */
    public void redraw() {
        // No-op: the persistent overlay handles its own rendering
    }

    /**
     * Remove all active highlights.
     * With persistent overlay, this is a no-op.
     */
    public void removeAllHighlights() {
        // No-op: we keep the overlay registered, it just won't render anything if currentItem is null
    }

    /**
     * Handle mouse movement for hover detection.
     * Currently disabled with new overlay system.
     */
    public void handleMouseMove(Point mousePos) {
        // TODO: Implement if needed with persistent overlay
    }

    /**
     * Handle mouse click for overlay interaction.
     * Currently disabled with new overlay system.
     */
    public boolean handleMouseClick(Point mousePos) {
        // TODO: Implement if needed with persistent overlay
        return false;
    }
}
