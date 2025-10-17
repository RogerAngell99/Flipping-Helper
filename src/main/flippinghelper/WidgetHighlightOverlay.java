package flippinghelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Overlay that highlights a specific widget with a colored rectangle.
 * Used to guide users to specific GE interface elements.
 */
public class WidgetHighlightOverlay extends Overlay {
    private final Widget widget;
    private final Color color;
    private final Rectangle relativeBounds;

    public WidgetHighlightOverlay(final Widget widget, Color color, Rectangle relativeBounds) {
        this.widget = widget;
        this.color = color;
        this.relativeBounds = relativeBounds;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (widget == null || widget.isHidden()) {
            return null;
        }

        Rectangle widgetBounds = widget.getBounds();
        if (widgetBounds == null) {
            return null;
        }

        // Apply relative bounds adjustments
        Rectangle highlightBounds = new Rectangle(
            widgetBounds.x + relativeBounds.x,
            widgetBounds.y + relativeBounds.y,
            relativeBounds.width,
            relativeBounds.height
        );

        // Draw semi-transparent highlight
        graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
        graphics.fillRect(highlightBounds.x, highlightBounds.y, highlightBounds.width, highlightBounds.height);

        return null;
    }
}
