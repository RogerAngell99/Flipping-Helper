package flippinghelper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.widgets.*;

/**
 * Creates clickable text widgets above the GE chatbox to auto-fill price and quantity.
 * Similar to Flipping Copilot's OfferEditor.
 */
@Slf4j
public class GeOfferAutoFillWidget {
    private static final int MOUSE_OFF_TEXT_COLOR = 0x0040FF; // Blue
    private static final int MOUSE_ON_TEXT_COLOR = 0xFFFFFF; // White

    private final Client client;
    private final GeInteractionHandler interactionHandler;

    private Widget textWidget;

    public GeOfferAutoFillWidget(Client client, GeInteractionHandler interactionHandler, Widget parent) {
        this.client = client;
        this.interactionHandler = interactionHandler;

        if (parent == null) {
            log.warn("Cannot create auto-fill widget: parent is null");
            return;
        }

        // Create the text widget as a child of the chatbox container
        textWidget = parent.createChild(-1, WidgetType.TEXT);
        prepareTextWidget(textWidget);
    }

    /**
     * Configure the text widget appearance and position.
     */
    private void prepareTextWidget(Widget widget) {
        widget.setTextColor(MOUSE_OFF_TEXT_COLOR);
        widget.setFontId(FontID.VERDANA_11_BOLD);
        widget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
        widget.setOriginalX(10);
        widget.setOriginalY(40);
        widget.setOriginalHeight(20);
        widget.setXTextAlignment(WidgetTextAlignment.LEFT);
        widget.setWidthMode(WidgetSizeMode.MINUS);
        widget.revalidate();
    }

    /**
     * Show clickable text for setting the price.
     */
    public void showPrice(int price) {
        if (textWidget == null) {
            log.warn("Cannot show price: text widget is null");
            return;
        }

        shiftChatboxWidgetsDown();

        textWidget.setText("set to Helper price: " + String.format("%,d", price) + " gp");
        textWidget.setAction(0, "Set price");
        setHoverListeners(textWidget);

        // Click handler to auto-fill the price
        textWidget.setOnOpListener((JavaScriptCallback) ev -> {
            interactionHandler.setChatboxValue(price);
            log.info("Auto-filled price: {}", price);
        });

        textWidget.revalidate();
    }

    /**
     * Show clickable text for setting the quantity.
     */
    public void showQuantity(int quantity) {
        if (textWidget == null) {
            log.warn("Cannot show quantity: text widget is null");
            return;
        }

        shiftChatboxWidgetsDown();

        textWidget.setText("set to Helper quantity: " + String.format("%,d", quantity));
        textWidget.setAction(1, "Set quantity");
        setHoverListeners(textWidget);

        // Click handler to auto-fill the quantity
        textWidget.setOnOpListener((JavaScriptCallback) ev -> {
            interactionHandler.setChatboxValue(quantity);
            log.info("Auto-filled quantity: {}", quantity);
        });

        textWidget.revalidate();
    }

    /**
     * Add hover listeners to change text color on mouse over/leave.
     */
    private void setHoverListeners(Widget widget) {
        widget.setHasListener(true);
        widget.setOnMouseRepeatListener((JavaScriptCallback) ev ->
            widget.setTextColor(MOUSE_ON_TEXT_COLOR)
        );
        widget.setOnMouseLeaveListener((JavaScriptCallback) ev ->
            widget.setTextColor(MOUSE_OFF_TEXT_COLOR)
        );
    }

    /**
     * Shift the chatbox title down to make room for our widget.
     */
    private void shiftChatboxWidgetsDown() {
        Widget chatboxTitle = client.getWidget(ComponentID.CHATBOX_TITLE);
        if (chatboxTitle != null) {
            chatboxTitle.setOriginalY(chatboxTitle.getOriginalY() + 7);
            chatboxTitle.revalidate();
        }
    }

    /**
     * Check if the widget is valid.
     */
    public boolean isValid() {
        return textWidget != null;
    }
}
