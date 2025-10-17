package flippinghelper;

import net.runelite.api.Client;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Helper class to access Grand Exchange widgets and state.
 */
@Singleton
public class GrandExchangeHelper {

    private static final int CURRENTLY_OPEN_GE_SLOT_VARBIT_ID = 4439;

    private final Client client;

    @Inject
    public GrandExchangeHelper(Client client) {
        this.client = client;
    }

    /**
     * Check if the GE interface is open.
     */
    public boolean isOpen() {
        return client.getWidget(InterfaceID.GRAND_EXCHANGE, 7) != null;
    }

    /**
     * Check if the GE home screen is open (not in an offer slot).
     */
    public boolean isHomeScreenOpen() {
        return isOpen() && !isSlotOpen();
    }

    /**
     * Check if an offer slot is currently open.
     */
    public boolean isSlotOpen() {
        return getOpenSlot() != -1;
    }

    /**
     * Get the currently open slot index (-1 if none).
     */
    public int getOpenSlot() {
        return client.getVarbitValue(CURRENTLY_OPEN_GE_SLOT_VARBIT_ID) - 1;
    }

    /**
     * Get the widget for a specific GE slot.
     */
    public Widget getSlotWidget(int slot) {
        return client.getWidget(465, 7 + slot);
    }

    /**
     * Get the buy button widget for a specific slot.
     */
    public Widget getBuyButton(int slot) {
        Widget slotWidget = getSlotWidget(slot);
        if (slotWidget == null) {
            return null;
        }
        return slotWidget.getChild(0);
    }

    /**
     * Get the sell button widget for a specific slot (same widget as buy).
     */
    public Widget getSellButton(int slot) {
        return getBuyButton(slot);
    }

    /**
     * Get the collect button widget.
     */
    public Widget getCollectButton() {
        Widget topBar = client.getWidget(465, 6);
        if (topBar == null) {
            return null;
        }
        return topBar.getChild(2);
    }

    /**
     * Get the offer container widget (when in an offer screen).
     */
    public Widget getOfferContainerWidget() {
        return client.getWidget(465, 26);
    }

    /**
     * Get the confirm button widget in the offer screen.
     */
    public Widget getConfirmButton() {
        Widget offerContainer = getOfferContainerWidget();
        if (offerContainer == null) {
            return null;
        }
        return offerContainer.getChild(58);
    }

    /**
     * Get the set quantity button widget.
     */
    public Widget getSetQuantityButton() {
        Widget offerContainer = getOfferContainerWidget();
        if (offerContainer == null) {
            return null;
        }
        return offerContainer.getChild(51);
    }

    /**
     * Get the set price button widget.
     */
    public Widget getSetPriceButton() {
        Widget offerContainer = getOfferContainerWidget();
        if (offerContainer == null) {
            return null;
        }
        return offerContainer.getChild(54);
    }

    /**
     * Get the current offer quantity from the interface.
     */
    public int getOfferQuantity() {
        return client.getVarbitValue(4396);
    }

    /**
     * Get the current offer price from the interface.
     */
    public int getOfferPrice() {
        return client.getVarbitValue(4398);
    }

    /**
     * Find an empty GE slot.
     * @return slot index (0-7) or -1 if no empty slot
     */
    public int findEmptySlot() {
        for (int i = 0; i < 8; i++) {
            Widget slotWidget = getSlotWidget(i);
            if (slotWidget != null) {
                Widget buyButton = getBuyButton(i);
                if (buyButton != null && !buyButton.isHidden()) {
                    return i;
                }
            }
        }
        return -1;
    }
}
